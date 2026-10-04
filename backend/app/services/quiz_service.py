"""Quiz generation + attempt/scoring service (docs/QUIZ-SYSTEM.md, docs/AI-RAG.md).

Layering matches subject/material/topic services: route -> service -> repository -> model.
"""
from __future__ import annotations

import uuid
from typing import Optional

from sqlalchemy.orm import Session

from app.ai.factory import get_ai_service
from app.ai.retrieval import MIN_CHUNKS_PER_TOPIC, get_relevant_chunks
from app.core.config import Settings
from app.core.exceptions import BadRequestError, NotFoundError
from app.models.question import DEFAULT_TIME_LIMIT_SECONDS, REQUIRED_OPTION_COUNT, Question
from app.models.quiz import Quiz
from app.models.quiz_answer import QuizAnswer
from app.models.quiz_attempt import QuizAttempt
from app.repositories.analytics_repository import AnalyticsRepository
from app.repositories.quiz_repository import QuizRepository
from app.repositories.subject_repository import SubjectRepository
from app.repositories.topic_repository import TopicRepository
from app.repositories.user_repository import UserRepository

MIN_VALID_QUESTIONS = 1


class QuizService:
    def __init__(self, db: Session, settings: Settings):
        self.db = db
        self.settings = settings
        self.ai = get_ai_service(settings)
        self.quizzes = QuizRepository(db)
        self.subjects = SubjectRepository(db)
        self.topics = TopicRepository(db)
        self.users = UserRepository(db)
        self.analytics = AnalyticsRepository(db)

    # ------------------------------------------------------------------
    # Generation
    # ------------------------------------------------------------------

    def generate_quiz(
        self,
        user_id: uuid.UUID,
        subject_id: uuid.UUID,
        topic_ids: list[uuid.UUID],
        num_questions: int,
        time_limit_seconds: int,
    ) -> Quiz:
        subject = self.subjects.get_for_user(subject_id, user_id)
        if subject is None:
            raise NotFoundError("Subject not found")

        topics = []
        for topic_id in topic_ids:
            topic = self.topics.get_for_user(topic_id, user_id)
            if topic is None or topic.subject_id != subject_id:
                raise NotFoundError(f"Topic {topic_id} not found for this subject")
            topics.append(topic)

        chunks_by_topic = get_relevant_chunks(self.db, self.ai, user_id, topics)

        # Exclude topics with insufficient grounding material rather than failing
        # outright, as long as at least one topic remains usable (docs/AI-RAG.md).
        usable_topics = [t for t in topics if len(chunks_by_topic.get(t.id, [])) >= MIN_CHUNKS_PER_TOPIC]
        if not usable_topics:
            raise BadRequestError("Not enough material on the selected topics yet to generate a fair quiz")

        topic_chunks_payload = [
            {
                "topic_id": str(topic.id),
                "topic_name": topic.name,
                "chunks": [{"id": str(c.id), "text": c.chunk_text} for c in chunks_by_topic[topic.id]],
            }
            for topic in usable_topics
        ]

        # Build a set of valid (chunk_id, topic_id) pairs for the grounding check below.
        valid_chunk_ids_by_topic: dict[str, set[str]] = {
            str(topic.id): {str(c.id) for c in chunks_by_topic[topic.id]} for topic in usable_topics
        }
        valid_topic_ids = {str(t.id) for t in usable_topics}

        raw_questions = self.ai.generate_quiz(topic_chunks_payload, num_questions)
        valid_questions = self._validate_questions(raw_questions, valid_topic_ids, valid_chunk_ids_by_topic)

        if len(valid_questions) < num_questions:
            # Retry once with a relaxed target (docs/QUIZ-SYSTEM.md step 6).
            retry_raw = self.ai.generate_quiz(topic_chunks_payload, num_questions * 2)
            retry_valid = self._validate_questions(retry_raw, valid_topic_ids, valid_chunk_ids_by_topic)
            if len(retry_valid) > len(valid_questions):
                valid_questions = retry_valid

        if len(valid_questions) < MIN_VALID_QUESTIONS:
            raise BadRequestError(
                "Could not generate enough valid, grounded questions from the available material"
            )

        valid_questions = valid_questions[:num_questions]

        user = self.users.get_by_id(user_id)
        pass_threshold = user.quiz_pass_threshold if user is not None else 0.70

        quiz = self.quizzes.create_quiz(subject_id, user_id, pass_threshold, usable_topics)

        question_models = [
            Question(
                quiz_id=quiz.id,
                topic_id=uuid.UUID(q["topic_id"]),
                prompt=q["prompt"],
                options=q["options"],
                correct_option_index=q["correct_option_index"],
                source_chunk_ids=q["source_chunk_ids"],
                time_limit_seconds=time_limit_seconds or DEFAULT_TIME_LIMIT_SECONDS,
                order_index=idx,
            )
            for idx, q in enumerate(valid_questions)
        ]
        self.quizzes.add_questions(question_models)
        self.db.refresh(quiz)
        return quiz

    @staticmethod
    def _validate_questions(
        raw_questions: list[dict],
        valid_topic_ids: set[str],
        valid_chunk_ids_by_topic: dict[str, set[str]],
    ) -> list[dict]:
        """Enforces docs/QUIZ-SYSTEM.md "Validation Rules" 1-5."""
        valid: list[dict] = []
        for q in raw_questions:
            topic_id = q.get("topic_id")
            options = q.get("options") or []
            correct_index = q.get("correct_option_index")
            source_chunk_ids = q.get("source_chunk_ids") or []

            if topic_id not in valid_topic_ids:
                continue
            if len(options) != REQUIRED_OPTION_COUNT:
                continue
            if not isinstance(correct_index, int) or not (0 <= correct_index < len(options)):
                continue
            normalized = [str(o).strip().lower() for o in options]
            if len(set(normalized)) != len(normalized):
                continue  # duplicate options
            if not source_chunk_ids:
                continue  # ungrounded
            allowed_chunks = valid_chunk_ids_by_topic.get(topic_id, set())
            if not any(cid in allowed_chunks for cid in source_chunk_ids):
                continue  # source not traceable to this topic's own material
            valid.append(q)
        return valid

    # ------------------------------------------------------------------
    # Attempt / scoring
    # ------------------------------------------------------------------

    def start_attempt(self, quiz_id: uuid.UUID, user_id: uuid.UUID) -> QuizAttempt:
        quiz = self.quizzes.get_for_user(quiz_id, user_id)
        if quiz is None:
            raise NotFoundError("Quiz not found")
        return self.quizzes.create_attempt(quiz_id, user_id)

    def submit_attempt(
        self, quiz_id: uuid.UUID, user_id: uuid.UUID, answers: list[dict]
    ) -> tuple[QuizAttempt, int, int]:
        quiz = self.quizzes.get_for_user(quiz_id, user_id)
        if quiz is None:
            raise NotFoundError("Quiz not found")

        attempt = self.quizzes.get_latest_open_attempt(quiz_id, user_id)
        if attempt is None:
            raise BadRequestError("No open attempt for this quiz — call start-attempt first")

        questions_by_id = {q.id: q for q in quiz.questions}
        answer_models = []
        correct_count = 0
        for ans in answers:
            question = questions_by_id.get(ans["question_id"])
            if question is None:
                raise BadRequestError(f"Question {ans['question_id']} does not belong to this quiz")
            selected = ans.get("selected_option_index")
            is_correct = selected is not None and selected == question.correct_option_index
            if is_correct:
                correct_count += 1
            answer_models.append(
                QuizAnswer(
                    quiz_attempt_id=attempt.id,
                    question_id=question.id,
                    selected_option_index=selected,
                    is_correct=is_correct,
                    time_taken_seconds=ans.get("time_taken_seconds"),
                )
            )

        self.quizzes.add_answers(answer_models)

        total_questions = len(quiz.questions)
        score = correct_count / total_questions if total_questions else 0.0
        passed = score >= quiz.pass_threshold_used

        attempt = self.quizzes.complete_attempt(attempt, score, passed)
        self.quizzes.complete_quiz(quiz, score, passed)

        self._update_topic_performance(user_id, quiz, answer_models)

        return attempt, correct_count, total_questions

    def _update_topic_performance(self, user_id: uuid.UUID, quiz: Quiz, answer_models: list[QuizAnswer]) -> None:
        """Updates TopicPerformance (docs/QUIZ-SYSTEM.md: "Every attempt updates
        TopicPerformance for each covered topic") using this attempt's own
        per-topic score, so multi-topic quizzes don't blend unrelated topics
        together in the rolling average.
        """
        questions_by_id = {q.id: q for q in quiz.questions}
        correct_by_topic: dict[uuid.UUID, int] = {}
        total_by_topic: dict[uuid.UUID, int] = {}
        for ans in answer_models:
            question = questions_by_id.get(ans.question_id)
            if question is None:
                continue
            total_by_topic[question.topic_id] = total_by_topic.get(question.topic_id, 0) + 1
            if ans.is_correct:
                correct_by_topic[question.topic_id] = correct_by_topic.get(question.topic_id, 0) + 1

        for topic_id, total in total_by_topic.items():
            topic_score = correct_by_topic.get(topic_id, 0) / total if total else 0.0
            self.analytics.upsert_topic_performance(user_id, topic_id, topic_score)
        self.db.commit()

    def get_result(self, quiz_id: uuid.UUID, user_id: uuid.UUID) -> tuple[QuizAttempt, int]:
        quiz = self.quizzes.get_for_user(quiz_id, user_id)
        if quiz is None:
            raise NotFoundError("Quiz not found")
        attempt = self.quizzes.get_latest_completed_attempt(quiz_id, user_id)
        if attempt is None:
            raise NotFoundError("No completed attempt for this quiz yet")
        return attempt, len(quiz.questions)
