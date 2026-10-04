# Prove2Break — Analytics

## Metrics Tracked

- **Study time** — total and per-subject, derived from completed/ended `FocusSession` durations (excluding break time, since break time is tracked separately).
- **Focus sessions** — count, average length, completion rate (COMPLETED vs ENDED_EARLY).
- **Breaks** — count taken, count deferred/skipped, average break length, extension frequency.
- **Unscheduled break requests** — count, and outcome breakdown (pass vs fail on first attempt).
- **Verification attempts** — total quiz attempts, per subject and per topic.
- **Pass/fail** — overall pass rate, trend over time.
- **Scores** — per-attempt score distribution, per-topic average score.
- **Topic performance** — `TopicPerformance` rows: attempt count and rolling average score per topic per user.
- **Weak topics** — topics with `average_score < 0.70` (or the user's configured threshold), surfaced with gentle framing.
- **Consistency** — study-day streaks / session frequency over time, used to give the student a sense of momentum (not gamified scoring in MVP, just a factual streak count).

## Dashboard Composition

The Analytics Dashboard (`UX-UI.md` screen #23) presents, in priority order:

1. **Headline stats row** — total study time (this week / all-time toggle), sessions completed, current streak.
2. **Quiz performance summary** — overall pass rate, total attempts, trend sparkline.
3. **Per-subject breakdown** — study time and topic-completion progress per subject, as a compact list/table.
4. **Weak Topics card** — top N topics below threshold, gently framed ("A few topics could use another look"), linking to Weak Topics Detail (screen #24) and, from there, directly into that topic's source material or an unscheduled-break-style practice check.
5. **Break behavior summary** — scheduled vs unscheduled break ratio, average extension usage (helps the student notice their own patterns without being told what to do about it).

All dashboard data is computed from denormalized aggregates (`TopicPerformance`, session/quiz summary tables/queries) rather than scanning raw event history at render time, keeping the dashboard fast even as history grows.
