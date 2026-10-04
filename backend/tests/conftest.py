"""Test fixtures.

Uses an in-memory SQLite DB instead of a live Postgres instance, for fast,
dependency-free test runs. Tradeoff: SQLite doesn't support pgvector or some
Postgres-specific types, but Phase 1 only touches the `users` table (plain
columns), so SQLAlchemy's cross-dialect behavior is sufficient here. Once
DocumentChunk/pgvector land, those tests will need a real Postgres (e.g. via
testcontainers) — noted for a later phase, not needed for auth-only Phase 1.
"""
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.config import get_settings
from app.db import Base, get_db
from app.main import app

SQLALCHEMY_TEST_DATABASE_URL = "sqlite:///:memory:"


@pytest.fixture()
def db_session():
    engine = create_engine(
        SQLALCHEMY_TEST_DATABASE_URL,
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
    Base.metadata.create_all(bind=engine)

    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()
        Base.metadata.drop_all(bind=engine)
        engine.dispose()


@pytest.fixture()
def client(db_session, tmp_path):
    def override_get_db():
        try:
            yield db_session
        finally:
            pass

    def override_get_settings():
        settings = get_settings()
        # Route uploaded-material storage to a per-test tmp dir instead of the
        # real STORAGE_DIR (keeps tests hermetic / avoids leftover files).
        settings = settings.model_copy(update={"STORAGE_DIR": str(tmp_path / "materials")})
        return settings

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_settings] = override_get_settings
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


def register_and_login(client, email: str, password: str = "correct-horse") -> dict:
    """Helper: registers a user and returns auth headers for subsequent requests."""
    client.post("/api/auth/register", json={"email": email, "password": password})
    login = client.post("/api/auth/login", json={"email": email, "password": password})
    token = login.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}
