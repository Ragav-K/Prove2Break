import enum
import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import DateTime, Enum, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base
from app.models.types import GUID


def _uuid_default():
    return uuid.uuid4()


def _utcnow():
    return datetime.now(timezone.utc)


class StudyMaterialStatus(str, enum.Enum):
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    READY = "READY"
    FAILED = "FAILED"


class StudyMaterial(Base):
    __tablename__ = "study_materials"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=_uuid_default)
    subject_id: Mapped[uuid.UUID] = mapped_column(
        GUID(), ForeignKey("subjects.id"), nullable=False, index=True
    )
    filename: Mapped[str] = mapped_column(String(255), nullable=False)
    # Local-disk storage path for MVP (see docs/PDF-PROCESSING.md deviation note in
    # docs/DATABASE.md — S3-compatible storage is the documented target, but no S3/MinIO
    # is wired up yet in this environment, so files are stored on local disk under a
    # configurable STORAGE_DIR; the column is still named to match the eventual S3 key).
    s3_key: Mapped[str] = mapped_column(String(512), nullable=False)
    page_count: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    status: Mapped[StudyMaterialStatus] = mapped_column(
        Enum(StudyMaterialStatus, native_enum=False, length=20),
        default=StudyMaterialStatus.PENDING,
        nullable=False,
    )
    failure_reason: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    uploaded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_utcnow, nullable=False)

    subject: Mapped["Subject"] = relationship("Subject", back_populates="materials")
    chunks: Mapped[list["DocumentChunk"]] = relationship(
        "DocumentChunk", back_populates="study_material", cascade="all, delete-orphan"
    )
