# Prove2Break — Testing Strategy

## Unit Test Targets

- Focus timer math (elapsed/remaining derivation from `startedAt`/`plannedDuration` across normal, boundary, and post-completion cases).
- Break recommendation calculation (offsets/durations per the `BREAK-SYSTEM.md` table, including the custom-duration `ceil` formula).
- Break extension logic (increment application, cap enforcement).
- Break state machine transitions (every edge in `BREAK-SYSTEM.md`'s diagrams, including invalid-transition rejection).
- Quiz scoring (score calculation, threshold comparison, timed-out-answer-as-incorrect handling, exact-threshold boundary).
- Pass/fail decision logic and break-unlock side effect.
- Topic performance aggregation (rolling average update on new `QuizAttempt`, weak-topic threshold check).

## Integration Test Targets

- **Auth** — register → login → authenticated request → token refresh → logout, including invalid-credential and expired-token paths.
- **PDF processing** — upload → each pipeline stage → final `READY`/`FAILED` status, including a deliberately-scanned/image-only fixture PDF to verify rejection.
- **Topic extraction** — processed material → `AIService.generateTopics` (using a fake/mock `AIService` in test) → persisted `Topic`/`Unit` records.
- **Quiz generation** — selected topics → chunk retrieval (pgvector) → `AIService.generateQuiz` (mocked) → schema validation → persisted `Quiz`/`Question`, including the insufficient-grounding-material failure path.
- **Progress tracking** — full loop: start session → complete topic → request unscheduled break → pass/fail quiz → verify `TopicPerformance`/analytics aggregates update correctly end-to-end.

## Phase 8 QA Status (2026-09-29)

- **Backend**: `pytest -v` — **43/43 passed** (42 pre-existing + 1 new regression test for the AI-embedding-failure path, see `docs/ERROR-HANDLING.md`). Coverage against the targets above is genuinely complete for unit + integration targets listed here, on SQLite. Postgres/pgvector-specific behavior (real ANN search vs. the keyword-overlap fallback) remains unverified — see `PROCESS.md` Known Deferred Items.
- **Android**: `gradlew.bat test` (`testDebugUnitTest`) — **116/116 passed** (not 117 as previously logged in `PROCESS.md`; the actual `@Test`-annotated method count in `app/src/test` is 116 — a stale count, not a missing/broken test).
- **`connectedAndroidTest` (instrumented)**: still never executed — no emulator/AVD or physical device was available in this environment (`adb devices` empty, `emulator -list-avds` empty). Verified instead that `app/src/androidTest/` compiles cleanly (`gradlew.bat compileDebugAndroidTestSources` and `assembleDebug` both `BUILD SUCCESSFUL`). **Important correction to prior phases' framing**: `app/src/androidTest/java/com/prove2break/ExampleInstrumentedTest.kt` is the *only* file under `androidTest/` — it's the unmodified Android Studio template test (asserts the app's package name). None of the instrumented scenarios listed below have actual test code written for them yet; "deferred, never run" is optimistic phrasing carried over from earlier phases — the more accurate status is "specified here, but no test code exists beyond the default template." Writing real instrumented tests for these scenarios (foreground service continuity, process-death recovery, blocked-app interstitial, emergency unlock, notification channels, permission-denial UI) is real remaining work, not just an execution gap.

## Android Instrumentation Test Targets

- **Background execution** — start a focus session, background the app, verify the foreground service keeps running and the timer stays accurate on foregrounding again.
- **Screen lock** — lock the device during an active session; verify session continuity on unlock.
- **App restart / process death** — force-kill the app process mid-session; verify `FocusSessionEntity` recovery reproduces correct elapsed/remaining state.
- **Notifications** — verify the ongoing focus notification appears/updates, and that break/quiz-result notifications post correctly through their channels.
- **Permission denial** — deny Usage Access/Notifications; verify the rest of the app (timer, quiz, analytics) continues functioning and the UI clearly reflects the disabled feature rather than crashing.
- **Service restart** — simulate the OS killing `FocusSessionService` directly (not the whole process); verify the WorkManager watchdog restarts it and state is consistent.
- **Blocked-app behavior** — with restriction configured and Usage Access granted, launch a restricted test app during an active session and verify the blocked-app interstitial appears; verify no interstitial appears for non-restricted apps.
- **Emergency unlock** — trigger emergency access mid-session; verify restriction lifts immediately without requiring a quiz, and an `EmergencyUnlock` record is created.
