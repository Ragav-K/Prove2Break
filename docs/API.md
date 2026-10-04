# Prove2Break — API Reference

Base path: `/api/v1`. All endpoints except auth register/login/refresh require `Authorization: Bearer <access_token>`.

## Auth

### POST /auth/register
- **Purpose:** Create a new user account.
- **Request:** email, password.
- **Response:** user id, email.
- **Auth:** none.
- **Errors:** 409 email already registered, 422 weak password.

### POST /auth/login
- **Purpose:** Authenticate and issue tokens.
- **Request:** email, password.
- **Response:** access_token, refresh_token, expires_in.
- **Auth:** none.
- **Errors:** 401 invalid credentials.

### POST /auth/refresh
- **Purpose:** Exchange a valid refresh token for a new access token.
- **Request:** refresh_token.
- **Response:** access_token, expires_in.
- **Auth:** none (refresh token itself is the credential).
- **Errors:** 401 expired/invalid refresh token.

### POST /auth/logout
- **Purpose:** Invalidate the refresh token server-side.
- **Request:** refresh_token.
- **Response:** 204.
- **Auth:** bearer.
- **Errors:** 401.

## Subjects

### GET /subjects
- **Purpose:** List the user's subjects with progress summary.
- **Response:** list of {id, name, exam_date, topics_total, topics_completed}.
- **Auth:** bearer.

### POST /subjects
- **Purpose:** Create a subject.
- **Request:** name, description (optional), exam_date (optional).
- **Response:** created Subject.
- **Auth:** bearer. **Errors:** 422 invalid input.

### GET /subjects/{id}
- **Purpose:** Subject detail with materials, units, topics.
- **Auth:** bearer. **Errors:** 404 not found/not owned.

### PATCH /subjects/{id}
- **Purpose:** Edit subject fields.
- **Request:** partial {name, description, exam_date}.
- **Auth:** bearer. **Errors:** 404, 422.

### DELETE /subjects/{id}
- **Purpose:** Delete subject and cascade its materials/topics/sessions.
- **Auth:** bearer. **Errors:** 404, 409 if an active focus session references it.

## Materials

### POST /subjects/{id}/materials
- **Purpose:** Upload a PDF for processing.
- **Request:** multipart file.
- **Response:** StudyMaterial {id, status=UPLOADED}.
- **Auth:** bearer. **Errors:** 413 too large, 415 unsupported type, 422 invalid.

### GET /materials/{id}
- **Purpose:** Poll processing status.
- **Response:** {id, status, failure_reason?}.
- **Auth:** bearer. **Errors:** 404.

### DELETE /materials/{id}
- **Purpose:** Remove material and its chunks/embeddings.
- **Auth:** bearer. **Errors:** 404.

## Topics

### GET /subjects/{id}/topics
- **Purpose:** List extracted topics for a subject.
- **Auth:** bearer.

### PATCH /topics/{id}
- **Purpose:** Rename/merge/split/edit a topic.
- **Request:** {name?, merge_into_topic_id?, is_completed?}.
- **Auth:** bearer. **Errors:** 404, 422.

### DELETE /topics/{id}
- **Purpose:** Remove an incorrect/duplicate topic.
- **Auth:** bearer. **Errors:** 404, 409 if referenced by an existing Quiz.

## Focus Sessions

### POST /focus-sessions
- **Purpose:** Start a new focus session.
- **Request:** subject_id, planned_duration_seconds.
- **Response:** FocusSession {id, started_at, planned_duration_seconds, status=FOCUSING}.
- **Auth:** bearer. **Errors:** 409 another session already active.

### GET /focus-sessions/{id}
- **Purpose:** Fetch current session state (used for reconciliation/recovery).
- **Auth:** bearer. **Errors:** 404.

### POST /focus-sessions/{id}/end
- **Purpose:** End a session early or mark completed.
- **Request:** {reason: EARLY | COMPLETED}.
- **Auth:** bearer. **Errors:** 404, 409 already ended.

## Breaks

### GET /focus-sessions/{id}/breaks/recommendation
- **Purpose:** Get the recommended break schedule for the session's planned duration.
- **Response:** list of {offset_seconds, suggested_duration_seconds}.
- **Auth:** bearer.

