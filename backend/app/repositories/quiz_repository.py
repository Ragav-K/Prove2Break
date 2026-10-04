"""Data-access layer for Quiz/Question/QuizAttempt/QuizAnswer.

Ownership isolation: Quiz has a direct `user_id` column (see the deviation
note in app/models/quiz.py), so every lookup filters on it directly rather
than joining through Subject — simpler and equally structurally safe.
"""
import uuid
from typing import Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.question import Question
from app.models.quiz import Quiz
from app.models.quiz_answer import QuizAnswer
from app.models.quiz_attempt import QuizAttempt


class QuizRepository:
    def __init__(self, db: Session):
        self.db = db

    # --- Quiz / Question ---

    def create_quiz(self, subject_id: uuid.UUID, user_id: uuid.UUID, pass_threshold: float, topics: list) -> Quiz:
        quiz = Quiz(subject_id=subject_id, user_id=user_id, pass_threshold_used=pass_threshold)
        quiz.topics = topics
        self.db.add(quiz)
        self.db.commit()
        self.db.refresh(quiz)
        return quiz

    def add_questions(self, questions: list[Question]) -> None:
        self.db.add_all(questions)
        self.db.commit()

    def get_for_user(self, quiz_id: uuid.UUID, user_id: uuid.UUID) -> Optional[Quiz]:
        stmt = select(Quiz).where(Quiz.id == quiz_id, Quiz.user_id == user_id)
        return self.db.execute(stmt).scalar_one_or_none()

    # --- QuizAttempt ---

    def create_attempt(self, quiz_id: uuid.UUID, user_id: uuid.UUID) -> QuizAttempt:
        attempt = QuizAttempt(quiz_id=quiz_id, user_id=user_id)
        self.db.add(attempt)
        self.db.commit()
        self.db.refresh(attempt)
        return attempt

    def get_attempt_for_user(self, attempt_id: uuid.UUID, user_id: uuid.UUID) -> Optional[QuizAttempt]:
        stmt = select(QuizAttempt).where(QuizAttempt.id == attempt_id, QuizAttempt.user_id == user_id)
        return self.db.execute(stmt).scalar_one_or_none()

    def get_latest_open_attempt(self, quiz_id: uuid.UUID, user_id: uuid.UUID) -> Optional[QuizAttempt]:
        stmt = (
            select(QuizAttempt)
            .where(
                QuizAttempt.quiz_id == quiz_id,
                QuizAttempt.user_id == user_id,
                QuizAttempt.completed_at.is_(None),
            )
            .order_by(QuizAttempt.started_at.desc())
        )
        return self.db.execute(stmt).scalars().first()

    def get_latest_completed_attempt(self, quiz_id: uuid.UUID, user_id: uuid.UUID) -> Optional[QuizAttempt]:
        stmt = (
            select(QuizAttempt)
            .where(
                QuizAttempt.quiz_id == quiz_id,
                QuizAttempt.user_id == user_id,
                QuizAttempt.completed_at.is_not(None),
            )
            .order_by(QuizAttempt.completed_at.desc())
        )
        return self.db.execute(stmt).scalars().first()

    def add_answers(self, answers: list[QuizAnswer]) -> None:
        self.db.add_all(answers)
        self.db.commit()

    def complete_attempt(self, attempt: QuizAttempt, final_score: float, passed: bool) -> QuizAttempt:
        from datetime import datetime, timezone

        attempt.completed_at = datetime.now(timezone.utc)
        attempt.final_score = final_score
        attempt.passed = passed
        self.db.commit()
        self.db.refresh(attempt)
        return attempt

    def complete_quiz(self, quiz: Quiz, score: float, passed: bool) -> Quiz:
        from datetime import datetime, timezone

        quiz.score = score
        quiz.passed = passed
        quiz.completed_at = datetime.now(timezone.utc)
        self.db.commit()
        self.db.refresh(quiz)
        return quiz
