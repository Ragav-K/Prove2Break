# Prove2Break — Quiz System

## Quiz as Verification, Not Exam

The quiz's purpose is narrow: confirm the student can recall/apply what they claim to have just studied, quickly enough to gate a break decision without turning into a study session of its own. It is intentionally short (rapid-fire, timed, one topic-scoped batch), not a comprehensive assessment.

## Topic Selection → Generation Flow

1. Student taps "Break Now" during an active focus session.
2. Student selects which of their `is_completed` (self-marked) topics they want this break to cover.
3. Backend retrieves grounding `DocumentChunk`s for those topics (see `AI-RAG.md`).
4. `AIService.generateQuiz` produces a small batch of questions (default 5, configurable) spread across the selected topics.
5. Each question is validated (rules below) before being persisted and served.
6. If validation strips too many questions to reach the minimum viable count, generation retries once with adjusted grounding/prompt parameters before failing with a clear error.

## Question JSON Schema

```json
{
  "id": "uuid",
  "topic_id": "uuid",
  "prompt": "string",
  "options": ["string", "string", "string", "string"],
  "correct_option_index": 0,
  "source_chunk_ids": ["uuid", "uuid"],
  "time_limit_seconds": 15,
  "order_index": 0
}
```

Served to the Android client with `correct_option_index` and `source_chunk_ids` stripped (server-side only), plus `prompt`, `options`, `time_limit_seconds`, `order_index`.

## Validation Rules

1. **Exactly one correct answer.** `correct_option_index` must reference exactly one option; the generation prompt and a post-generation check both enforce this (never "select all that apply" phrasing, never an option list with ambiguous correctness).
2. **No duplicate options.** All strings in `options` must be distinct (case-insensitive, trimmed) — a duplicate indicates a generation defect and the question is discarded/regenerated.
3. **Valid option count.** Exactly 4 options (configurable constant, consistent across the app) — fewer makes guessing too easy, more slows down "rapid-fire."
4. **Topic relevance.** The question must be answerable using only the grounding chunks tied to its `topic_id` — enforced by prompting the model with only that topic's chunks as context, and spot-checked by confirming `source_chunk_ids` are non-empty and belong to the claimed topic.
5. **Source grounding.** `source_chunk_ids` must be non-empty and reference real `DocumentChunk` rows belonging to the requesting user's own material — a question with no traceable source is rejected before being shown to the student (this is the core anti-"generic trivia" guarantee).

## Rapid-Fire Presentation

See `UX-UI.md` for the full UI rules (one question at a time, visible countdown, no back/skip/pause, auto-advance). Functionally: each question is answered or times out, producing exactly one `QuizAnswer` per `Question`, in order.

## Scoring

- `final_score = correct_answers / total_questions`.
- Timed-out questions (`selected_option_index = null`) count as incorrect.
- Passed if `final_score >= pass_threshold` (default 0.70, from `User.quiz_pass_threshold`, overridable via Settings).
- On pass: linked `Break.status` transitions to unlocked/`ACTIVE`.
- On fail: `Break` stays locked; the session returns to `FOCUSING`; UI shows the non-judgmental copy from `PRODUCT.md`.
- Every attempt updates `TopicPerformance` for each covered topic (rolling average score, attempt count), feeding weak-topic detection in `ANALYTICS.md`.
