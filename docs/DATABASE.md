# Prove2Break — Database Schema

PostgreSQL + pgvector extension. All tables include `id` (UUID PK), `created_at`, `updated_at` unless noted.

## Entities

### User
- `email` (unique, indexed), `password_hash`, `quiz_pass_threshold` (default 0.70), `created_at`.
- Relationships: has many Subjects, FocusSessions, EmergencyUnlocks.

### Subject
- `user_id` (FK -> User, indexed), `name`, `description`, `exam_id` (FK -> Exam, nullable).
- Relationships: has many StudyMaterials, Units, Topics, FocusSessions.
- Index: `(user_id)` for "list my subjects" query pattern.

### Exam
- `subject_id` (FK -> Subject), `exam_date`, `name` (nullable, e.g. "Midterm").
- One Subject may reference an Exam for its target date (kept as a separate entity to allow multiple exams per subject in future, though MVP UI exposes one).

### Unit
- `subject_id` (FK -> Subject, indexed), `name`, `order_index`.
- Groups Topics into a syllabus-like structure detected from material sections.

### Topic
- `unit_id` (FK -> Unit, nullable — a topic can belong directly to a Subject if no unit detected), `subject_id` (FK -> Subject, indexed), `name`, `is_completed` (bool), `completed_at` (nullable), `source_material_id` (FK -> StudyMaterial).
- Relationships: has many TopicPerformance rows (aggregated), referenced by Quiz.topics (many-to-many via `quiz_topics`).

### StudyMaterial
- `subject_id` (FK -> Subject, indexed), `filename`, `s3_key`, `page_count`, `status` (enum: UPLOADED, PROCESSING, READY, FAILED), `failure_reason` (nullable).
- Relationships: has many DocumentChunks.

### DocumentChunk
- `study_material_id` (FK -> StudyMaterial, indexed), `topic_id` (FK -> Topic, nullable), `content` (text), `chunk_index`, `embedding` (`vector(1536)` or provider-appropriate dimension via pgvector).
- **Index:** `ivfflat`/`hnsw` vector index on `embedding` (per pgvector) for similarity search — this is the core RAG retrieval index. Also index on `study_material_id`.

### FocusSession
- `user_id` (FK -> User, indexed), `subject_id` (FK -> Subject, indexed), `started_at`, `planned_duration_seconds`, `ended_at` (nullable), `status` (enum: FOCUSING, BREAK_AVAILABLE, BREAK_ACTIVE, BREAK_VERIFICATION, QUIZ_ACTIVE, COMPLETED, ENDED_EARLY).
- Relationships: has many Breaks, EmergencyUnlocks.
- Index: `(user_id, status)` to quickly find "my active session."

### Break
- `focus_session_id` (FK -> FocusSession, indexed), `type` (enum: SCHEDULED, UNSCHEDULED), `status` (enum: AVAILABLE, ACTIVE, DEFERRED, SKIPPED, COMPLETED), `scheduled_at` (nullable), `started_at` (nullable), `ended_at` (nullable), `extension_count`, `quiz_id` (FK -> Quiz, nullable — set for UNSCHEDULED breaks).

### BlockedApp
- `user_id` (FK -> User, indexed), `package_name`, `app_label`, `is_active` (bool).
- Unique constraint: `(user_id, package_name)`.

### Quiz
- `focus_session_id` (FK -> FocusSession, indexed), `break_id` (FK -> Break, nullable), `pass_threshold_used`, `score` (nullable until scored), `passed` (bool, nullable), `created_at`, `completed_at` (nullable).
- Many-to-many with Topic via `quiz_topics` join table (`quiz_id`, `topic_id`).
- Relationships: has many Questions.

### Question
- `quiz_id` (FK -> Quiz, indexed), `topic_id` (FK -> Topic), `prompt` (text), `options` (JSONB array of strings), `correct_option_index` (int), `source_chunk_ids` (JSONB array of UUIDs — grounding trace), `time_limit_seconds`, `order_index`.

### QuizAttempt
- `quiz_id` (FK -> Quiz, indexed, effectively 1:1 with Quiz per MVP but modeled separately for future multi-attempt support), `user_id` (FK -> User), `started_at`, `completed_at`, `final_score`, `passed`.

### QuizAnswer
- `quiz_attempt_id` (FK -> QuizAttempt, indexed), `question_id` (FK -> Question), `selected_option_index` (nullable — null if timed out), `is_correct` (bool), `answered_at`, `time_taken_seconds`.

### TopicPerformance
- `user_id` (FK -> User, indexed), `topic_id` (FK -> Topic, indexed), `attempt_count`, `average_score`, `last_attempt_at`.
- Unique constraint: `(user_id, topic_id)`. Recomputed/upserted after each QuizAttempt affecting that topic.

### StudyPlan
- `subject_id` (FK -> Subject, indexed), `generated_at`, `plan_data` (JSONB — ordered list of topic ids with suggested study dates/durations), `algorithm_version` (e.g. `"rule-based-v1"` — future-proofs for an ML planner version later).

### EmergencyUnlock
- `user_id` (FK -> User, indexed), `focus_session_id` (FK -> FocusSession), `triggered_at`, `reason` (optional free text, never required).

## Key Relationship Notes

- Deleting a `Subject` cascades to `StudyMaterial`, `Unit`, `Topic`, `DocumentChunk` (via `StudyMaterial`), and `FocusSession` history for that subject (soft-considerations: analytics aggregation may want to retain anonymized history — deferred to a future decision, not MVP).
- `Quiz` intentionally separates "the quiz definition" (`Quiz`/`Question`) from "the attempt" (`QuizAttempt`/`QuizAnswer`) even though MVP only allows one attempt per quiz, to avoid a schema migration if multi-attempt/retake is added later (`FUTURE-ROADMAP.md`).
- `TopicPerformance` is a denormalized aggregate table, updated transactionally alongside `QuizAttempt` writes, so the analytics dashboard and weak-topic detection never need to scan all historical `QuizAnswer` rows at read time.

## Indexes Summary

| Table | Index | Reason |
|---|---|---|
| DocumentChunk | vector index on `embedding` | RAG similarity search |
| DocumentChunk | `study_material_id` | fetch all chunks for a material |
| Subject | `user_id` | "my subjects" list |
| Unit, Topic | `subject_id` | subject detail page |
| FocusSession | `(user_id, status)` | find active session fast |
| Break | `focus_session_id` | session's break history |
| BlockedApp | unique `(user_id, package_name)` | prevent duplicate entries |
| Quiz | `focus_session_id` | session's quiz history |
| TopicPerformance | unique `(user_id, topic_id)` | upsert target, weak-topic query |
