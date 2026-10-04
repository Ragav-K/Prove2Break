import uuid

from sqlalchemy import JSON, ForeignKey, Integer, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base
from app.models.types import GUID


def _uuid_default():
    return uuid.uuid4()


DEFAULT_TIME_LIMIT_SECONDS = 15
REQUIRED_OPTION_COUNT = 4


class Question(Base):
    __tablename__ = "questions"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=_uuid_default)
    quiz_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("quizzes.id"), nullable=False, index=True)
    topic_id: Mapped[uuid.UUID] = mapped_column(GUID(), ForeignKey("topics.id"), nullable=False, index=True)
    prompt: Mapped[str] = mapped_column(Text(), nullable=False)
    # JSON list[str] — cross-dialect (plain JSON column, real JSONB on Postgres
    # per docs/DATABASE.md; SQLite stores the same shape as a JSON string,
    # mirroring the EmbeddingVector fallback pattern already used in Phase 4).
    options: Mapped[list[str]] = mapped_column(JSON(), nullable=False)
    correct_option_index: Mapped[int] = mapped_column(Integer, nullable=False)
    # Grounding trace (docs/AI-RAG.md "Grounding Rule") — list[str] of DocumentChunk UUIDs.
    source_chunk_ids: Mapped[list[str]] = mapped_column(JSON(), nullable=False)
    time_limit_seconds: Mapped[int] = mapped_column(Integer, nullable=False, default=DEFAULT_TIME_LIMIT_SECONDS)
    order_index: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    quiz: Mapped["Quiz"] = relationship("Quiz", back_populates="questions")
