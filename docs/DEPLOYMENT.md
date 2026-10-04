# Prove2Break — Deployment

## Backend

- **Containerized FastAPI** — the backend ships as a Docker image (Python base image, dependencies installed via the project's dependency manager, e.g. `pip`/`poetry`/`uv` — chosen at implementation time). A `docker-compose.yml` for local dev runs the API container alongside a PostgreSQL+pgvector container.
- **Env-based config** — the same image runs in dev/staging/production; all environment-specific behavior comes from environment variables (`ENVIRONMENT.md`), never from separate code branches or hardcoded config files per environment.
- **Migrations run as a pre-start step** — `alembic upgrade head` runs explicitly before the API process starts (as a separate deploy step or container init step), never implicitly inside the app's request-handling lifecycle.
- **Health check endpoint** — a lightweight `/health` (or `/api/v1/health`) route for the deployment platform's readiness/liveness checks, checking DB connectivity at minimum. **Phase 8 QA fix**: `/health` previously returned a static `{"status": "ok"}` with no DB check at all; it now runs `SELECT 1` against the configured database and reports `{"status": "ok", "db": "reachable"}` or `{"status": "error", "db": "unreachable"}` accordingly.
- **`backend/Dockerfile`** — added during Phase 8 QA. Prior phases had a `docker-compose.yml` (repo root) for the Postgres+pgvector container only; there was no Dockerfile for the FastAPI service itself. `backend/Dockerfile` is a basic `python:3.11-slim` image installing `requirements.txt` and running `uvicorn app.main:app`; it deliberately does NOT run `alembic upgrade head` (kept as a separate deploy step per the migrations rule above). Not yet wired into `docker-compose.yml` as an `api` service, and not build-tested against a live Docker daemon in this environment (see Known Deferred Items — Docker daemon unavailable).
- **Object storage** — S3 or an S3-compatible service (e.g. MinIO for local dev, a managed S3-compatible provider in production), configured via `S3_*` env vars.

## Android

- **Build variants** — standard `debug` and `release` Gradle build types. `debug` points at a local/dev backend base URL and allows verbose logging; `release` points at the production backend URL, disables verbose logging, and enables code shrinking/obfuscation (R8).
- **Signing** — release builds are signed with an upload/app signing key managed via the Play Console's App Signing (Google manages the final signing key; the developer manages an upload key). The upload keystore is never committed to source control; it's referenced via a local `keystore.properties` file (git-ignored) or CI secret store, following standard Android release-signing practice. No real keys, passwords, or keystore files are included in this repository.
- **Versioning** — `versionCode` incremented per release, `versionName` following semantic versioning aligned with `CHANGELOG.md` entries.

## Phase 8 QA — Release Build Attempt (2026-09-29)

- `gradlew.bat assembleRelease` from `android/`: **BUILD SUCCESSFUL** (~4m18s), producing `app/build/outputs/apk/release/app-release-unsigned.apk` (~11.9 MB). No fake keystore/signing secrets were created.
- As expected, the output is explicitly **unsigned** — `app/build.gradle.kts`'s `release` build type declares no `signingConfig` at all. To produce an installable/publishable release build, someone needs to: generate a real upload keystore (`keytool -genkey ...`, never committed), add a `keystore.properties` (git-ignored) with its path/passwords, and wire a `signingConfigs { release { ... } }` block referencing it into `release { signingConfig = signingConfigs.getByName("release") }`.
- Also noted: `isMinifyEnabled = false` on the release build type even though ProGuard files are already wired in (`proguardFiles(...)`) — R8 shrinking/obfuscation is effectively a no-op right now. Flip to `true` once the app is otherwise stable, since ProGuard rules for Compose/Hilt/Room may need verification once enabled.

## MVP Deployment Shape

For MVP, a single backend environment (staging-as-production is acceptable pre-launch) is sufficient: one FastAPI container, one managed PostgreSQL+pgvector instance, one S3-compatible bucket, and the Android app distributed initially via internal testing tracks (Play Console internal testing or direct APK/AAB sharing) before any public release track.
