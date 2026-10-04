"""Cross-dialect UUID column type.

Production runs on PostgreSQL (native UUID type). Tests run against SQLite
(no native UUID type), so this stores UUIDs as CHAR(36) there while still
handing back real `uuid.UUID` instances to application code either way.
"""
import uuid

from sqlalchemy import CHAR, JSON, Float, TypeDecorator
from sqlalchemy.dialects.postgresql import UUID as PG_UUID

try:
    from pgvector.sqlalchemy import Vector as PG_VECTOR
except ImportError:  # pragma: no cover - pgvector always installed via requirements.txt
    PG_VECTOR = None


class GUID(TypeDecorator):
    impl = CHAR
    cache_ok = True

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql":
            return dialect.type_descriptor(PG_UUID(as_uuid=True))
        return dialect.type_descriptor(CHAR(36))

    def process_bind_param(self, value, dialect):
        if value is None:
            return value
        if dialect.name == "postgresql":
            return str(value)
        if not isinstance(value, uuid.UUID):
            return str(uuid.UUID(value))
        return str(value)

    def process_result_value(self, value, dialect):
        if value is None:
            return value
        if isinstance(value, uuid.UUID):
            return value
        return uuid.UUID(value)


class EmbeddingVector(TypeDecorator):
    """Cross-dialect embedding column.

    On PostgreSQL this becomes a real `pgvector` `vector(dim)` column, enabling
    `ivfflat`/`hnsw` similarity search (see docs/DATABASE.md). pgvector has no
    SQLite equivalent, so on SQLite (used by the Phase 1 in-memory test suite)
    this degrades to a plain JSON column holding a list[float] — enough for the
    Phase 4 pipeline/model tests (which only need to write/read a placeholder
    embedding), but NOT usable for real vector similarity search. Any test that
    needs actual pgvector similarity search must run against real Postgres —
    documented tradeoff, consistent with the Phase 1 SQLite-vs-Postgres approach
    already logged in PROCESS.md.
    """

    impl = JSON
    cache_ok = True

    def __init__(self, dim: int, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.dim = dim

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql" and PG_VECTOR is not None:
            return dialect.type_descriptor(PG_VECTOR(self.dim))
        return dialect.type_descriptor(JSON())

    class comparator_factory(TypeDecorator.Comparator):
        # TypeDecorator hides the wrapped pgvector Vector's comparator, so expose
        # cosine distance (pgvector's `<=>`) explicitly. Postgres-only in practice;
        # app/ai/retrieval.py only calls this on the postgresql dialect.
        def cosine_distance(self, other):
            return self.op("<=>", return_type=Float)(other)
