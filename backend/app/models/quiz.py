import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import Column, DateTime, Float, ForeignKey, Table
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base
from app.models.types import GUID


def _uuid_default():
    return uuid.uuid4()


def _utcnow():
    return datetime.now(timezone.utc)


# Many-to-many Quiz <-> Topic per docs/DATABASE.md.
quiz_topics = Table(
    "quiz_topics",
    Base.metadata,
    Column("quiz_id", GUID(), ForeignKey("quizzes.id"), primary_key=True),
    Column("topic_id", GUID(), ForeignKey("topics.id"), primary_key=True),
)


class Quiz(Base):
    __tablename__ = "quizzes"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=_uuid_default)
    # docs/DATABASE.md models Quiz off `focus_session_id`/`break_id`, but no
    # FocusSession/Break backend model exists yet (those are Android-only
    # concepts so far — see docs/PROCESS.md Phase 2/5 notes). Scoping directly
    # to subject_id + user_id instead is a documented deviation (see
    # docs/QUIZ-SYSTEM.md) that keeps ownership isolation enforceable today;
    # focus_session_id/break_id can be added back as real FKs once those
    # backend models land.
    subject_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("subjects.id"), nullable=False, index=True)
    user_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("users.id"), nullable=False, index=True)
    pass_threshold_used: Mapped[float] = mapped_column(Float, nullable=False, default=0.70)
    score: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    passed: Mapped[Optional[bool]] = mapped_column(nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow, nullable=False)
    completed_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)

    topics = relationship("Topic", secondary=quiz_topics)
    questions: Mapped[list["Question"]] = relationship(
        "Question", back_populates="quiz", cascade="all, delete-orphan", order_by="Question.order_index"
    )
    attempts: Mapped[list["QuizAttempt"]] = relationship(
        "QuizAttempt", back_populates="quiz", cascade="all, delete-orphan"
    )
