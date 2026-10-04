"""Analytics aggregation service (docs/ANALYTICS.md).

Backend owns quiz/topic-performance analytics only — study-time/focus-session
stats are Android-local (Room) per the Phase 2/5 "no backend sync" decision and
are aggregated client-side instead (see docs/PROCESS.md "Data split for
analytics"). This service only ever aggregates Quiz/QuizAttempt/TopicPerformance
data that already lives on the backend.
"""
import uuid

from sqlalchemy.orm import Session

from app.models.topic_performance import WEAK_TOPIC_THRESHOLD
from app.repositories.analytics_repository import AnalyticsRepository
from app.repositories.topic_repository import TopicRepository


class AnalyticsService:
    def __init__(self, db: Session):
        self.db = db
        self.analytics = AnalyticsRepository(db)
        self.topics = TopicRepository(db)

    def get_overview(self, user_id: uuid.UUID) -> dict:
        return self.analytics.quiz_overview(user_id)

    def get_topic_performance(self, user_id: uuid.UUID) -> list[dict]:
        rows = self.analytics.list_for_user(user_id)
        return [self._to_dict(row) for row in rows]

    def get_weak_topics(self, user_id: uuid.UUID, threshold: float = WEAK_TOPIC_THRESHOLD) -> list[dict]:
        rows = self.analytics.list_weak_for_user(user_id, threshold)
        return [self._to_dict(row) for row in rows]

    @staticmethod
    def _to_dict(row) -> dict:
        return {
            "topic_id": row.topic_id,
            "topic_name": row.topic.name if row.topic is not None else None,
            "attempt_count": row.attempt_count,
            "average_score": row.average_score,
            "last_attempt_at": row.last_attempt_at,
        }
