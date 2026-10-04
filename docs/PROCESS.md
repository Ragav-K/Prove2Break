# Prove2Break — Project Process & Status

> This is the single source of truth for project status. Read it before starting work; update it after finishing work.

## Status Symbol Legend

| Symbol | Meaning |
|---|---|
| ⬜ | NOT STARTED |
| 🟡 | IN PROGRESS |
| 🟢 | COMPLETED |
| 🔴 | BLOCKED |
| 🟠 | NEEDS FIX |
| 🔵 | TESTING |

## Current Phase

**Phase 8 — QA (complete). All 8 numbered phases done.**

## Current Status

🟢 MVP COMPLETE (with honestly-documented open items — see Section 4/7 below before considering this release-ready)

## Overall Progress

**100% of the planned 8-phase MVP roadmap.** This does NOT mean release-ready — see "Known deferred items" for what still needs real work (unverified pgvector path, mock AI provider, unencrypted tokens, no S3, unsigned release build, no instrumented test coverage).

---

## Section 1: Master Checklist

### Phase 0 — Documentation
- [x] Project structure created (`android/`, `backend/`, `docs/`, root README)
- [x] All docs/ files created and populated
- [x] PROCESS.md established as source of truth

### Phase 1 — Foundation
- [x] Android project created
- [x] Compose configured
- [x] Navigation configured
- [x] Dependency injection configured
- [x] Theme created
- [x] Backend created
- [x] Database configured
- [x] Authentication implemented

### Phase 2 — Focus Engine
- [x] Focus session model
- [x] Focus timer
- [x] Background persistence
- [x] Foreground service
- [x] Session recovery
- [x] Session completion

### Phase 3 — App Restriction
- [x] Permission flow
- [x] App selection
- [x] Restriction engine
- [x] Blocked-app screen
- [x] Emergency access

### Phase 4 — Study Management
- [x] Subject creation
- [x] Exam date
- [x] Study material upload
- [x] PDF processing
- [x] Topic extraction
- [x] Topic completion

### Phase 5 — Break System
- [x] Break recommendation
- [x] Scheduled break
- [x] Deferred break
- [x] Break extension
- [x] Unscheduled break (request/topic-selection + state machine; AI quiz itself is a Phase 6 stub, see below)
- [x] Break state machine

### Phase 6 — AI Verification
- [x] RAG pipeline (real pgvector path on Postgres; keyword-overlap fallback on SQLite — see Known Deferred Items)
- [x] Quiz generation (mock deterministic provider behind real `AIService` abstraction; real-provider seam left in place)
- [x] Rapid-fire UI (one question full-screen, visible countdown, no back/skip/re-answer)
- [x] Question timer (`QuizRunner` pure state machine — tick/timeout auto-advances, records null answer on timeout)
- [x] Quiz scoring (backend)
- [x] Pass/fail (backend, against configurable threshold)
- [x] Break unlocking (`BreakManager.resolveQuizResult` wired to the real backend-scored result; also fixed a pre-existing Phase 5 gap where pass never routed to BreakActiveScreen)

### Phase 7 — Analytics
- [x] Study time (Android-local Room aggregation)
- [x] Focus sessions (session/completion/ended-early counts, streak days)
- [x] Quiz history (backend overview: attempts, pass rate, avg score)
- [x] Topic performance (backend `TopicPerformance`, rolling average, updated on quiz submission)
- [x] Weak topics (avg < 70% threshold, non-judgmental copy)
- [x] Dashboard (`AnalyticsScreen` — combines both data sources client-side)

### Phase 8 — QA
- [x] Unit tests (backend 43/43, Android 116/116 — both genuinely re-run and verified this phase, one stale count corrected)
- [x] Integration tests (auth, PDF pipeline, quiz generation, analytics — reviewed against docs/TESTING.md targets)
- [ ] Android instrumentation testing (**genuinely NOT done** — see below, corrected framing)
- [x] Permission testing (audited manifest + runtime flow vs. docs/PERMISSIONS.md; graceful denial confirmed)
- [x] Background execution testing (foreground service / AuthAuthenticator / restriction-monitor code paths traced and confirmed correct — not device-tested)
- [x] Error testing (5 scenarios traced through real code; found and fixed one real bug — see Completed Work)
- [x] Release build (attempted for real — succeeds but unsigned, see Completed Work)

