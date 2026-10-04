# Prove2Break — Requirements

## Functional Requirements

### Authentication (FR-001 – FR-005)
- **FR-001**: Users can register with email + password.
- **FR-002**: Users can log in and receive a JWT access token + refresh token.
- **FR-003**: Access tokens expire (default 30 min); refresh tokens expire (default 30 days) and can be rotated.
- **FR-004**: Users can log out, invalidating the stored refresh token client-side (and server-side blacklist optional for MVP).
- **FR-005**: Passwords are hashed server-side (bcrypt/argon2) and never stored or logged in plaintext.

### Subjects & Exams (FR-006 – FR-010)
- **FR-006**: Users can create a Subject with a name and optional description.
- **FR-007**: Users can set an optional exam date per Subject.
- **FR-008**: Users can edit or delete a Subject (delete cascades to its materials, topics, and sessions per `DATABASE.md`).
- **FR-009**: Users can view a list of their Subjects with progress summary (topics completed / total).
- **FR-010**: Subjects belong to exactly one user; no cross-user visibility.

### Study Materials & Processing (FR-011 – FR-018)
- **FR-011**: Users can upload a PDF file as study material under a Subject.
- **FR-012**: Only text-based PDFs are supported in MVP; scanned/image-only PDFs are rejected with a clear message.
- **FR-013**: Uploaded files are validated for type, size limit (default 50MB), and page count limit (default 500 pages) before processing.
- **FR-014**: The backend extracts text, cleans it, detects sections, and chunks it for embedding.
- **FR-015**: The backend generates topic candidates from the processed material via the `AIService.generateTopics` call.
- **FR-016**: Users can review, edit, merge, split, rename, or delete extracted topics before confirming them.
- **FR-017**: Users can mark a topic as "completed" (self-reported) at any time.
- **FR-018**: Processing failures (extraction, embedding) are surfaced to the user with a retry option.

### Focus Sessions (FR-019 – FR-025)
- **FR-019**: Users can start a focus session by choosing a duration (1h/2h/3h/4h/custom) and a Subject.
- **FR-020**: The focus timer is timestamp-authoritative (`startedAt` + `plannedDuration`), not a decrementing UI counter.
- **FR-021**: The focus session survives app backgrounding, screen lock, and process death, recoverable from local Room storage on restart.
- **FR-022**: A foreground service keeps the session alive and posts a persistent notification while focus is active.
- **FR-023**: Users can end a focus session early; partial session data is still recorded.
- **FR-024**: On planned-duration completion, the session transitions to a "completed" state and analytics are updated.
- **FR-025**: Only one focus session can be active per user at a time.

### App Restriction (FR-026 – FR-030)
- **FR-026**: Users manually select which installed apps to restrict before/at the start of a focus session.
- **FR-027**: The app requests only the permissions required for the restriction mechanisms actually used, and explains each one before requesting it.
- **FR-028**: When a restricted app is opened during focus, the user sees a blocked-app screen instead of (or overlaying) that app.
- **FR-029**: An emergency-access path exists to exit restriction in a genuine emergency, logged for the student's own record (not gated by AI/quiz).
- **FR-030**: The app is explicit in-product that blocking is best-effort, not an unbypassable guarantee (see `APP-RESTRICTION.md`).

### Breaks (FR-031 – FR-038)
- **FR-031**: The app recommends a break schedule based on session duration (see `BREAK-SYSTEM.md` table).
- **FR-032**: Users can adjust/skip a recommended break time.
- **FR-033**: At a scheduled break, users can take it, defer it, or continue studying (continuing never auto-starts a break).
- **FR-034**: Users can extend an active break by a bounded increment (subject to a max extensions/time cap).
- **FR-035**: Users can request an unscheduled break at any time during active focus.
- **FR-036**: An unscheduled break request requires the user to select which claimed-complete topics it should cover, then pass a generated quiz.
- **FR-037**: Passing the quiz (≥ threshold) immediately unlocks the break; failing keeps focus/restriction active and shows non-judgmental messaging.
- **FR-038**: All break state transitions follow the explicit state machine in `BREAK-SYSTEM.md`.

