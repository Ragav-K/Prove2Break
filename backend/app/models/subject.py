import uuid
from datetime import date, datetime, timezone
from typing import Optional

from sqlalchemy import Date, DateTime, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base
from app.models.types import GUID


def _uuid_default():
    return uuid.uuid4()


def _utcnow():
    return datetime.now(timezone.utc)


class Subject(Base):
    __tablename__ = "subjects"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=_uuid_default)
    user_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("users.id"), nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    exam_name: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    exam_date: Mapped[Optional[date]] = mapped_column(Date(), nullable=True)
    description: Mapped[Optional[str]] = mapped_column(Text(), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=_utcnow, onupdate=_utcnow, nullable=False
    )

    units: Mapped[list["Unit"]] = relationship(
        "Unit", back_populates="subject", cascade="all, delete-orphan"
    )
    topics: Mapped[list["Topic"]] = relationship(
        "Topic", back_populates="subject", cascade="all, delete-orphan"
    )
    materials: Mapped[list["StudyMaterial"]] = relationship(
        "StudyMaterial", back_populates="subject", cascade="all, delete-orphan"
    )
