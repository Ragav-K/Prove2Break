# Prove2Break — Notification System

## Notifications Needed

| Notification | Trigger | Priority |
|---|---|---|
| Focus session active (ongoing) | Session starts, updates as time passes | Low (silent, ongoing) — required foreground service notification |
| Scheduled break available | Break offset time reached | Default |
| Break ending soon | ~1 min before an active break's planned end | Default |
| Break ended, resuming focus | Break timer completes | Default |
| Unscheduled break: quiz ready | Quiz generation completes | Default |
| Quiz result | Quiz completes (pass/fail) | Default |
| Session completed | Planned duration reached / manually ended | Default |
| Material processing complete/failed | PDF pipeline finishes | Default |
| Emergency unlock confirmation | Emergency access used | Default |

## Channels

- **`focus_session_ongoing`** — low importance, silent, used only for the persistent foreground-service notification showing elapsed/remaining time and quick actions (Break Now, End Session).
- **`break_events`** — default importance, covers break-available, break-ending-soon, break-ended notifications.
- **`quiz_events`** — default importance, covers quiz-ready and quiz-result notifications.
- **`material_processing`** — default importance, covers upload/processing completion or failure.
- **`general`** — default importance, catch-all for anything not fitting the above (e.g. emergency unlock confirmation).

Each channel is created up front at app first-run (required on Android 8+), independently user-configurable (mutable/volume/vibration) via system notification settings, respecting the product's general non-alarming tone (no urgent/red styling on quiz-fail notifications, consistent with `PRODUCT.md`'s non-judgmental principle).

## Foreground Service Notification Requirement

Android requires an active foreground service to display an ongoing notification. `FocusSessionService` posts this on start via `focus_session_ongoing` and updates it in place (not by re-posting a new notification) as elapsed/remaining time changes, showing at minimum: session status, remaining time, and one or two quick actions. This notification cannot be dismissed by the user while the service is running (standard foreground-service notification behavior) — which is intentional, reinforcing that focus is currently active.

## Permission Handling

On Android 13+, posting most notifications (channels other than the exempt foreground-service one, in practice all of them need the runtime `POST_NOTIFICATIONS` permission to actually display) requires the runtime `POST_NOTIFICATIONS` permission — requested with a clear explainer at first relevant use (e.g. right before starting the first focus session), not at cold app launch. If denied, the app still functions (timer, restriction, quiz flow all continue), just without the visible reminders — communicated to the student so they aren't confused by "silent" behavior.
