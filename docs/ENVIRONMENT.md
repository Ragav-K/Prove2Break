# Prove2Break — Environment Setup

## Backend Environment Variables (names only — see `backend/.env.example`)

| Variable | Purpose |
|---|---|
| `DATABASE_URL` | PostgreSQL connection string (includes pgvector-enabled database). |
| `JWT_SECRET_KEY` | Secret used to sign/verify JWTs. |
| `JWT_ALGORITHM` | JWT signing algorithm (e.g. HS256). |
| `JWT_ACCESS_TOKEN_EXPIRE_MINUTES` | Access token lifetime. |
| `JWT_REFRESH_TOKEN_EXPIRE_DAYS` | Refresh token lifetime. |
| `S3_BUCKET_NAME` | Object storage bucket for uploaded PDFs. |
| `S3_ENDPOINT_URL` | S3-compatible endpoint (for non-AWS providers or local MinIO). |
| `S3_ACCESS_KEY` | Object storage access key. |
| `S3_SECRET_KEY` | Object storage secret key. |
| `STORAGE_DIR` | **Phase 4 interim deviation:** local-disk directory used in place of S3 for uploaded PDFs. `StudyMaterial.s3_key` currently holds a local path, not a real S3 key. S3 wiring (via `S3_*` vars above) is still the documented target and should replace this before any real deployment; tracked as a known gap in PROCESS.md. |
| `MAX_UPLOAD_SIZE_BYTES` | Upload size cap enforced by `POST /api/materials/upload`. |
| `MAX_PDF_PAGES` | Page-count cap enforced by the PDF processing pipeline. |
| `AI_PROVIDER` | Which AI provider implementation `AIService` should use. |
| `AI_API_KEY` | API key for the configured AI provider. |
| `AI_MODEL_NAME` | Model identifier to use for generation calls. |
| `CORS_ORIGINS` | Allowed origins for CORS (comma-separated). |

## Android Build Config Fields

- `BASE_URL` — backend API base URL, differs per build variant (debug → local/dev backend, release → production backend).
- `ENABLE_VERBOSE_LOGGING` — boolean, true only in debug builds.

## Local Dev Setup — Backend

1. Clone the repo, `cd backend`.
2. Copy `.env.example` to `.env` and fill in local values (a local Postgres+pgvector via docker-compose, a MinIO instance or real S3 test bucket, and a dev-tier AI provider key).
3. Start dependencies: `docker compose up -d` (Postgres+pgvector, optionally MinIO). The container is published on host port **5433** (not 5432) to avoid clashing with a natively installed PostgreSQL service; use `postgresql://prove2break:prove2break@localhost:5433/prove2break` (credentials are the local-dev defaults in `docker-compose.yml`).
4. Install dependencies (via the project's chosen Python dependency manager).
5. Run migrations: `alembic upgrade head`.
6. Start the API: `uvicorn main:app --reload`.
7. Confirm `/health` responds successfully.

## Local Dev Setup — Android (Android Studio)

1. Open the `android/` directory as an Android Studio project.
2. Ensure the Android Gradle Plugin / Kotlin versions match the project's `gradle/libs.versions.toml` (once the Gradle project is scaffolded in Phase 1).
3. Set `BASE_URL` (debug build config field) to point at the locally running backend (e.g. `http://10.0.2.2:8000` for the emulator reaching the host machine).
4. Sync Gradle, run the `debug` build variant on an emulator or device.
5. For any Usage Access/Accessibility Service testing, manually grant the special permission via system settings on the test device/emulator, since it can't be granted via a standard runtime dialog.
