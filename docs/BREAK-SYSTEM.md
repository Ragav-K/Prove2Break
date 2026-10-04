# Prove2Break — Break System

## Break Recommendation Table

| Focus Session Duration | Suggested Break Count | Suggested Break Timing | Suggested Break Length |
|---|---|---|---|
| 1 hour | 1 | at ~50 min | 5 min |
| 2 hours | 2 | at ~50 min, ~110 min | 5–10 min |
| 3 hours | 3 | roughly every 50–60 min | 10 min |
| 4 hours | 4 | roughly every 50–60 min | 10–15 min |
| Custom | `ceil(duration / 55min)` breaks | evenly spaced at ~55 min intervals | 5–10 min, scaled modestly with total duration |

These are recommendations, not enforced — the student can adjust, defer, or skip any scheduled break (`FR-032`, `FR-033`).

## Adjustability

- Recommended break times/lengths are editable at session setup (within sane bounds) before the session starts.
- Once a session is active, a scheduled break's timing is fixed for that occurrence, but the student can defer it (push to "ask again in N minutes") rather than take it immediately.

## Break Types

- **Scheduled break** — created up front from the recommendation table when the session starts; becomes `AVAILABLE` at its offset time.
- **Deferred break** — a scheduled break the student chose not to take immediately; re-offered after a short delay rather than disappearing, and never auto-starts.
- **Unscheduled break** — student-initiated at any time via "Break Now," gated by the quiz verification flow (`QUIZ-SYSTEM.md`).

**Rule:** "Continue studying" never auto-starts a break later on its own — every break, scheduled or not, requires an explicit student action to move from `AVAILABLE`/offered to `ACTIVE`.

## Break Extension

- While a break is `ACTIVE`, the student may request an extension in bounded increments (e.g. +5 min), up to a maximum number of extensions per break (default 2) and/or a maximum total break length, to prevent break creep from silently consuming the whole session.
- Extension requests during `BREAK_VERIFICATION`/`QUIZ_ACTIVE` are not applicable (there is no break yet to extend — the quiz itself is timed per-question instead).

## Break State Machine

### Primary path

```
IDLE
  │ (session starts)
  ▼
FOCUSING
  │ (scheduled break time reached)
  ▼
BREAK_AVAILABLE ──(defer)──> FOCUSING (re-offered later)
  │ (take break)
  ▼
BREAK_ACTIVE ──(extend)──> BREAK_ACTIVE (bounded)
  │ (break ends / student resumes)
  ▼
FOCUSING
  │ ... repeats for each scheduled break ...
  │ (planned duration elapsed)
  ▼
SESSION_COMPLETED
```

### Unscheduled path (branches from FOCUSING)

```
FOCUSING
  │ (student taps "Break Now")
  ▼
BREAK_VERIFICATION   (select claimed-complete topics)
  │
  ▼
QUIZ_ACTIVE          (rapid-fire quiz, one question at a time)
  │
  ├──(score >= threshold: PASS)──> BREAK_ACTIVE (break unlocked)
  │
  └──(score < threshold: FAIL)───> FOCUSING (break stays locked; gentle
                                     "keep studying and try again" message)
```

### Notes

- `BREAK_VERIFICATION` and `QUIZ_ACTIVE` are sub-states of an unscheduled break attempt — no restriction is lifted and no break time counts until `PASS` is reached.
- A `FAIL` result does not block future unscheduled attempts on different (or the same, after more study) topics — the student can simply try again once they've studied more, per the non-judgmental product principle.
- `SESSION_COMPLETED` is reached either by planned-duration elapsing or the student explicitly ending the session early (`ENDED_EARLY`, a distinct terminal state so analytics can distinguish full completions from early ends).
