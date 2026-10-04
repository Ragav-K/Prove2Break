# Prove2Break — Security

## Transport & Auth

- **HTTPS everywhere** — all client-backend traffic over TLS; no plaintext HTTP endpoints, even in dev (local dev uses a local TLS cert or an HTTPS-terminating proxy where practical; if HTTP is used purely for localhost dev convenience, it is never used for anything resembling production data).
- **JWT** — access + refresh token scheme per `AUTHENTICATION.md`, HMAC-signed with a secret from `JWT_SECRET_KEY` (never hardcoded, never committed).
- **Password hashing** — bcrypt/argon2id, per-password salt, no custom crypto.

## Authorization

- Every resource (Subject, Material, Topic, FocusSession, Quiz, etc.) is scoped by `user_id` at the repository query level — there is no code path that fetches a resource by id alone without also filtering by the authenticated user, preventing IDOR-style cross-user access.
- Role/permission model is single-tier for MVP (every authenticated user has the same capabilities over their own data) — no admin/staff roles in MVP scope.

## Input Validation

- All request bodies validated via Pydantic schemas server-side (never trust client-side validation alone).
- File uploads validated for type/size/page-count before any processing begins (`PDF-PROCESSING.md`).
- Quiz answer submissions validated against the actual question set for that quiz (can't submit an answer for a question that doesn't belong to the quiz/user).

## Secure Token Storage (Client)

- Android stores JWTs via `EncryptedSharedPreferences`/encrypted DataStore, never plain storage, never logged (see `AUTHENTICATION.md`).

## Private Document Storage

- Uploaded PDFs live in S3-compatible storage under a per-user/per-subject key prefix, accessed only via backend-issued short-lived signed URLs or backend-proxied reads — never a publicly readable bucket/object.
- `DocumentChunk.content` (extracted text) is stored in PostgreSQL, accessible only through authenticated, user-scoped queries.

## No API Keys in Android Source

- The Android app never embeds AI provider keys, S3 credentials, or the JWT signing secret. All AI/storage operations happen server-side; the Android client only ever calls the Prove2Break backend with its own JWT.

## Environment Variables & Secrets Management

- All secrets (DB credentials, JWT secret, S3 keys, AI provider key) are supplied via environment variables, never committed to source.
- `backend/.env.example` documents variable **names** only, with placeholder values — real `.env` files are git-ignored.
- Production secrets are managed via the deployment platform's secret store (see `DEPLOYMENT.md`), not baked into container images.

## Error Handling Without Leaking Internals

- API error responses return a stable, generic error shape (`{detail: "..."}` with an appropriate HTTP status) — stack traces, SQL errors, and internal file paths are never returned to the client; they're logged server-side only.
- 500-level errors are logged with a correlation id that can be referenced in support conversations without exposing internals to the end user.

## Rate Limiting

- Auth endpoints (`/auth/login`, `/auth/register`, `/auth/refresh`) are rate-limited per IP/email to slow brute-force and credential-stuffing attempts.
- Quiz-generation and upload endpoints are rate-limited per user to bound AI/storage cost exposure from misuse or bugs.

## Logging Hygiene

- Logs never contain: raw passwords, full JWTs, full extracted document text, or full quiz question/answer content.
- Logs may contain: user id (not email, where avoidable), request path, status code, latency, and category-level error reasons — enough to debug without being a privacy liability (see `PRIVACY.md`).
