"""Application configuration.

Settings are loaded from environment variables (see backend/.env.example for the
authoritative list of names). Uses pydantic-settings so config is validated at
startup and can be overridden per-environment without code changes.
"""
from functools import lru_cache
from typing import List

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # --- Database ---
    DATABASE_URL: str = "postgresql+psycopg://your-user:your-password@localhost:5432/prove2break"

    # --- JWT Auth ---
    JWT_SECRET_KEY: str = "dev-secret-change-me"
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    JWT_REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    # --- Object Storage (S3-compatible) ---
    S3_BUCKET_NAME: str = ""
    S3_ENDPOINT_URL: str = ""
    S3_ACCESS_KEY: str = ""
    S3_SECRET_KEY: str = ""

    # --- AI Provider (behind AIService abstraction) ---
    AI_PROVIDER: str = ""
    AI_API_KEY: str = ""
    AI_MODEL_NAME: str = ""

    # --- CORS ---
    CORS_ORIGINS: str = "http://localhost:3000,http://10.0.2.2:8000"

    # --- Study material storage (Phase 4) ---
    # Local-disk fallback used until S3/MinIO wiring lands (see docs/ENVIRONMENT.md
    # deviation note). Files are written under this directory, keyed by a
    # generated path stored in StudyMaterial.s3_key.
    STORAGE_DIR: str = "./storage/materials"
    MAX_UPLOAD_SIZE_BYTES: int = 50 * 1024 * 1024
    MAX_PDF_PAGES: int = 500

    @property
    def cors_origins_list(self) -> List[str]:
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",") if origin.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
