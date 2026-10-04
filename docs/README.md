# Prove2Break Documentation

**Prove2Break** — *Study. Prove. Break. Repeat.*

An Android study-focus app where students upload their own material, the app extracts topics from it, restricts distracting apps during focus sessions, and lets students earn breaks — either on a recommended schedule, or early by passing a rapid-fire AI quiz grounded in their own uploaded material.

## How to use this documentation

1. **Start with `PROCESS.md`.** It is the single source of truth for what phase the project is in, what's done, what's in progress, what's blocked, and what decisions have been made. Every other doc describes the intended design; `PROCESS.md` describes actual current reality.
2. **Read before you build.** Before starting work on any feature, read the relevant doc(s) below plus `PROCESS.md`'s Current Task and Master Checklist sections.
3. **Update after you build.** After finishing any unit of work, update `PROCESS.md` (checklist, Completed Work table, Current Task, Decisions if applicable) and update any design doc whose content changed as a result of implementation discoveries.
4. Treat design docs (`ARCHITECTURE.md`, `DATABASE.md`, etc.) as the intended blueprint, and `PROCESS.md` as the live status board. If they conflict, `PROCESS.md` wins for "what's actually built"; the design doc still wins for "what it should look like when finished," unless a Decision in `PROCESS.md` explicitly supersedes it.

## Document Index

| Doc | Description |
|---|---|
| `PROCESS.md` | **Source of truth.** Phase, status, checklists, current task, completed work, blockers, bugs, decisions, next actions. |
| `PRODUCT.md` | Problem, users, vision, core loop, MVP boundaries, product principles (tone rules). |
| `REQUIREMENTS.md` | Functional (FR-xxx) and non-functional requirements. |
| `FEATURES.md` | Feature-by-feature inventory: purpose, user story, inputs/outputs, dependencies, edge cases, status. |
| `USER-FLOW.md` | ASCII flow diagrams for main loop, unscheduled break, auth, upload/processing, break state transitions. |
| `UX-UI.md` | Design language, full screen inventory, rapid-fire quiz UI rules. |
| `ARCHITECTURE.md` | High-level system architecture (Android + backend + DB + storage + AI). |
| `ANDROID-ARCHITECTURE.md` | MVVM/Repository pattern, package structure, Hilt, Room, DataStore, Navigation, WorkManager, foreground service. |
| `BACKEND-ARCHITECTURE.md` | FastAPI app structure, layering, migrations, config. |
| `DATABASE.md` | All entities, fields, relationships, indexes. |
| `API.md` | All REST endpoints grouped by resource. |
| `AUTHENTICATION.md` | Register/login/logout, JWT design, secure storage, password hashing. |
| `PDF-PROCESSING.md` | Upload → validation → extraction → chunking → embedding pipeline. |
| `AI-RAG.md` | RAG architecture, embedding/retrieval strategy, grounding rule, `AIService` abstraction. |
| `QUIZ-SYSTEM.md` | Quiz-as-verification design, generation flow, question schema, validation rules. |
| `FOCUS-ENGINE.md` | Timer design (timestamp-authoritative), durations, survival requirements, service design. |
| `BREAK-SYSTEM.md` | Break recommendation logic, scheduled/unscheduled/deferred breaks, state machine. |
| `APP-RESTRICTION.md` | Manual app selection, Android limitations disclosure, restriction mechanisms, permissions rationale. |
| `NOTIFICATION-SYSTEM.md` | Notifications needed, channels, foreground service notification requirement. |
| `ANALYTICS.md` | Metrics tracked, dashboard composition. |
| `STUDY-PLANNER.md` | Rule-based MVP planner design; explicitly not ML for MVP. |
| `SECURITY.md` | HTTPS, JWT, hashing, authZ, input validation, secrets management, rate limiting. |
| `PRIVACY.md` | Data minimization, storage privacy, AI data handling disclosure, deletion support. |
| `PERMISSIONS.md` | Every Android permission: why, when requested, denial behavior. |
| `OFFLINE-MODE.md` | What works offline vs needs network, user communication of limitations. |
| `ERROR-HANDLING.md` | Failure scenario table with user-facing and technical handling. |
| `TESTING.md` | Unit / integration / instrumentation test targets. |
| `DEPLOYMENT.md` | Backend containerization, Android build variants and signing process. |
| `ENVIRONMENT.md` | Required env vars, local dev setup for Android + backend. |
| `TROUBLESHOOTING.md` | Known issues table (empty for now), common setup problems. |
| `CHANGELOG.md` | Keep a Changelog style project history. |
| `FUTURE-ROADMAP.md` | Post-MVP scope: OCR, adaptive AI planner, iOS, etc. |

## Project Structure

```
Prove2Break/
├── docs/       — this documentation
├── android/    — Android app (Kotlin, Jetpack Compose) — not yet started
├── backend/    — FastAPI backend — not yet started
└── README.md   — top-level project readme
```
