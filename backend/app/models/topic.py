import enum
import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import DateTime, Enum, Float, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base
from app.models.types import GUID


def _uuid_default():
    return uuid.uuid4()


def _utcnow():
    return datetime.now(timezone.utc)


class TopicCompletionStatus(str, enum.Enum):
    NOT_STARTED = "NOT_STARTED"
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"


class TopicDifficulty(str, enum.Enum):
    EASY = "EASY"
    MEDIUM = "MEDIUM"
    HARD = "HARD"


class Topic(Base):
    __tablename__ = "topics"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=_uuid_default)
    subject_id: Mapped[uuid.UUID] = mapped_column(
        GUID(), ForeignKey("subjects.id"), nullable=False, index=True
    )
    unit_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        GUID(), ForeignKey("units.id"), nullable=True, index=True
    )
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text(), nullable=True)
    source_reference: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    completion_status: Mapped[TopicCompletionStatus] = mapped_column(
        Enum(TopicCompletionStatus, native_enum=False, length=20),
        default=TopicCompletionStatus.NOT_STARTED,
        nullable=False,
    )
    difficulty: Mapped[TopicDifficulty] = mapped_column(
        Enum(TopicDifficulty, native_enum=False, length=10),
        default=TopicDifficulty.MEDIUM,
        nullable=False,
    )
    performance: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, onupdate=_utcnow, nullable=False
    )

    subject: Mapped["Subject"] = relationship("Subject", back_populates="topics")
    unit: Mapped[Optional["Unit"]] = relationship("Unit", back_populates="topics")
