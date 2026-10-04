# Prove2Break — Android Architecture

## Pattern: MVVM + Repository

- **View (Compose UI)** — stateless composables observing `StateFlow`/`State` from a ViewModel. No business logic in composables.
- **ViewModel** — holds UI state, calls use cases, exposes `StateFlow` to the View. One ViewModel per screen (or closely related screen group).
- **UseCase (domain layer)** — single-responsibility operations (e.g. `StartFocusSessionUseCase`, `SubmitQuizAnswerUseCase`) that orchestrate one or more repositories. Optional for simple CRUD screens, required for anything with business rules (timer math, break state transitions, scoring).
- **Repository** — single source of truth per data type, abstracting Room (local) and Retrofit (remote) behind an interface consumed by ViewModels/UseCases. Repositories decide caching/offline strategy; callers never touch DAOs or API services directly.

## Package Structure

```
android/app/src/main/java/com/prove2break/
├── data/
│   ├── local/           # Room entities, DAOs, DataStore definitions
│   ├── remote/          # Retrofit API interfaces, DTOs, interceptors (auth, refresh)
│   └── repository/      # Repository implementations (data/local + data/remote -> domain models)
├── domain/
│   ├── model/           # Plain domain models (Subject, Topic, FocusSession, Break, Quiz, ...)
│   └── usecase/         # Business-logic use cases
├── ui/
│   ├── auth/            # Login, Register
│   ├── subject/         # Subject list/detail/create
│   ├── material/        # Upload, processing status, topic review
│   ├── focus/            # Focus session setup + active session screens
│   ├── apprestriction/  # App selection, permission explainer, blocked-app screen
│   ├── breaks/           # Scheduled/unscheduled break screens
│   ├── quiz/             # Rapid-fire quiz UI, result screen
│   ├── analytics/        # Dashboard, weak topics
│   ├── planner/          # Study planner screen
│   ├── settings/         # Settings
│   └── common/           # Shared composables, theme
├── focus/                # Focus timer engine + foreground service (cross-cutting, not just UI)
├── apprestriction/       # Restriction engine implementation (UsageStatsManager/Accessibility integration), cross-cutting
├── notifications/        # Notification builders, channels
└── di/                   # Hilt modules
```

`ui/*` packages hold Compose screens + ViewModels for that feature; `focus/` and `apprestriction/` at the top level hold the cross-cutting engine code that the foreground service and restriction detection run, independent of any single screen's lifecycle.

## Dependency Injection — Hilt

- `@HiltAndroidApp` on the `Application` class.
- `di/DatabaseModule` — provides the Room database and DAOs.
- `di/NetworkModule` — provides Retrofit, OkHttp (with auth interceptor + refresh-token authenticator), API service interfaces.
- `di/RepositoryModule` — binds repository interfaces to implementations.
- `di/AiModule` (backend-adjacent naming avoided here — this is just the network client for AI-backed endpoints, not a direct AI SDK) — provides any AI-related API client wrappers.
- ViewModels use `@HiltViewModel` + constructor injection of use cases/repositories.
- The foreground service uses `@AndroidEntryPoint` for field injection of the focus timer engine and repositories.

## Room Usage

- Entities mirror the subset of backend entities needed offline: `FocusSessionEntity`, `BlockedAppEntity`, cached `SubjectEntity`/`TopicEntity` for offline viewing, cached `QuizAttemptEntity` for offline dashboard.
- `FocusSessionEntity` is the authoritative local record for an in-progress session — written on session start, read back by the foreground service and ViewModel on process restart, updated on every state transition (break events, completion).
- Migrations managed via Room's `AutoMigration` where possible; manual `Migration` objects for anything AutoMigration can't express.

## DataStore Usage

- `Preferences DataStore` for lightweight user settings: quiz pass threshold override, notification preferences, theme choice, onboarding-seen flags.
- A separate encrypted DataStore (or EncryptedSharedPreferences, decided at implementation time based on current best-practice library support) for JWT access/refresh tokens — never the plain Preferences DataStore for secrets.

## Navigation Compose

- Single `NavHost` with a sealed `Screen` route class per screen in the inventory (`UX-UI.md`).
- Top-level graphs: `AuthGraph`, `MainGraph` (subject/material/analytics/settings), `FocusGraph` (session setup → active session → breaks → quiz → summary, largely full-screen and back-stack-restricted during an active session to discourage accidental exit).
- The active-focus and quiz screens disable the system back gesture/button from navigating away silently — back either confirms an intentional exit (with consequence messaging) or is a no-op, per `UX-UI.md` quiz rules.

## WorkManager Usage

- A periodic/one-off `CoroutineWorker` acts as a watchdog: if the foreground service is unexpectedly not running while a `FocusSessionEntity` shows an active session, WorkManager restarts it.
- `CoroutineWorker` also handles deferred sync of locally-recorded session/quiz data to the backend when connectivity returns (offline-first sync).

## Foreground Service Design (Focus Sessions)

- `FocusSessionService` (foreground service, type `dataSync`/`specialUse` as appropriate per target SDK) started when a session begins.
- Holds a reference to the timer engine (timestamp-based, see `FOCUS-ENGINE.md`), posts/updates the ongoing notification (see `NOTIFICATION-SYSTEM.md`), and listens for state transition intents (break requested, session ended).
- On service creation, it first checks Room for an existing active `FocusSessionEntity` (covers process-death recovery) before assuming a fresh start.
- Stops itself only on session completion or explicit user-ended session; the WorkManager watchdog guards against being killed prematurely by the OS.
