import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import DateTime, Float, ForeignKey, Integer, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base
from app.models.types import GUID


def _uuid_default():
    return uuid.uuid4()


def _utcnow():
    return datetime.now(timezone.utc)


# Weak-topic threshold, matching the pattern of a per-user configurable value
# (User.quiz_pass_threshold) but with a simple constant default per docs/ANALYTICS.md
# ("average_score < 0.70"). Kept here so both the service and any future
# per-user override can reference one source of truth.
WEAK_TOPIC_THRESHOLD = 0.70


class TopicPerformance(Base):
    """Denormalized aggregate updated transactionally alongside QuizAttempt writes
    (docs/DATABASE.md "TopicPerformance"), so analytics/weak-topic queries never
    need to scan raw QuizAnswer history at read time.
    """

    __tablename__ = "topic_performance"
    __table_args__ = (UniqueConstraint("user_id", "topic_id", name="uq_topic_performance_user_topic"),)

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=_uuid_default)
    user_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("users.id"), nullable=False, index=True)
    topic_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("topics.id"), nullable=False, index=True)
    attempt_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    average_score: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    last_attempt_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, onupdate=_utcnow, nullable=False
    )

    topic: Mapped["Topic"] = relationship("Topic")