### Quiz / Verification (FR-039 – FR-046)
- **FR-039**: Quiz questions are generated via `AIService.generateQuiz`, grounded via RAG on the student's own uploaded material for the selected topics only.
- **FR-040**: Questions are presented one at a time, each with an individual countdown timer.
- **FR-041**: Questions auto-advance on timeout (counted as incorrect) or on answer submission; there is no back/skip/pause control.
- **FR-042**: Each question has exactly one correct answer among its options; the generation pipeline validates this before presenting.
- **FR-043**: Quiz score = percentage of questions answered correctly.
- **FR-044**: Pass threshold defaults to 70%, configurable per user (and overridable via server-side config for defaults).
- **FR-045**: Every quiz attempt (questions, answers, correctness, score, pass/fail, topics covered, timestamp) is recorded for analytics.
- **FR-046**: Quiz generation failures (e.g. AI provider error, insufficient material for the topic) are surfaced with a clear retry path, never silently blocking the break indefinitely without explanation.

### Analytics & Study Planning (FR-047 – FR-052)
- **FR-047**: The app tracks total and per-subject study time.
- **FR-048**: The app tracks focus session history (count, durations, completions vs early-ends).
- **FR-049**: The app tracks quiz history and per-topic performance (average score, attempt count).
- **FR-050**: Topics with average score < 70% are surfaced as "weak topics" using gentle, non-judgmental language.
- **FR-051**: A rule-based study planner suggests a study order using exam date, remaining topics, difficulty, weak topics, and available time (see `STUDY-PLANNER.md`).
- **FR-052**: Users can view a dashboard summarizing study time, session count, break stats, quiz pass rate, and weak topics.

## Non-Functional Requirements

| Category | Requirement | Measurable Criteria |
|---|---|---|
| **Performance** | Focus timer UI must reflect accurate elapsed/remaining time | Recomputed from timestamps on every resume; drift ≤ 1s vs wall clock |
| **Performance** | Quiz question generation latency | P95 ≤ 8s per quiz (up to 5 questions) from request to first question ready |
| **Performance** | PDF processing latency | P95 ≤ 60s for a 50-page text PDF, async with progress feedback beyond that |
| **Reliability** | Focus session must survive process death | 100% of sessions recoverable from Room on relaunch within the planned duration window |
| **Reliability** | Foreground service must not be killed silently without recovery | Service restart (via `START_STICKY`/WorkManager watchdog) resumes correct session state |
| **Security** | All network traffic encrypted | 100% of API calls over HTTPS/TLS 1.2+ |
| **Security** | Passwords never stored or logged in plaintext | Verified by code review + automated secret-scanning in CI |
| **Security** | JWT tokens stored securely on-device | Access/refresh tokens stored via Android `EncryptedSharedPreferences` or DataStore w/ encryption, never plain SharedPreferences |
| **Privacy** | Study material never logged in full | Logs contain only metadata (file id, page count, status), never raw extracted text |
| **Privacy** | No cross-user data exposure | Every query scoped by authenticated `user_id`; verified via integration tests |
| **Accessibility** | UI meets basic contrast/legibility standards | Material 3 color roles meet WCAG AA contrast; scalable text supported |
| **Accessibility** | Quiz timer is perceivable without relying on color alone | Countdown shown as both numeric text and a progress bar |
| **Maintainability** | AI provider swappable without touching business logic | All AI calls routed through `AIService` interface; verified by no direct SDK imports outside `ai/` module |
| **Maintainability** | Modular monolith package boundaries respected | Enforced via code review + package-structure lint rules described in `ANDROID-ARCHITECTURE.md`/`BACKEND-ARCHITECTURE.md` |
| **Offline behavior** | Focus timer, session state, and blocked-app enforcement continue offline | Verified by airplane-mode instrumentation test during an active session |
| **Offline behavior** | Features requiring network fail gracefully offline | Upload/quiz-generation/sync attempts show a clear "needs connection" message, no crash |
