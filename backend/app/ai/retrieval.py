"""RAG retrieval: given topic ids, fetch grounding DocumentChunks for quiz
generation (docs/AI-RAG.md "pgvector Similarity Retrieval").

Two code paths, picked at runtime by inspecting the bound SQLAlchemy dialect:

- Postgres (production / pgvector available): real ANN similarity search —
  chunks are ranked by cosine distance between a query embedding (the topic
  name + a representative snippet, embedded via `AIService.embed_text`) and
  `DocumentChunk.embedding`, using pgvector's `<=>` operator via
  `EmbeddingVector`'s pgvector-backed column.
- SQLite (this environment's test suite — Docker/Postgres is not available
  here, see docs/PROCESS.md "Known Deferred Items"): pgvector has no SQLite
  equivalent, so this degrades to a keyword/text-overlap scoring fallback —
  ranks chunks by the number of shared significant words with the topic name,
  clearly a lesser approximation of real semantic similarity. This path is
  ONLY exercised in this environment; real ANN behavior remains unverified
  against live Postgres until Docker is available.

Retrieval is always scoped through StudyMaterial -> Subject.user_id, so
cross-user retrieval is structurally impossible (docs/AI-RAG.md "Grounding Rule").
"""
from __future__ import annotations

import re
import uuid
from typing import Dict, List

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ai.service import AIService
from app.models.document_chunk import DocumentChunk
from app.models.study_material import StudyMaterial
from app.models.subject import Subject
from app.models.topic import Topic

DEFAULT_TOP_K_PER_TOPIC = 8
MIN_CHUNKS_PER_TOPIC = 1

_WORD_RE = re.compile(r"[a-z0-9]+")


def _keywords(text: str) -> set[str]:
    return {w for w in _WORD_RE.findall(text.lower()) if len(w) >= 4}


def get_relevant_chunks(
    db: Session,
    ai_service: AIService,
    user_id: uuid.UUID,
    topics: List[Topic],
    top_k_per_topic: int = DEFAULT_TOP_K_PER_TOPIC,
) -> Dict[uuid.UUID, List[DocumentChunk]]:
    """Returns {topic_id: [DocumentChunk, ...]} scoped to the user's own material."""
    dialect_name = db.get_bind().dialect.name
    result: Dict[uuid.UUID, List[DocumentChunk]] = {}

    for topic in topics:
        base_stmt = (
            select(DocumentChunk)
            .join(StudyMaterial, DocumentChunk.study_material_id == StudyMaterial.id)
            .join(Subject, StudyMaterial.subject_id == Subject.id)
            .where(DocumentChunk.topic_id == topic.id, Subject.user_id == user_id)
        )

        if dialect_name == "postgresql":
            embedding_dim = _chunk_embedding_dim(db, topic.id) or 1536
            query_text = f"{topic.name}. {topic.description or ''}".strip()
            query_vector = ai_service.embed_text(query_text, embedding_dim)
            stmt = (
                base_stmt.where(DocumentChunk.embedding.is_not(None))
                .order_by(DocumentChunk.embedding.cosine_distance(query_vector))
                .limit(top_k_per_topic)
            )
            chunks = list(db.execute(stmt).scalars().all())
        else:
            # SQLite fallback: keyword/text-overlap scoring (documented above).
            all_chunks = list(db.execute(base_stmt).scalars().all())
            topic_keywords = _keywords(f"{topic.name} {topic.description or ''}")
            scored = []
            for chunk in all_chunks:
                overlap = len(topic_keywords & _keywords(chunk.chunk_text))
                scored.append((overlap, chunk))
            # Stable-ish ordering: highest overlap first, then original (chunk_index) order.
            scored.sort(key=lambda pair: (-pair[0], pair[1].chunk_index))
            chunks = [c for _, c in scored[:top_k_per_topic]]
            if not chunks:
                # No keyword overlap at all (e.g. very short topic name) — fall back
                # to "just take the topic's own chunks in order" rather than
                # returning nothing when material genuinely exists.
                chunks = all_chunks[:top_k_per_topic]

        result[topic.id] = chunks

    return result


def _chunk_embedding_dim(db: Session, topic_id: uuid.UUID) -> int | None:
    from app.models.document_chunk import EMBEDDING_DIM

    return EMBEDDING_DIM
