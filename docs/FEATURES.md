# Prove2Break — Feature Inventory

Status columns use the legend from `PROCESS.md`. All features are currently ⬜ NOT STARTED (Phase 0 — docs only).

---

### 1. Authentication (Register/Login/Logout)
- **Purpose:** Identify the student and protect their data.
- **User story:** As a student, I want to create an account and log in so my study data is mine and persists across devices.
- **Inputs:** email, password (register/login); refresh token (session restore).
- **Outputs:** JWT access + refresh token; authenticated session.
- **Dependencies:** Backend auth service, Android secure token storage.
- **Edge cases:** duplicate email, wrong password, expired refresh token, token theft/rotation.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 2. Subject Management
- **Purpose:** Organize study material and progress per subject/exam.
- **User story:** As a student, I want to create a subject so I can track a specific exam's material separately from others.
- **Inputs:** name, description, exam date (optional).
- **Outputs:** Subject record with progress summary.
- **Dependencies:** Auth.
- **Edge cases:** deleting a subject with active focus session, subject with no material yet.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 3. PDF Upload
- **Purpose:** Get the student's own material into the system.
- **User story:** As a student, I want to upload my notes/textbook PDF so the app can quiz me on it.
- **Inputs:** PDF file, target subject.
- **Outputs:** StudyMaterial record, upload status.
- **Dependencies:** S3-compatible storage, subject management.
- **Edge cases:** oversized file, non-PDF file, scanned/image-only PDF, corrupt file.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 4. PDF Processing Pipeline
- **Purpose:** Turn a raw PDF into structured, embeddable, RAG-ready content.
- **User story:** As a student, I want my upload processed automatically so I don't have to manually structure my notes.
- **Inputs:** raw PDF bytes.
- **Outputs:** cleaned text, detected sections, chunks, embeddings stored in `DocumentChunk`.
- **Dependencies:** document_processing service, AIService (embeddings).
- **Edge cases:** unparseable PDF, extremely short/long document, mixed-language content.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 5. Topic Extraction
- **Purpose:** Give the student (and the quiz system) a topic-level breakdown of their material.
- **User story:** As a student, I want the app to suggest topics from my material so I don't have to define my own syllabus structure.
- **Inputs:** processed document chunks.
- **Outputs:** candidate Topic records (name, unit grouping, source chunk refs).
- **Dependencies:** AIService.generateTopics, PDF processing.
- **Edge cases:** too few/too many topics extracted, overlapping topics, poorly structured source material.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 6. Topic Review & Completion Tracking
- **Purpose:** Let the student correct AI topic extraction and mark progress.
- **User story:** As a student, I want to edit topic names and mark ones I've finished studying.
- **Inputs:** topic edits (rename/merge/split/delete), completion toggle.
- **Outputs:** updated Topic records, completion state.
- **Dependencies:** Topic extraction.
- **Edge cases:** marking complete with zero study time logged, un-completing a topic already used in a passed quiz.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 7. Focus Timer
- **Purpose:** Core session clock driving the whole loop.
- **User story:** As a student, I want to start a focus timer for a chosen duration and trust it keeps running accurately even if I lock my phone.
- **Inputs:** duration selection, subject.
- **Outputs:** FocusSession record (startedAt, plannedDuration, status), live elapsed/remaining display.
- **Dependencies:** Room, foreground service.
- **Edge cases:** process death mid-session, device reboot, clock change, duration edited mid-session (not allowed).
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 8. Foreground Service & Notification
- **Purpose:** Keep the session alive in the background and visibly communicate it's active.
- **User story:** As a student, I want to see a persistent notification showing my focus session is running.
- **Inputs:** active FocusSession.
- **Outputs:** ongoing notification with elapsed/remaining time and quick actions.
- **Dependencies:** Focus timer, Notification system.
- **Edge cases:** service killed by OS, notification permission denied.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 9. App Restriction Setup
- **Purpose:** Let the student choose their own distractions to block.
- **User story:** As a student, I want to pick which apps get blocked during focus so I'm not tempted by them.
- **Inputs:** list of installed apps, user selection.
- **Outputs:** BlockedApp list tied to user (and optionally per-subject/session).
- **Dependencies:** Permission flow (usage stats / accessibility as applicable).
- **Edge cases:** system apps that can't be blocked, uninstalled app still in list.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 10. Restriction Engine (Blocking Enforcement)
- **Purpose:** Actually intercept and block restricted apps during focus.
- **User story:** As a student, I want restricted apps to actually be inaccessible while I'm focusing.
- **Inputs:** active FocusSession + BlockedApp list.
- **Outputs:** blocked-app screen shown instead of/over the restricted app.
- **Dependencies:** UsageStatsManager/Accessibility Service, foreground service.
- **Edge cases:** OEM battery optimization killing detection, split-screen/multi-window bypass attempts.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 11. Blocked-App Screen
- **Purpose:** Redirect attention back to the study session when a restricted app is opened.
- **User story:** As a student, when I try to open a blocked app, I want to see a reminder and a way back to studying, not silent failure.
- **Inputs:** attempted app open event.
- **Outputs:** full-screen interstitial with session context and "Back to studying" / "Request a break" actions.
- **Dependencies:** Restriction engine.
- **Edge cases:** rapid repeated attempts, notification-based access to blocked app.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 12. Emergency Access
- **Purpose:** Safety valve for genuine emergencies without gaming the system for casual use.
- **User story:** As a student, I want a way to fully exit restriction in a real emergency without needing to pass a quiz.
- **Inputs:** emergency-unlock request.
- **Outputs:** EmergencyUnlock record, restriction lifted, session marked accordingly.
- **Dependencies:** Restriction engine.
- **Edge cases:** repeated/abused emergency unlocks (tracked but not blocked in MVP — visibility only).
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 13. Break Recommendation Engine
- **Purpose:** Suggest sensible break timing based on session length.
- **User story:** As a student, I want the app to suggest when to take breaks so I don't have to plan that myself.
- **Inputs:** plannedDuration.
- **Outputs:** suggested break offsets/durations (see `BREAK-SYSTEM.md` table).
- **Dependencies:** Focus timer.
- **Edge cases:** very short custom durations with no natural break point.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 14. Scheduled Break
- **Purpose:** Give the student a guilt-free break at a planned time.
- **User story:** As a student, when a scheduled break arrives, I want a clear prompt to take it, defer it, or skip it.
- **Inputs:** break trigger time reached.
- **Outputs:** Break record (status=AVAILABLE → ACTIVE/DEFERRED/SKIPPED).
- **Dependencies:** Break recommendation engine, notification system.
- **Edge cases:** app backgrounded when trigger fires, multiple scheduled breaks queued.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 15. Break Extension
- **Purpose:** Allow a bit more breathing room without abandoning structure.
- **User story:** As a student, I want to extend my break by a few minutes if I need to.
- **Inputs:** extension request during ACTIVE break.
- **Outputs:** updated Break duration, extension count.
- **Dependencies:** Scheduled/unscheduled break.
- **Edge cases:** extension cap reached, extension requested after break already ended.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 16. Unscheduled Break Request
- **Purpose:** Let a student ask for an early break, gated by proof of study.
- **User story:** As a student, I want to request a break early if I've genuinely finished a topic, by proving it.
- **Inputs:** "Break Now" tap, claimed-complete topic selection.
- **Outputs:** BREAK_VERIFICATION state entered, quiz requested.
- **Dependencies:** Topic completion, Quiz generation.
- **Edge cases:** no topics marked complete yet, all topics already fully verified.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 17. RAG-Grounded Quiz Generation
- **Purpose:** Produce fair, material-grounded verification questions.
- **User story:** As a student, I want quiz questions based on what I actually studied, not generic trivia.
- **Inputs:** selected topics, related DocumentChunks.
- **Outputs:** Quiz + Question records with validated schema.
- **Dependencies:** AIService.generateQuiz, pgvector retrieval.
- **Edge cases:** topic with too little source material, ambiguous/duplicate-option questions filtered out.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 18. Rapid-Fire Quiz UI
- **Purpose:** Fast, focused verification experience.
- **User story:** As a student, I want to answer quiz questions quickly, one at a time, without being able to stall or go back.
- **Inputs:** Question stream.
- **Outputs:** QuizAnswer per question, auto-advance.
- **Dependencies:** Quiz generation.
- **Edge cases:** timeout with no answer selected, app backgrounded mid-quiz.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 19. Quiz Scoring & Pass/Fail
- **Purpose:** Turn answers into a break decision.
- **User story:** As a student, I want to know immediately whether I've unlocked my break.
- **Inputs:** QuizAnswers.
- **Outputs:** score, pass/fail, break unlock or lock-with-message.
- **Dependencies:** Rapid-fire quiz UI.
- **Edge cases:** exact-threshold score (rounding rule), partial submission due to app close.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 20. Analytics Dashboard
- **Purpose:** Show the student their study patterns and weak spots.
- **User story:** As a student, I want to see my study time, quiz history, and weak topics in one place.
- **Inputs:** aggregated FocusSession/Quiz/TopicPerformance data.
- **Outputs:** dashboard charts/cards.
- **Dependencies:** all tracking features above.
- **Edge cases:** no data yet (empty state), single outlier session skewing averages.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 21. Weak Topic Detection
- **Purpose:** Proactively point the student at what needs more work, kindly.
- **User story:** As a student, I want to know which topics I'm weak on without feeling judged.
- **Inputs:** TopicPerformance (avg score < 70%).
- **Outputs:** "Needs another look" list with gentle copy.
- **Dependencies:** Quiz scoring history.
- **Edge cases:** topic with only one attempt (low sample size framing).
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 22. Study Planner (rule-based)
- **Purpose:** Suggest what to study next.
- **User story:** As a student, I want a suggested order to study my remaining topics before my exam.
- **Inputs:** exam date, remaining topics, difficulty, weak topics, available time.
- **Outputs:** ordered StudyPlan.
- **Dependencies:** Subject/topic/exam data, analytics.
- **Edge cases:** no exam date set, exam date in the past, more topics than time allows.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 23. Notifications
- **Purpose:** Keep the student informed without needing the app in foreground.
- **User story:** As a student, I want to be notified when a break is available or a session completes.
- **Inputs:** session/break state transitions.
- **Outputs:** system notifications across defined channels.
- **Dependencies:** Foreground service.
- **Edge cases:** notification permission denied (Android 13+), Do Not Disturb.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 24. Offline Mode Handling
- **Purpose:** Keep the core loop usable without connectivity.
- **User story:** As a student, I want my focus timer and restrictions to keep working even without internet.
- **Inputs:** connectivity state.
- **Outputs:** graceful degradation messaging for network-dependent features.
- **Dependencies:** Focus timer, restriction engine, local Room cache.
- **Edge cases:** quiz requested while offline, upload attempted while offline.
- **Implementation status:** ⬜ | **Testing status:** ⬜

### 25. Settings
- **Purpose:** Let the student configure personal preferences.
- **User story:** As a student, I want to adjust my quiz pass threshold and manage my blocked-app list from settings.
- **Inputs:** preference changes.
- **Outputs:** updated user preferences (DataStore + backend sync).
- **Dependencies:** Auth.
- **Edge cases:** invalid threshold value, settings changed mid-session.
- **Implementation status:** ⬜ | **Testing status:** ⬜
