# Prove2Break — User Flows

## Auth Flow

```
[Launch App]
     │
     ▼
[Has valid session token?] --yes--> [Home / Dashboard]
     │no
     ▼
[Login Screen] <---> [Register Screen]
     │
     ▼
[Submit credentials] --> [Backend validates] --fail--> [Show error, stay on screen]
     │success
     ▼
[Store JWT access+refresh securely] --> [Home / Dashboard]
```

## PDF Upload & Processing Flow

```
[Subject Screen] --> [Tap "Add Material"] --> [Pick PDF via file picker]
     │
     ▼
[Client-side validation: type, size] --fail--> [Show error]
     │pass
     ▼
[Upload to backend/S3] --> [StudyMaterial status = UPLOADED]
     │
     ▼
[Backend pipeline]
   Validation --> Text Extraction --> Cleaning --> Section Detection
        --> Topic Detection --> Chunking --> Embedding --> pgvector store
     │
     ├──fail at any stage──> [status = FAILED, reason shown, retry option]
     │
     ▼success
[status = READY] --> [Notify user] --> [Topic Review Screen]
                                             │
                                             ▼
                                 [User confirms/edits topics]
```

## Main Loop Flow

```
[Home] --> [Start Focus Session]
     │
     ▼
[Choose Subject] --> [Choose Duration (1h/2h/3h/4h/custom)]
     │
     ▼
[Confirm/adjust restricted app list]
     │
     ▼
[Session STARTS: state=FOCUSING]
   - FocusSession(startedAt, plannedDuration) written to Room
   - Foreground service + notification started
   - Restricted apps blocked
     │
     ▼
[Studying...] ------------------------------------------------┐
     │                                                         │
     │ (scheduled break time reached)                          │ (user taps "Break Now")
     ▼                                                         ▼
[state=BREAK_AVAILABLE]                              [Select claimed-complete topics]
     │                                                         │
   ┌─┴─────────────┐                                           ▼
   ▼               ▼                                 [state=BREAK_VERIFICATION]
[Take break]   [Defer / Continue]                              │
   │               │                                           ▼
   ▼               └──> back to [Studying...]         [Generate RAG quiz on those topics]
[state=BREAK_ACTIVE]                                            │
   │                                                            ▼
[Break ends / user resumes] --> back to [Studying...]  [state=QUIZ_ACTIVE]
                                                                  │
                                                    ┌─────────────┴─────────────┐
                                                    ▼                           ▼
                                              [PASS (>=70%)]               [FAIL (<70%)]
                                                    │                           │
                                                    ▼                           ▼
                                        [state=BREAK_ACTIVE]        [Break stays locked;
                                        (break unlocked)             gentle message shown;
                                                                     back to Studying...]
     │
     ▼ (plannedDuration elapsed)
[state=SESSION_COMPLETED]
   - Restrictions lifted
   - Study time, quiz history, topic performance recorded
   - Analytics dashboard updated
```

## Break State Transitions (summary — full detail in BREAK-SYSTEM.md)

```
IDLE -> FOCUSING -> BREAK_AVAILABLE -> BREAK_ACTIVE -> FOCUSING -> ... -> SESSION_COMPLETED

Unscheduled path:
FOCUSING -> BREAK_VERIFICATION -> QUIZ_ACTIVE -> (PASS -> BREAK_ACTIVE) | (FAIL -> FOCUSING)
```
