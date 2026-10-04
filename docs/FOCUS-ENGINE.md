# Prove2Break — Focus Engine

## Supported Durations

1 hour, 2 hours, 3 hours, 4 hours, and a custom duration (user-entered, within a sane min/max bound, e.g. 15 minutes to 8 hours) selected at session start.

## Timestamp-Authoritative Design

The timer's source of truth is **never a decrementing in-memory counter**. Instead, each `FocusSession` stores:

- `startedAt` (absolute timestamp)
- `plannedDuration` (fixed at session start)
- derived at any moment: `elapsedDuration = now - startedAt` (minus any paused/break time if the design treats breaks as pausing the focus clock — decided at implementation: MVP treats the focus clock as continuing to run in the background during a break, since a "focus session" duration is wall-clock, while break time is tracked separately per `Break` records)
- derived: `remainingDuration = plannedDuration - elapsedDuration`
- `status` (FOCUSING / BREAK_AVAILABLE / BREAK_ACTIVE / BREAK_VERIFICATION / QUIZ_ACTIVE / COMPLETED / ENDED_EARLY)

Every UI read of "time remaining" recomputes from `startedAt` + `plannedDuration` against the current clock — it never trusts a previously displayed number decremented locally. This makes the timer immune to being backgrounded, having its process killed, or the UI simply not running for a while: whenever it's read next, it's correct.

**Why not a decrementing counter:** a counter that ticks down in a `while` loop or `Handler.postDelayed` chain drifts (missed ticks under CPU throttling/Doze), and worse, resets to a wrong value if the process restarts and the counter's last value wasn't itself derived from a timestamp. Timestamp-based derivation has no such failure mode — it's always recomputable and correct given the two facts (`startedAt`, `plannedDuration`) plus current time.

## Survival Requirements

The focus session must correctly survive:

1. **Backgrounding** — app moved to background, timer continues via the foreground service; UI recomputes correct elapsed time when reopened.
2. **Screen lock** — service keeps running; no dependency on screen-on state.
3. **Process death** — OS kills the app process (e.g. low memory). On next launch/service restart, the app reads the active `FocusSessionEntity` from Room and recomputes state from `startedAt`/`plannedDuration`/`status` — no data lost, no discontinuity visible to the user.
4. **App restart** — same recovery path as process death; if `remainingDuration <= 0` was reached while the app was dead, the session is immediately reconciled to `COMPLETED` on next read, with analytics recorded as if it had completed on time (best-effort backfill).

## Foreground Service + WorkManager Design

- `FocusSessionService` (foreground service) owns the live session for as long as it's active: posts/updates the ongoing notification, listens for break/session-end intents, and is the single place that decides "based on current time and session state, what should the UI/notification show right now."
- A WorkManager watchdog (periodic `CoroutineWorker`, or an expedited one-off scheduled at critical transition points like the next break time) checks: is there an active `FocusSessionEntity` but no running service? If so, it restarts the service — recovering from the OS having killed the service outright (not just the process).
- The watchdog is also responsible for firing break-availability notifications precisely even if the app/service isn't the one actively computing the next trigger at that exact moment (e.g. using `AlarmManager`/`WorkManager` exact-timing APIs appropriate to target SDK constraints).

## Room Persistence Rationale

Room is the durable local store for `FocusSessionEntity` specifically because:
- It must be readable synchronously-enough on process/service start, before any network call could complete — a session recovery cannot block on connectivity.
- It's the natural place to also track locally-computed break sub-state (which breaks have been offered/taken/skipped) so the UI is instantly consistent on cold start.
- It gives WorkManager/foreground-service code a single, testable data source without duplicating recovery logic across two different in-memory representations.

Session data is synced to the backend (for cross-device analytics, in future) opportunistically when connectivity allows, but the local Room record is authoritative for "is a session currently active and what state is it in" at any given instant on-device.
