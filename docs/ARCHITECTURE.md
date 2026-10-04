# Prove2Break — High-Level Architecture

## System Diagram

```
                         ┌───────────────────────────────┐
                         │        Android App            │
                         │  (Kotlin, Jetpack Compose,     │
                         │   MVVM, Hilt, Room, DataStore)│
                         │                                │
                         │  ┌──────────┐  ┌─────────────┐ │
                         │  │Focus Svc │  │ Restriction │ │
                         │  │(foreground│  │  Engine     │ │
                         │  │ service) │  │(UsageStats/ │ │
                         │  └──────────┘  │ Accessibility)│
                         │                └─────────────┘ │
                         └───────────────┬────────────────┘
                                         │ REST + JWT (HTTPS)
                                         ▼
                         ┌───────────────────────────────┐
                         │        FastAPI Backend         │
                         │      (modular monolith)        │
                         │                                │
                         │  api/  auth/  services/         │
                         │  repositories/  models/         │
                         │  schemas/  document_processing/ │
                         │  ai/ (AIService abstraction)    │
                         └───────┬───────────────┬────────┘
                                 │               │
                       ┌─────────┘               └─────────┐
                       ▼                                   ▼
           ┌───────────────────────┐            ┌───────────────────────┐
           │ PostgreSQL + pgvector │            │   S3 / S3-compatible  │
           │ (relational + vector  │            │   object storage      │
           │  data, single DB)     │            │   (raw PDFs)          │
           └───────────────────────┘            └───────────────────────┘
                                 │
                                 ▼
                      ┌───────────────────────┐
                      │   AIService (behind    │
                      │   provider-agnostic    │
                      │   interface)           │
                      │  generateQuiz          │
                      │  analyzeDocument       │
                      │  generateTopics        │
                      │  generateStudyPlan     │
                      │  generateRecommendations│
                      └───────────┬────────────┘
                                  ▼
                       [ Pluggable LLM Provider ]
                       (OpenAI / Anthropic / etc,
                        swappable, no hard dep)
```

## Architectural Principles

1. **Modular monolith, not microservices.** One deployable FastAPI service with strict internal module boundaries (auth, subjects, materials, focus, breaks, quiz, analytics). This avoids premature network-boundary complexity while still keeping concerns cleanly separated for a future split if ever needed.
2. **`AIService` abstraction is a hard boundary.** No module outside `backend/ai/` imports a vendor SDK directly. All AI capability is exposed through five methods: `generateQuiz`, `analyzeDocument`, `generateTopics`, `generateStudyPlan`, `generateRecommendations`. This keeps the product provider-agnostic and testable (mock the interface in tests).
3. **Repository pattern on both sides.** Android's data layer and the backend's data layer both isolate persistence behind repository interfaces, so ViewModels/services depend on abstractions, not concrete Room/SQLAlchemy calls.
4. **Single source of truth for status: `PROCESS.md`.** Design docs describe intent; `PROCESS.md` describes what's actually built, in progress, or blocked at any given time.
5. **Android-only MVP, backend API-first.** The backend is built to be platform-agnostic (versioned REST API, JWT auth) so a future iOS client could consume it without backend changes.

## Android ↔ Backend Communication

- All communication is REST over HTTPS, JSON payloads, via Retrofit/OkHttp on the Android side.
- Every authenticated request carries a JWT access token in the `Authorization: Bearer` header.
- Access tokens are short-lived; the app transparently refreshes via the refresh-token endpoint on a 401, retrying the original request once.
- Long-running operations (PDF processing, quiz generation) are kicked off via a POST that returns immediately with a status resource; the Android app polls or is notified (MVP: polling with backoff) until the resource reaches a terminal status.

## Where State Lives

| State | Location | Why |
|---|---|---|
| Active focus session (authoritative) | Android Room (local) | Must survive process death and work offline; synced to backend when connectivity allows for analytics. |
| Blocked-app list | Android Room (local) + backend copy | Enforcement must work offline; backend copy allows cross-device settings sync later. |
| User auth tokens | Android EncryptedSharedPreferences/DataStore | Must never leave the device in plaintext. |
| Subjects, materials, topics | Backend PostgreSQL (source of truth), cached locally in Room for offline viewing | Backend is authoritative since materials require server-side processing anyway. |
| Document chunks & embeddings | Backend PostgreSQL + pgvector | Only the backend performs retrieval for quiz generation. |
| Quiz attempts & analytics | Backend PostgreSQL (source of truth) | Cross-session aggregation belongs server-side; Android caches for offline dashboard viewing. |
| User preferences (quiz threshold, etc.) | Android DataStore, synced to backend | Needs to be available offline immediately, but persisted centrally too. |
