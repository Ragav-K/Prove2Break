# Prove2Break — Backend Architecture

## Stack

Python, FastAPI, Pydantic (schemas/validation), SQLAlchemy (ORM), PostgreSQL + pgvector, JWT auth, Alembic (migrations).

## App Structure

```
backend/
├── api/                     # Route definitions, grouped by resource (thin — delegate to services)
│   ├── auth.py
│   ├── subjects.py
│   ├── materials.py
│   ├── topics.py
│   ├── focus_sessions.py
│   ├── breaks.py
│   ├── quiz.py
│   └── analytics.py
├── auth/                     # JWT creation/validation, password hashing, current-user dependency
├── models/                   # SQLAlchemy ORM models (one file per entity or logical group)
├── schemas/                  # Pydantic request/response schemas (never expose ORM models directly)
├── repositories/              # Data-access layer — one repository per aggregate, wraps SQLAlchemy queries
├── services/                  # Business logic layer — orchestrates repositories + ai/ + document_processing/
├── ai/                        # AIService interface + provider implementations (OpenAI, Anthropic, etc.)
├── document_processing/       # PDF validation, extraction, cleaning, section/topic detection, chunking, embedding
├── core/                       # config, security utils, exceptions, logging setup (implied support module)
└── main.py                    # FastAPI app instantiation, router registration, middleware, startup/shutdown
```

## Layering Rule

**Route → Service → Repository → Model.**

- `api/*.py` routes only: parse/validate request via Pydantic schema, call the relevant service method, map the result to a response schema. No business logic, no direct DB queries.
- `services/*.py` hold business rules: e.g. `QuizService.evaluate_attempt()` computes score, decides pass/fail, decides break-unlock, and records the attempt via `repositories/quiz_repository.py`. Services call `ai/` for AI-backed operations and `document_processing/` for the upload pipeline.
- `repositories/*.py` are the only modules that construct SQLAlchemy queries. They accept/return domain-ish dataclasses or ORM models internal to the backend — never Pydantic API schemas (that mapping happens in the service or route layer).
- `models/*.py` are pure SQLAlchemy declarative models — no business logic, only relationships/constraints/indexes.

This layering keeps route handlers thin and testable, and keeps AI/storage vendor specifics out of business logic (business logic depends on the `AIService` interface, not a concrete provider).

## Migrations

- **Alembic** manages schema migrations against PostgreSQL.
- One migration per meaningful schema change, generated via `alembic revision --autogenerate` and hand-reviewed (autogenerate misses some pgvector-specific index syntax, e.g. `ivfflat`/`hnsw` indexes on `DocumentChunk.embedding`, which are added manually in the migration).
- Migrations run automatically on deploy in a pre-start step (never implicitly on app boot in production) so schema state is explicit and reviewable.

## Config / Env Handling

- All configuration loaded via a `core/config.py` `Settings` object (Pydantic `BaseSettings`), reading from environment variables — see `ENVIRONMENT.md` for the full variable list.
- No secrets committed to source; local dev uses `backend/.env` (git-ignored) seeded from `backend/.env.example`.
- Settings are injected via FastAPI dependency injection (`Depends(get_settings)`) rather than imported as a global where practical, to keep services testable with overridden config.
- CORS origins, JWT secret/algorithm/expiry, DB URL, S3 credentials, and AI provider config are all environment-driven — nothing environment-specific is hardcoded.

## Request Lifecycle Example (Unscheduled Break Verification)

1. `POST /focus-sessions/{id}/breaks/unscheduled` hits `api/breaks.py`.
2. Route validates payload (`schemas/breaks.py`: claimed topic ids) and calls `services/break_service.py::request_unscheduled_break`.
3. `BreakService` calls `services/quiz_service.py::generate_quiz_for_topics`, which retrieves relevant `DocumentChunk`s via `repositories/document_chunk_repository.py` (pgvector similarity search) and calls `ai/service.py::AIService.generateQuiz`.
4. Generated questions are validated (schema rules in `QUIZ-SYSTEM.md`) and persisted via `repositories/quiz_repository.py`.
5. Response schema returns the quiz (without correct-answer flags) to the Android client.
6. Subsequent `POST /quiz/{id}/answers` calls flow through the same layering to score and, on pass, flip the `Break` state via `BreakService`.