---

## Section 2: Current Task

- **Task:** None — all 8 numbered development phases are complete. This section is intentionally idle pending the user's direction on what comes next (pre-release hardening items below, or new feature work).
- **Status:** idle
- **Last completed:** Phase 8 — QA, 2026-09-29

---

## Section 3: Completed Work

| Date | Feature | Status | Notes |
|---|---|---|---|
| 2026-09-28 | Phase 0 documentation foundation | 🟢 COMPLETED | Created full `docs/` structure (32 files), root README, .gitignore, backend/.env.example. No app code written — by design. |
| 2026-09-28 | Phase 1 — Android foundation | 🟢 COMPLETED | Gradle project (Kotlin, AGP 8.5.2, Compose BOM 2024.06.00) with Hilt, Navigation Compose, Room, DataStore, Retrofit/OkHttp, WorkManager deps. Material 3 theme (calm teal/amber palette). Full package skeleton per `ANDROID-ARCHITECTURE.md`. `./gradlew assembleDebug` and `./gradlew test` both verified **BUILD SUCCESSFUL** (2/2 unit tests passing). compileSdk/targetSdk set to 36 (no SDK 35 available on build machine) — environment detail, not architectural. |
| 2026-09-28 | Phase 1 — Backend foundation + auth | 🟢 COMPLETED | FastAPI app (`app/main.py`), pydantic-settings config matching `.env.example` exactly, SQLAlchemy + psycopg3 (chosen for future pgvector compatibility), Alembic initialized with hand-written initial `users` migration, argon2id password hashing (argon2-cffi — no bcrypt 72-byte truncation issue), JWT access+refresh auth (`/api/auth/register|login|refresh`), no user-enumeration on login/refresh failures. Real venv + `pip install` + `pytest -v` run: **9/9 tests passed**. Tests use in-memory SQLite (documented tradeoff — pgvector-backed tables will need real Postgres in a later phase). |
| 2026-09-28 | Phase 2 — Focus Engine | 🟢 COMPLETED | Timestamp-only `FocusTimer.computeSnapshot()` (never a decrementing counter), Room-backed `FocusSessionManager` (start/pause/resume/complete + recovery), real foreground `FocusService` (START_STICKY, ongoing + completion notifications), `FocusWatchdogWorker` (Hilt CoroutineWorker, WorkManager, restarts service if killed), Focus Setup (duration picker 1h/2h/3h/4h/custom) and Focus Mode screens wired into nav. `./gradlew test`: **14/14 passed** (FocusTimer 8, FocusSessionManager 3, StartFocusSessionUseCase 3). `./gradlew assembleDebug`: **BUILD SUCCESSFUL**. `connectedAndroidTest` still not run (no emulator attached — see Known Deferred Items). Known gaps: session subjectId hardcoded to `1L` pending Phase 4 Subjects; runtime `POST_NOTIFICATIONS` request deferred to Phase 3 (owns permissions UX). |
| 2026-09-30 | Postgres/pgvector verification (post-MVP) | 🟢 COMPLETED | Closed the long-standing Docker gap. Port 5432 was already taken by a native Windows `postgres.exe` service (which is why password auth failed against the container), so `docker-compose.yml` now maps the container to host port **5433**. All migrations applied; full RAG loop driven end-to-end on real Postgres. Found+fixed two real bugs (missing `topic_performance` migration; broken pgvector `cosine_distance`). Backend `pytest`: still 43/43. Schema-drift check (`alembic check`) shows only intentional/cosmetic diffs: the hand-written `ivfflat` index (not declared on the model) and JSONB-vs-JSON on two `questions` columns. |
| 2026-09-29 | Phase 8 — QA | 🟢 COMPLETED | Genuine audit, not a rubber stamp. **Bug found & fixed:** `material_service.upload_and_process` had zero exception handling around per-chunk `AIService.embed_text()` — any AI/embedding failure propagated as an unhandled 500 instead of the documented `FAILED`/`ai_error` status; fixed with a proper try/except + rollback of partial topics, new regression test added. **Also fixed:** `/health` was a hardcoded `{"status":"ok"}` with no DB check, contradicting `DEPLOYMENT.md`; now runs `SELECT 1` and reports reachability. **Test counts corrected:** backend **43/43** (was 42, +1 regression test); Android **116/116** (PROCESS.md previously said 117 — stale/miscounted, not a missing test, corrected here). **Instrumented tests — honest correction:** `androidTest/` contains only the unmodified Android Studio template; none of docs/TESTING.md's 8 instrumented scenarios have real test code yet (previous phases' "deferred" framing undersold this — it's "unwritten," not "written but unrun"). Still no emulator/device available (4th consecutive failed attempt across phases 1/4/6/8, consistent pattern). Error-handling and permission audits traced real code paths (not assumptions) — 4 of 5 scenarios matched docs as-is, 1 was the bug above. `gradlew.bat assembleRelease`: **BUILD SUCCESSFUL**, unsigned APK (~11.9MB) — no keystore/signing config exists, R8 minification currently disabled despite ProGuard rules being present. Created `backend/Dockerfile` (previously missing — docker-compose.yml only covered Postgres) — not build-tested (Docker daemon still broken, 4th failed attempt). |
| 2026-09-29 | Phase 7 — Analytics | 🟢 COMPLETED | **Data-split architecture confirmed:** Android-side aggregation in `AnalyticsViewModel` (concurrent fetch via `async`), combining Room-local focus/break stats with backend quiz/topic-performance stats — never merged server-side, matching the recommended approach from the prior "Known deferred items" note. Backend: `TopicPerformance` model (rolling average, `WEAK_TOPIC_THRESHOLD = 0.70`), updated automatically on quiz submission (hooks into existing `quiz_service.submit_attempt`, no scoring-logic duplication); `GET /api/analytics/{overview,topics,weak-topics}`. Android: `GetLocalStudyStatsUseCase` (pure aggregation — study time excluding paused/break time, streak days, ended-early vs completed counts, break behavior stats), `AnalyticsScreen` combining both sources with non-judgmental weak-topic copy, linked from Dashboard via a new "Progress" button (Dashboard itself remains a Phase-1 placeholder). **Backend `pytest -v`: 42/42 passed.** **Android `gradlew.bat test`: 117/117 passed**, `assembleDebug`: **BUILD SUCCESSFUL**. |
| 2026-09-29 | Phase 6 — AI Verification (Android half) | 🟢 COMPLETED | `QuizRunner` — pure, dependency-free rapid-fire timer/answer state machine (tick/timeout auto-advances, records `null` on timeout, no back/skip/re-answer), unit-testable without coroutines/mocks. `RapidFireQuizScreen` (one question full-screen, visible countdown) + `QuizViewModel` (generate→attempt→timed loop→submit→`BreakManager.resolveQuizResult`, replacing the Phase 5 stub's dev pass/fail buttons with the real backend-scored result). Result copy verified non-judgmental per `PRODUCT.md` ("Break remains locked — keep studying and try again"). Deleted `VerificationStubScreen.kt`. **Bonus fix:** the Phase 5 stub never actually routed to BreakActiveScreen on pass — that's now fixed as part of this wiring. `gradlew.bat test`/`assembleDebug`: **both BUILD SUCCESSFUL**. Known minor limitation: ~1s of timer-budget drift possible if a student answers mid-tick (never negative, never double-charged) — acceptable, not a scoring bug. |
| 2026-09-29 | Phase 6 — AI Verification (backend half) | 🟢 COMPLETED | `AIService` abstract interface (was pre-existing from a prior killed run, verified correct) implemented by `MockAIProvider` (deterministic, template-based quiz generation from retrieved chunk text + SHA-256-hashed bag-of-words embeddings — explicitly **not real AI**, code comments say so) and an `OpenAICompatibleProvider` seam (reads `AI_PROVIDER`/`AI_API_KEY`/`AI_MODEL_NAME`, raises `NotImplementedError` — ready for a real provider later) via `get_ai_service()` factory. `app/ai/retrieval.py`: real pgvector cosine-distance search on Postgres, keyword-overlap fallback on SQLite (documented approximation). Added `topic_id` FK to `DocumentChunk` (needed for per-topic retrieval, wasn't in Phase 4's model). Full quiz generation→attempt→submit→result API with all 5 QUIZ-SYSTEM.md validation rules + ownership isolation. `pytest -v`: **37/37 passed** (29 existing + 8 new), all on SQLite. **Docker/Postgres still not runnable** in this environment (`docker compose up -d` retried, same daemon-not-running failure as Phases 1/4) — migration syntax-checked only, pgvector ANN search unverified live. `docs/API.md`/`docs/AI-RAG.md` updated to reflect actual endpoint shapes and the mock-provider/SQLite-fallback implementation status. |
| 2026-09-28 | Android auth UI | 🟢 COMPLETED | Real `/api/auth/{register,login,refresh}` wiring; `TokenStore` interface + DataStore-backed `AuthTokenStore` (access+refresh); `AuthRepository` (register auto-logs-in since `RegisterResponse` carries no tokens); `AuthAuthenticator` (OkHttp) handles 401→refresh→retry-once via a separate non-recursive plain client, force-logout on unrecoverable refresh failure via `AuthSessionManager` SharedFlow observed at the NavHost root (clears back stack to Login). Real Splash (checks auth state), Login, Register, Settings-with-Logout screens. `gradlew.bat test`/`assembleDebug`: **both BUILD SUCCESSFUL**. **Known gap (security-relevant):** tokens stored in plain, non-encrypted DataStore — no `androidx.security:security-crypto` dependency was added (out of scope for this pass); isolated behind the `TokenStore` interface so swapping to encrypted storage is a one-file change. Should be revisited before any real release per `SECURITY.md`. Also: backend has no `/api/auth/logout` endpoint, so Android logout is local-only (no server-side token revocation). |
| 2026-09-28 | Phase 5 — Break System | 🟢 COMPLETED | Pure `BreakRecommendation` engine (exact 1h/2h/3h/4h table + custom-duration formula, user-editable); `BreakSchedule.currentAvailableBreak` derives break-availability from elapsed time on the fly (never persisted/polled, consistent with the timestamp-only philosophy); `BreakManager` (Hilt) owns the full state machine incl. FOCUSING→BREAK_AVAILABLE→BREAK_ACTIVE→FOCUSING, "Continue Studying" leaving the break untouched (proven by a dedicated test), and FOCUSING→BREAK_VERIFICATION→QUIZ_ACTIVE for the unscheduled path; break extension budget (`breakCount × 2 × 5min` pool) clamped so it can never be exceeded; `FocusSessionStatus` extended with BREAK_VERIFICATION/QUIZ_ACTIVE; Break-active screen, topic-checklist + **stub** verification screen (`VerificationStubScreen.kt` — explicitly marked `TODO(Phase 6)`, the entire file Phase 6 will delete and replace with the real rapid-fire quiz UI); FocusService notification now reflects break state. `gradlew.bat test`: **76/76 passed**. `gradlew.bat assembleDebug`: **BUILD SUCCESSFUL**. Known gap: no `ENDED_EARLY` session-level terminal state yet (session-level concern, out of this phase's scope, flagged for analytics phase). |
| 2026-09-28 | Phase 4 — Study Management (Android half) | 🟢 COMPLETED | Retrofit endpoints + Gson DTOs for subjects/materials/topics matching backend schemas field-for-field; minimal auth stub (`AuthTokenStore` + `AuthInterceptor`, DataStore-backed — Android login/register UI still doesn't exist, tracked as a gap); repositories/domain models/use cases (`CreateSubjectUseCase`, `UploadMaterialUseCase` with status-polling state machine incl. timeout, topic toggle/rename); Compose screens (Subjects list w/ progress bars, Subject create/edit incl. date picker, Subject detail w/ PDF upload via `ActivityResultContracts.OpenDocument`, Topic list grouped by unit w/ inline rename). Fixed the Phase 2 `subjectId = 1L` hardcode properly — changed focus-session subjectId from `Long` to `String` throughout (Room→backend UUID mismatch), Focus Setup now picks a real subject or prompts to create one. `./gradlew test` and `./gradlew assembleDebug`: **both BUILD SUCCESSFUL**, all suites green (new + all prior). |
| 2026-09-28 | Phase 4 — Study Management (backend half) | 🟢 COMPLETED | `Subject/Unit/Topic/StudyMaterial/DocumentChunk` SQLAlchemy models; `EmbeddingVector` cross-dialect column type (real pgvector `Vector(1536)` on Postgres, JSON fallback on SQLite — mirrors the existing `GUID` pattern); Alembic migration hand-written (enables `CREATE EXTENSION IF NOT EXISTS vector`, creates all 5 tables + ivfflat index) but **not yet executed against real Postgres** — Docker Desktop daemon wasn't running in this environment, so `docker-compose up` and the live migration are an open gap; PDF pipeline made real (pypdf, text-density heuristic rejects scanned/non-text PDFs, heading-regex section detection, ~800-char/150-overlap chunking); full CRUD API for subjects/materials/topics with ownership isolation (404 for not-owned, not just 403). `pytest -v`: **29/29 passed** (9 existing auth + 20 new) against SQLite. Uploaded PDFs stored on local disk via new `STORAGE_DIR` (interim — S3 wiring from `.env.example`'s `S3_*` vars still the documented target, not yet implemented) — see `ENVIRONMENT.md`. |
| 2026-09-28 | Phase 3 — App Restriction | 🟢 COMPLETED | `BlockedApp`/`EmergencyUnlock` Room entities+DAOs; `InstalledAppsRepository` (PackageManager); honest-limitations disclosure + per-permission rationale cards before any system prompt (fulfills Phase 2's deferred `POST_NOTIFICATIONS` request too); restriction engine uses **UsageStatsManager polling** (2s interval from `FocusService`), not AccessibilityService — documented real limitations: brief flash-before-block latency, requires manual Usage Access grant (no runtime dialog exists for it), vulnerable to OEM battery-optimization service kills, circumventable by a determined user (Safe Mode/uninstall/secondary profile) — app never claims otherwise. `BlockedAppActivity` full-screen interstitial + emergency-unlock reason picker (Academic/Emergency/Communication/Other) writing a record before granting any access. `./gradlew test`: **all passing**, including new `RestrictionCheckerTest` (8) and `EmergencyUnlockPolicyTest` (8). `./gradlew assembleDebug`: **BUILD SUCCESSFUL**. `connectedAndroidTest` still not run (no emulator). Room DB bumped to v2 with `fallbackToDestructiveMigration()` (acceptable pre-release, no shipped installs). |

---

## Section 4: Blocked Items

| Date Raised | Item | Blocking Reason | Owner | Status |
|---|---|---|---|---|
| — | — | — | — | — |

---

## Section 5: Bugs

| Date Found | Bug | Severity | Status | Notes |
|---|---|---|---|---|
| 2026-09-29 | `material_service.upload_and_process` leaked unhandled 500s on any `AIService.embed_text()` failure instead of setting the documented `FAILED`/`ai_error` status | Medium (correctness vs. documented contract; would matter a lot once a real, fallible LLM provider replaces the mock) | 🟢 FIXED | Wrapped in try/except, rolls back partial topics/units, sets FAILED + reason. Regression test added (Phase 8). |
| 2026-09-29 | `GET /health` did not check DB connectivity, contradicting `DEPLOYMENT.md` | Low | 🟢 FIXED | Now runs `SELECT 1`, reports reachable/unreachable. |
| 2026-09-30 | **No Alembic migration for `topic_performance`** (Phase 7 added the model only). On real Postgres, quiz submission and all `/api/analytics/*` endpoints would fail with "relation does not exist"; tests passed only because they build the schema via `create_all` on SQLite. | High | 🟢 FIXED | Added migration `b3c4d5e6f7a8_topic_performance.py`; verified on real Postgres. |
| 2026-09-30 | **pgvector similarity search crashed** (`AttributeError: ... no attribute 'cosine_distance'`): `EmbeddingVector` is a `TypeDecorator` and hid pgvector's comparator, so `POST /api/quiz/generate` returned 500 on Postgres. Never caught because retrieval on SQLite uses the keyword fallback. | High | 🟢 FIXED | Added a `comparator_factory` exposing `cosine_distance` (`<=>`) in `app/models/types.py`; quiz generation verified on real Postgres. |
| 2026-09-29 | `docs/PROCESS.md` claimed 117 Android unit tests; actual was 116 | Low (documentation accuracy only, no missing/broken test) | 🟢 FIXED | Corrected in this file. |

---

## Section 6: Important Decisions

| Decision | Rationale |
|---|---|
| **Modular monolith, not microservices** | Single FastAPI service with clearly separated internal modules (auth, subjects, materials, focus, breaks, quiz, analytics). Avoids premature distributed-systems complexity for a single-team MVP; internal boundaries are enforced by module/package structure, not network hops. |
| **Android-only MVP** | Focus effort on one platform to reach a working product faster. Backend and API are designed API-first and platform-agnostic so iOS can consume the same backend later without rework. |
| **Provider-agnostic `AIService` abstraction** | All AI calls (topic extraction, quiz generation, study plan/recommendations) go through a single `AIService` interface. No code outside that boundary knows which LLM vendor is in use, so the provider can be swapped (or multiple providers used) without touching business logic. |
| **PostgreSQL + pgvector for RAG** | Keeps vector search inside the same transactional database as the rest of the app's relational data instead of standing up a separate vector store, simplifying operations for an MVP. |
| **Room for local focus-session persistence** | Focus session state must survive process death, so the authoritative in-progress session record is written to Room (not just an in-memory ViewModel or Service field) and re-read on process/service restart. |
| **Timestamp-based focus timer, not a decrementing counter** | The timer stores `startedAt` + `plannedDuration` and derives `elapsedDuration`/`remainingDuration` by comparing to current time. A decrementing counter drifts or resets under process death, Doze, or clock changes; a timestamp-based model is always recomputable and correct. |
| **JWT auth (access + refresh tokens)** | Stateless, standard, works well with a mobile client that needs to survive app restarts without re-prompting for credentials constantly. |
| **70% quiz pass threshold, configurable** | Default default is a placeholder tuned for "meaningfully learned it" without being punitive; made configurable (per-user or server-side config) since the right bar may need tuning post-launch. |
| **Non-judgmental failure messaging** | Product principle: verification failure is never framed as a personal failure ("you failed"). Always framed as the break remaining locked pending more study — see `PRODUCT.md` for exact phrasing rules. |
| **Manual app-restriction selection with honest limitation disclosure** | Android does not give third-party apps a guaranteed, tamper-proof way to block other apps. Rather than overpromise, the app is transparent in-product about what blocking can and cannot guarantee (see `APP-RESTRICTION.md`), and lets the student pick which apps to restrict rather than guessing. |

---

## Section 7: Next Actions

All 8 numbered phases are complete. Remaining work is pre-release hardening, not new feature phases:

1. ~~Resolve the Docker/pgvector gap~~ — done 2026-09-30 (see Completed Work). Follow-up: add an automated Postgres-backed test for the pgvector retrieval path.
2. **Replace the mock AI provider** (`MockAIProvider`) with a real LLM/embedding provider via the existing `OpenAICompatibleProvider` seam in `app/ai/factory.py` — quiz questions and embeddings are currently template/hash-based placeholders, not genuinely AI-generated.
3. **Write real instrumented tests** (`androidTest/`) for the 8 scenarios in `docs/TESTING.md` (background execution, screen lock, process death, notifications, service restart, blocked-app behavior, emergency unlock, permission denial) — currently only Android Studio's default template exists. Requires an emulator/device to actually execute, which has never been available in this environment.
4. **Encrypt Android token storage** (`androidx.security:security-crypto`) — currently plain DataStore, isolated behind `TokenStore` for an easy swap.
5. **Wire real S3** to replace the local-disk `STORAGE_DIR` stand-in.
6. **Add server-side logout/token revocation** — currently local-only on Android.
7. **Set up release signing** (real upload keystore + `keystore.properties` + `signingConfig` in `build.gradle.kts`) and enable R8 minification (currently `isMinifyEnabled = false` despite ProGuard rules being present) before any real release build.
8. Smaller/optional: decide on an `ENDED_EARLY` session/break terminal state if analytics ever needs to distinguish it; make the Dashboard screen (still a Phase-1 placeholder) a real home screen; build out the curated "commonly distracting apps" shortlist (Phase 3 polish item).

## Known deferred items (final status as of Phase 8 close, 2026-09-29)

- ~~Docker/pgvector unverified~~ **RESOLVED 2026-09-30.** Docker Desktop was started; `docker compose up -d` works, all 4 Alembic migrations apply cleanly on real Postgres 16 + pgvector, and the full upload→topics→pgvector retrieval→quiz→submit→analytics flow was driven end-to-end against it. Two real bugs were found and fixed in the process (see Section 5). Remaining caveat: the pytest suite itself still runs on SQLite (keyword-overlap retrieval fallback); the pgvector path is verified by a manual end-to-end run, not by an automated Postgres-backed test — adding one is a worthwhile follow-up.
- **AI is a deterministic mock**, not a real LLM/embedding model (`MockAIProvider` in `app/ai/providers.py`). Quiz questions are template-generated from chunk text; embeddings are hashed bag-of-words vectors. Clearly labeled in code, not a bug — but must not be mistaken for a finished AI feature. Swap in a real provider via the existing `OpenAICompatibleProvider` seam.
- **Android instrumented tests (`connectedAndroidTest`) have no real test code** — `androidTest/` contains only the unmodified Android Studio template. This was under-described in earlier phases as merely "not run"; the honest status (confirmed in Phase 8) is that the 8 scenarios in `docs/TESTING.md` were never written, not just never executed. No emulator/device has been available in this environment across all 8 phases.
- **Android auth tokens stored unencrypted** (plain DataStore, no `security-crypto`) — isolated behind `TokenStore` interface for an easy swap; flag before any real release.
- No server-side logout/token revocation endpoint — Android logout is local-only.
- Local file storage (`STORAGE_DIR`) used as an interim stand-in for S3 — real S3/MinIO wiring not yet implemented (env vars already defined in `.env.example`).
- **Release APK builds successfully but is unsigned** — no keystore/signing config exists; R8 minification is disabled (`isMinifyEnabled = false`) despite ProGuard rules being present in the project.
- **`backend/Dockerfile` was created in Phase 8** (previously missing entirely) but has never been build-tested against a live Docker daemon, for the same reason as the pgvector gap above.
- No `ENDED_EARLY` session/break terminal state — ending early currently reconciles into existing states; revisit only if a future need arises.
- App-selection list (Phase 3) shows all launchable apps rather than a curated "commonly distracting" shortlist at the top — functional, minor UX polish item.
- Two Kotlin deprecation warnings from Phase 3 (`UsageEvents.Event.MOVE_TO_FOREGROUND`, `AppOpsManager.unsafeCheckOpNoThrow`) — both are the correct stable APIs for minSdk 26; left as warnings, not suppressed.
- Dashboard screen is still the Phase-1 placeholder; Analytics ended up as a separate linked screen instead of replacing it.
- **Data split for analytics (new, important for Phase 7):** FocusSession/ScheduledBreak/BreakBudget live only in Android Room (Phase 2/5 decision — local-first, no backend sync). Quiz/QuizAttempt/QuizAnswer/TopicPerformance live only on the backend (Phase 6). Phase 7's dashboard must reconcile both sources (e.g. either sync focus-session summaries to the backend, or build the dashboard as an Android-side aggregation that also calls the backend quiz/topic endpoints) — this wasn't decided yet and should be an explicit architectural decision logged in Section 6 once Phase 7 starts.