### POST /focus-sessions/{id}/breaks/scheduled/{break_id}/start
- **Purpose:** Start a scheduled break that's become available.
- **Auth:** bearer. **Errors:** 409 wrong state.

### POST /focus-sessions/{id}/breaks/unscheduled
- **Purpose:** Request an early break; triggers quiz generation.
- **Request:** {topic_ids: [...]}.
- **Response:** {break_id, quiz_id, status=BREAK_VERIFICATION}.
- **Auth:** bearer. **Errors:** 422 no topics selected, 409 wrong session state.

### POST /breaks/{id}/extend
- **Purpose:** Extend an active break.
- **Request:** {additional_seconds}.
- **Auth:** bearer. **Errors:** 409 extension cap reached, wrong state.

### POST /breaks/{id}/end
- **Purpose:** End the active break, resume focus.
- **Auth:** bearer. **Errors:** 409 wrong state.

## Quiz

> **Implementation deviation (Phase 6):** the endpoints below (`/api/quiz/*`) are
> what actually shipped, in place of the `/quiz/{id}` + `/breaks/{id}` shapes
> originally sketched above. No `Break`/`FocusSession` backend model exists yet
> (Android-only concepts so far — see `Quiz.subject_id`/`user_id` deviation note
> in `app/models/quiz.py`), so quiz generation is triggered directly with a
> subject + topic ids rather than via a break-request endpoint, and there's an
> explicit `/attempt` step (supporting retakes) instead of folding attempt
> creation into the GET. `break_status` is omitted from the submit response
> for the same reason. These will likely be reconciled once `Break` lands.

### POST /api/quiz/generate
- **Purpose:** Generate a source-grounded quiz over the given topics (RAG retrieval + `AIService.generate_quiz`, see `AI-RAG.md`).
- **Request:** {subject_id, topic_ids: [...], num_questions? (default 5), time_limit_seconds? (default 15)}.
- **Response:** Quiz + questions with `correct_option_index`/`source_chunk_ids` stripped.
- **Auth:** bearer. **Errors:** 404 subject/topic not found or not owned, 422 not enough grounding material to generate a fair quiz.

### POST /api/quiz/{id}/attempt
- **Purpose:** Start a new attempt at an existing quiz (also usable for retakes).
- **Response:** {attempt_id, quiz_id, questions: [...] (answer-key stripped)}.
- **Auth:** bearer. **Errors:** 404 quiz not found or not owned.

### POST /api/quiz/{id}/submit
- **Purpose:** Submit all answers for the most recent open attempt, compute score, decide pass/fail.
- **Request:** {answers: [{question_id, selected_option_index (nullable if timed out), time_taken_seconds}]}.
- **Response:** {attempt_id, score, passed, correct_count, total_questions}.
- **Auth:** bearer. **Errors:** 404 quiz not found, 409 no open attempt (call `/attempt` first).

### GET /api/quiz/{id}/result
- **Purpose:** Fetch the latest completed attempt's score/pass-fail outcome (plain data — no judgmental copy; that's the Android client's job per `PRODUCT.md`).
- **Response:** {attempt_id, quiz_id, score, passed, correct_count, total_questions, completed_at}.
- **Auth:** bearer. **Errors:** 404 quiz not found, or no completed attempt yet.

## App Restriction

### GET /blocked-apps
- **Purpose:** List the user's configured restricted apps.
- **Auth:** bearer.

### PUT /blocked-apps
- **Purpose:** Replace the full restricted-app list.
- **Request:** [{package_name, app_label}].
- **Auth:** bearer.

### POST /emergency-unlocks
- **Purpose:** Record an emergency exit from restriction.
- **Request:** {focus_session_id, reason?}.
- **Auth:** bearer.

## Analytics

### GET /analytics/dashboard
- **Purpose:** Aggregate study time, session count, quiz pass rate, weak topics.
- **Auth:** bearer.

### GET /analytics/topics/weak
- **Purpose:** List topics with average score below threshold.
- **Auth:** bearer.

### GET /subjects/{id}/study-plan
- **Purpose:** Fetch (generating if stale) the rule-based study plan.
- **Auth:** bearer. **Errors:** 404, 422 if no exam date set.
