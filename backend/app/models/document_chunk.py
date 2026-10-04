import uuid

from sqlalchemy import ForeignKey, Integer, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base
from app.models.types import GUID, EmbeddingVector

# Placeholder dimension — real embeddings (and their real dimension, dependent on
# the chosen provider/model) land in Phase 6 (AI-RAG). 1536 matches OpenAI's
# text-embedding-3-small/ada-002 as a reasonable placeholder per the task brief.
EMBEDDING_DIM = 1536


def _uuid_default():
    return uuid.uuid4()


class DocumentChunk(Base):
    __tablename__ = "document_chunks"

    id: Mapped[uuid.UUID] = mapped_column(GUID(), primary_key=True, default=_uuid_default)
    study_material_id: Mapped[uuid.UUID] = mapped_column(
        GUID(), ForeignKey("study_materials.id"), nullable=False, index=True
    )
    # Added in Phase 6 (AI-RAG) per docs/DATABASE.md — lets retrieval scope
    # directly to "chunks belonging to this topic" instead of a whole-document
    # search. Nullable because Phase 4 material processing may not always be
    # able to attribute a chunk to a single detected topic/section.
    topic_id: Mapped["uuid.UUID | None"] = mapped_column(
        GUID(), ForeignKey("topics.id"), nullable=True, index=True
    )
    chunk_text: Mapped[str] = mapped_column(Text(), nullable=False)
    chunk_index: Mapped[int] = mapped_column(Integer, nullable=False)
    # See app/models/types.py::EmbeddingVector for the Postgres/pgvector vs.
    # SQLite-test-fallback tradeoff.
    embedding: Mapped[list[float] | None] = mapped_column(EmbeddingVector(EMBEDDING_DIM), nullable=True)

    study_material: Mapped["StudyMaterial"] = relationship("StudyMaterial", back_populates="chunks")
