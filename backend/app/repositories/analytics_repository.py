"""Data-access layer for TopicPerformance and quiz-aggregate analytics queries."""
import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.quiz import Quiz
from app.models.quiz_attempt import QuizAttempt
from app.models.topic import Topic
from app.models.topic_performance import TopicPerformance


class AnalyticsRepository:
    def __init__(self, db: Session):
        self.db = db

    # --- TopicPerformance ---

    def get_topic_performance(self, user_id: uuid.UUID, topic_id: uuid.UUID) -> Optional[TopicPerformance]:
        stmt = select(TopicPerformance).where(
            TopicPerformance.user_id == user_id, TopicPerformance.topic_id == topic_id
        )
        return self.db.execute(stmt).scalar_one_or_none()

    def upsert_topic_performance(self, user_id: uuid.UUID, topic_id: uuid.UUID, topic_score: float) -> TopicPerformance:
        """Rolling-average upsert for one topic after a single quiz attempt's
        per-topic score (docs/DATABASE.md: "Recomputed/upserted after each
        QuizAttempt affecting that topic.")
        """
        row = self.get_topic_performance(user_id, topic_id)
        now = datetime.now(timezone.utc)
        if row is None:
            row = TopicPerformance(
                user_id=user_id,
                topic_id=topic_id,
                attempt_count=1,
                average_score=topic_score,
                last_attempt_at=now,
            )
            self.db.add(row)
        else:
            new_count = row.attempt_count + 1
            row.average_score = ((row.average_score * row.attempt_count) + topic_score) / new_count
            row.attempt_count = new_count
            row.last_attempt_at = now
        self.db.flush()
        return row

    def list_for_user(self, user_id: uuid.UUID) -> list[TopicPerformance]:
        stmt = (
            select(TopicPerformance)
            .join(Topic, TopicPerformance.topic_id == Topic.id)
            .where(TopicPerformance.user_id == user_id)
            .order_by(Topic.name.asc())
        )
        return list(self.db.execute(stmt).scalars().all())

    def list_weak_for_user(self, user_id: uuid.UUID, threshold: float) -> list[TopicPerformance]:
        stmt = (
            select(TopicPerformance)
            .where(TopicPerformance.user_id == user_id, TopicPerformance.average_score < threshold)
            .order_by(TopicPerformance.average_score.asc())
        )
        return list(self.db.execute(stmt).scalars().all())

    # --- Quiz overview aggregates ---

    def quiz_overview(self, user_id: uuid.UUID) -> dict:
        total_attempts = self.db.execute(
            select(func.count(QuizAttempt.id)).where(
                QuizAttempt.user_id == user_id, QuizAttempt.completed_at.is_not(None)
            )
        ).scalar_one()

        passed_attempts = self.db.execute(
            select(func.count(QuizAttempt.id)).where(
                QuizAttempt.user_id == user_id,
                QuizAttempt.completed_at.is_not(None),
                QuizAttempt.passed.is_(True),
            )
        ).scalar_one()

        avg_score = self.db.execute(
            select(func.avg(QuizAttempt.final_score)).where(
                QuizAttempt.user_id == user_id, QuizAttempt.completed_at.is_not(None)
            )
        ).scalar_one()

        total_quizzes = self.db.execute(
            select(func.count(Quiz.id)).where(Quiz.user_id == user_id)
        ).scalar_one()

        return {
            "total_attempts": total_attempts or 0,
            "passed_attempts": passed_attempts or 0,
            "pass_rate": (passed_attempts / total_attempts) if total_attempts else 0.0,
            "average_score": float(avg_score) if avg_score is not None else 0.0,
            "total_quizzes": total_quizzes or 0,
        }
