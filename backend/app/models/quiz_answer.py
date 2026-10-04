import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import DateTime, Float, ForeignKey, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base
from app.models.types import GUID


def _uuid_default():
    return uuid.uuid4()


def _utcnow():
    return datetime.now(timezone.utc)


class QuizAnswer(Base):
    __tablename__ = "quiz_answers"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=_uuid_default)
    quiz_attempt_id: Mapped[uuid.UUID] = mapped_column(
        GUID(), ForeignKey("quiz_attempts.id"), nullable=False, index=True
    )
    question_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("questions.id"), nullable=False)
    # Null == timed out (docs/QUIZ-SYSTEM.md "Scoring": counts as incorrect).
    selected_option_index: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    is_correct: Mapped[bool] = mapped_column(nullable=False, default=False)
    answered_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow, nullable=False)
    time_taken_seconds: Mapped[Optional[float]] = mapped_column(Float, nullable=True)

    attempt: Mapped["QuizAttempt"] = relationship("QuizAttempt", back_populates="answers")
