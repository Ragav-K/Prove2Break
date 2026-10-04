"""SQLAlchemy engine/session setup.

Uses the `psycopg` (v3) driver, which has first-class pgvector support (via the
`pgvector` package's psycopg integration) for when the AI/RAG phase adds
DocumentChunk.embedding columns — so we avoid psycopg2 here to not paint
ourselves into a corner.
"""
from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.core.config import get_settings


def _normalize_db_url(url: str) -> str:
    """Ensure the URL uses the psycopg3 driver regardless of how it's written in .env."""
    if url.startswith("postgresql://"):
        return url.replace("postgresql://", "postgresql+psycopg://", 1)
    return url


settings = get_settings()
SQLALCHEMY_DATABASE_URL = _normalize_db_url(settings.DATABASE_URL)

# `future=True`/2.0-style engine. pool_pre_ping avoids stale-connection errors.
engine = create_engine(SQLALCHEMY_DATABASE_URL, pool_pre_ping=True)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def get_db():
    """FastAPI dependency yielding a DB session, closed after the request."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
