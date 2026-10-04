# Prove2Break — Study Planner

## MVP: Rule-Based, Explicitly Not ML

The Study Planner for MVP is a deterministic, rule-based scheduler — **not** an AI/ML recommendation engine. This is a deliberate scope decision (see `PROCESS.md` Decisions): a simple, explainable rule set is enough to be genuinely useful, ships faster, and is trivially debuggable, whereas an adaptive ML planner is real future scope once there's usage data to train/tune against (`FUTURE-ROADMAP.md`).

## Inputs

- **Exam date** (`Subject.exam_id -> Exam.exam_date`) — if absent, the planner falls back to a generic "no deadline" ordering (weak topics + difficulty only, no date-based pacing).
- **Remaining topics** — all `Topic` rows for the subject where `is_completed = false`.
- **Difficulty** — MVP proxy for difficulty: topic length/chunk count (more source material ≈ assumed more content to learn) combined with any explicit difficulty tag if the student sets one; not AI-inferred difficulty in MVP.
- **Weak topics** — from `TopicPerformance`, topics already attempted with `average_score < threshold` are prioritized for review even if marked complete.
- **Available time** — student-entered or inferred from recent session history (average daily study time over the last N days), used to estimate how many topics can realistically be covered before the exam date.

## Algorithm (v1, `algorithm_version = "rule-based-v1"`)

1. Compute days remaining until `exam_date` (or a default planning horizon if no date is set).
2. Compute available study sessions between now and the exam date, using recent average daily study time (or a conservative default if no history exists yet).
3. Build a candidate list: all incomplete topics + all weak topics (even if marked complete), deduplicated.
4. Sort the candidate list by a simple weighted score: `weak_topic_bonus (highest priority) > higher_difficulty_proxy > earlier_unit_order_index (respect original material structure) > topic_creation_order (stable tiebreak)`.
5. Greedily allocate topics to available study sessions in that sorted order until either all topics are allocated or available time runs out (in which case the plan flags "N topics may not fit before your exam" honestly rather than silently truncating).
6. Persist as a `StudyPlan.plan_data` JSON: ordered list of `{topic_id, suggested_session_index, estimated_minutes}`.

## Output Presentation

The Study Planner screen (`UX-UI.md` #25) shows the ordered list as a simple checklist mapped to "study this next," refreshed (regenerated) whenever topics change, a quiz attempt updates weak-topic status, or the exam date changes.

## Explicitly Future Scope (see `FUTURE-ROADMAP.md`)

- ML-based adaptive planning that learns from an individual student's actual retention curve.
- Spaced-repetition scheduling tuned per topic based on historical forgetting patterns.
- Cross-subject time-budget balancing when a student has multiple concurrent exams.
