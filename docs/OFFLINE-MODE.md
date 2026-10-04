# Prove2Break — Offline Mode

## Works Offline

- **Focus timer** — timestamp-authoritative (`FOCUS-ENGINE.md`), computed entirely from locally stored `startedAt`/`plannedDuration`; needs no network to keep running or to display correct elapsed/remaining time.
- **Existing session state** — an already-started `FocusSession` (and its break sub-state) is fully readable/resumable from local Room storage without connectivity.
- **Local session data recording** — study time, break events, and session completion are written locally first (offline-first) and synced to the backend when connectivity returns.
- **Notifications** — locally scheduled/posted notifications (break available, session complete) do not require network.
- **Break state (scheduled/deferred/active/extend)** — all pure client-side state transitions, no backend round-trip required to progress through them.
- **Cached quizzes** — if a quiz was already generated while online (e.g. pre-fetched or generated moments earlier), answering already-fetched questions works offline; but starting a *new* unscheduled break request requires generating a fresh quiz, which needs network (see below).
- **Local analytics** — recently recorded session/quiz data is viewable from the local cache even offline, though it may not reflect the very latest cross-device aggregate from the backend.
- **App restriction / blocking enforcement** — `UsageStatsManager`/Accessibility-based detection and the blocked-app interstitial are fully local and function without connectivity.

## Needs Network

- **PDF upload** — requires reaching the backend/S3 to store the file.
- **AI processing / topic extraction / quiz generation** — requires the backend, which in turn requires the configured AI provider; cannot be performed on-device.
- **Cloud sync** — pushing locally recorded session/quiz history to the backend, and pulling the latest cross-device analytics/study plan.
- **Login/registration/token refresh** — requires reaching the backend auth endpoints (an already-valid, unexpired access token can still be used offline for anything that doesn't itself need a network call).

## Communicating Limitations to the User

- A persistent, unobtrusive connectivity indicator (not a blocking modal) appears when offline, only on screens where it's actually relevant (upload, quiz request, sync-dependent analytics) — not globally nagging on the focus timer screen, since that screen works fine offline.
- If the student requests an unscheduled break while offline, the app explains plainly that generating a verification quiz needs a connection, and offers to keep the break request queued so it starts automatically once connectivity returns, rather than silently failing or misleadingly acting as if nothing happened.
- If a PDF upload is attempted offline, it's queued locally (via WorkManager) and clearly marked "will upload when back online," rather than erroring destructively.
