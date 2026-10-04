# Changelog

All notable changes to Prove2Break will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.1.1-mvp] — 2026-09-30

### Fixed
- Missing Alembic migration for the `topic_performance` table (Phase 7 added the model only) — quiz submission and `/api/analytics/*` would have failed on real Postgres.
- pgvector similarity search crashed (`cosine_distance` not exposed by the `EmbeddingVector` `TypeDecorator`), so `POST /api/quiz/generate` returned 500 on Postgres.

### Changed
- `docker-compose.yml` publishes Postgres on host port 5433 (a native Windows PostgreSQL service already owns 5432 on the dev machine).

### Verified
- All migrations apply on real Postgres 16 + pgvector; full upload → topics → pgvector retrieval → quiz → submit → analytics flow driven end-to-end. Closes the "Docker/pgvector never verified" gap. Backend pytest still 43/43 (SQLite).

## [0.1.0-mvp] — 2026-09-29

All 8 planned MVP development phases complete. This is a functional, tested MVP with honestly-documented open items (below) — not yet release-ready.

### Added
- Full project documentation structure, `PROCESS.md` as living source of truth (Phase 0)
- Android project scaffold: Kotlin + Compose + Material 3 theme, Hilt DI, Navigation Compose graph, Room/DataStore/Retrofit/WorkManager, full package skeleton (Phase 1)
- Backend scaffold: FastAPI, SQLAlchemy + Alembic, argon2id password hashing, JWT access/refresh auth (`/api/auth/register|login|refresh`) (Phase 1)
- Focus Engine: timestamp-based `FocusTimer`, Room-backed `FocusSessionManager` (start/pause/resume/complete/recovery), real foreground `FocusService` with notifications, `FocusWatchdogWorker`, Focus Setup + Focus Mode screens (Phase 2)
- App Restriction: manual app selection, honest Android-limitations disclosure + per-permission rationale UI, `UsageStatsManager`-based restriction engine, `BlockedAppActivity` interstitial, Emergency Unlock flow (Phase 3)
- Study Management: Subject/Unit/Topic/StudyMaterial/DocumentChunk models, PDF processing pipeline (pypdf, scanned-doc rejection, heading detection, chunking), full CRUD API with ownership isolation, docker-compose for Postgres+pgvector, Android Subjects/Materials/Topics screens with upload + status polling (Phase 4)
- Break System: recommendation engine (1h/2h/3h/4h/custom), timestamp-derived break availability, full scheduled/deferred/extension/unscheduled-request state machine, break-active + topic-checklist UI (Phase 5)
- Android auth UI: real Login/Register/Splash/Settings-logout screens, `AuthRepository`, OkHttp `AuthAuthenticator` for 401→refresh→retry with force-logout on unrecoverable session expiry
- AI Verification: `AIService` abstraction with a deterministic `MockAIProvider` and an `OpenAICompatibleProvider` seam for a real provider later; RAG retrieval (pgvector on Postgres, keyword-overlap fallback on SQLite); full quiz generate/attempt/submit/result API; Android rapid-fire quiz UI (`QuizRunner` timer/answer state machine) wired to real break-unlocking (Phase 6)
- Analytics: backend `TopicPerformance` tracking + weak-topic detection, Android local study/session/break stat aggregation, combined `AnalyticsScreen` dashboard with non-judgmental weak-topic recommendations (Phase 7)
- `backend/Dockerfile` (Phase 8 — was missing; docker-compose.yml only ever covered Postgres)

### Fixed
- Focus session `subjectId` was hardcoded to `1L` with no real Subject to back it — changed to a `String` holding the backend's real subject UUID; Focus Setup now requires picking (or creating) an actual subject
- Phase 5's break-verification stub never routed to BreakActiveScreen on a "pass" — fixed when wiring the real quiz result (Phase 6)
- `material_service.upload_and_process` leaked unhandled 500s on any AI/embedding failure instead of the documented `FAILED`/`ai_error` status (Phase 8)
- `GET /health` now actually checks DB connectivity (`SELECT 1`) instead of returning a hardcoded OK (Phase 8)
- Corrected a stale Android test count in `PROCESS.md` (117 → actual 116) (Phase 8)

### Verified (as of Phase 8)
- Android: `gradlew.bat assembleDebug` + `gradlew.bat test` — **116/116 unit tests pass**. `gradlew.bat assembleRelease` also succeeds, producing an unsigned APK.
- Backend: `pytest -v` — **43/43 tests pass** (auth, subjects, materials, topics, PDF pipeline, quiz, analytics — all against SQLite; pgvector/Postgres path still unexercised).

### Known gaps (honest MVP status — nothing here is swept under the rug)
- ~~pgvector never verified~~ — resolved in 0.1.1 (see above). Caveat: pytest still runs on SQLite; no automated Postgres-backed test yet.
- AI provider is a deterministic mock, not a real LLM — quiz questions/embeddings are template/hash-based placeholders, clearly labeled in code; a real provider must be dropped into the `OpenAICompatibleProvider` seam before shipping.
- Android instrumented tests (`connectedAndroidTest`) have **no real test code** — only the unmodified Android Studio template exists; the 8 scenarios in `docs/TESTING.md` were never written, and no emulator/device has been available across any phase.
- Android auth tokens stored unencrypted (plain DataStore) — flag before any real release.
- No server-side logout/token revocation on the backend (Android logout is local-only).
- Local file storage (`STORAGE_DIR`) stands in for S3 — not yet wired to real object storage.
- Release APK is unsigned; R8 minification disabled despite ProGuard rules being present.
- `backend/Dockerfile` (new in Phase 8) has never been build-tested against a live Docker daemon.
- No `ENDED_EARLY` session/break terminal state.
