# Prove2Break — Privacy

## Study Material Sensitivity

Uploaded study material can be personal or sensitive — a student's own annotated notes, sometimes covering health/legal/personal-development topics, not just generic textbook content. It's treated as private data belonging solely to the uploading user, never repurposed, aggregated across users, or exposed to anyone else.

## Data Minimization

- Only data required for the product's stated features is collected: email (auth), study material (for topic extraction/quizzing), session/quiz activity (for the focus loop and analytics).
- No unnecessary tracking: no ad-tech SDKs, no behavioral tracking beyond what directly powers the study-analytics features described in `ANALYTICS.md`, no location data.

## No Full-PDF-Content Logging

- Application logs never contain full extracted document text or full quiz prompts/answers. Logs reference records by id (`study_material_id`, `question_id`) so engineers can look up context in the database (itself access-controlled) without sensitive content passing through general-purpose log aggregation.

## Private Storage

- Raw PDFs in S3-compatible storage are private-by-default (no public bucket ACLs), accessed only via backend-authenticated flows.
- Extracted text/chunks in PostgreSQL are reachable only through user-scoped, authenticated queries (see `SECURITY.md` Authorization).

## AI Provider Data Handling Disclosure

- When study material or quiz-generation prompts are sent to the configured AI provider (`AIService`), that provider necessarily processes the student's own content to generate topics/questions. This is disclosed to the user (e.g. in a Privacy/Terms surface within Settings) in plain language: "To generate topics and quiz questions from your uploaded material, relevant excerpts are sent to our AI provider for processing."
- The specific provider in use is an operational/config detail (`AI_PROVIDER` env var) and can change; the disclosure is written to remain accurate regardless of which specific provider is configured (i.e., "our AI provider," not naming a vendor in user-facing copy, to avoid the copy going stale).

## Deletion Support

- A user can delete a Subject (cascading its materials, chunks, topics, and related session/quiz history) or their entire account.
- Account deletion removes/anonymizes personally identifying data and associated study material from primary storage within a defined operational window; backups age out per standard backup retention rather than being immediately purged (documented plainly to the user rather than over-promising instant erasure from all systems).

## No Cross-User Exposure

- Every data access path is scoped by authenticated `user_id` (see `SECURITY.md`); there is no admin-facing feature in MVP that surfaces one user's study content to another user or to staff without a clear support-access justification (out of MVP scope to build tooling for staff access at all).

## No Unnecessary Data Collection

- No collection of device contacts, SMS, call logs, or other unrelated personal data — permissions requested are exactly the set justified in `PERMISSIONS.md`, nothing broader "just in case."
