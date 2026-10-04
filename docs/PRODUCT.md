# Prove2Break — Product

**Tagline:** Study. Prove. Break. Repeat.

## Problem

Students studying for exams sit down with good intentions and lose the session to their phone. Existing "focus" or "app blocker" apps either (a) block apps on a rigid timer with no regard for whether the student is actually studying their own material, or (b) let the student turn off the block whenever willpower runs out, defeating the purpose. Neither approach connects the *break* to *actual demonstrated understanding* of what the student claims to have studied.

## Target Users

- Students (secondary, university, competitive-exam) preparing for a specific exam with a fixed date and defined syllabus/material.
- Self-directed learners who upload their own notes/textbook PDFs and want structure without a teacher enforcing it.
- People who already know they're prone to phone distraction during study and want a system with actual friction, not just a suggestion.

## Product Vision

An app that turns "I'll just check Instagram for a second" into a real decision with a real cost: either wait for your scheduled break, or prove — with a quiz on your own material — that you actually learned what you say you learned.

## Core Value Proposition

Breaks are earned, not just timed. The proof is grounded in the student's own uploaded material (via RAG), so it can't be gamed with generic trivia, and failing a verification is never framed as punishment — it's framed as "not yet," with a clear path forward.

## Core Loop (22 steps)

1. Student registers/logs in.
2. Student creates a Subject (e.g. "Organic Chemistry") with an optional exam date.
3. Student uploads study material (PDF) for that subject.
4. Backend validates and processes the PDF (extract → clean → chunk → embed).
5. AI extracts a topic/unit structure from the material.
6. Student reviews and confirms the extracted topics (edit/merge/split if needed).
7. Student selects which apps to restrict during focus sessions (e.g. Instagram, YouTube, games).
8. Student sets a focus session duration (1h / 2h / 3h / 4h / custom).
9. App recommends a break schedule based on the duration (see `BREAK-SYSTEM.md`).
10. Student starts the focus session; restricted apps become blocked; a foreground service and notification keep the session alive and visible.
11. Student studies. The timer runs in the background, tracked by timestamp, surviving lock/backgrounding/process death.
12. At a scheduled break point, the app notifies the student a break is available.
13. Student either takes the scheduled break, defers it, or continues studying past it.
14. Alternatively, at any point, the student can request an **unscheduled break** by tapping "Break Now."
15. Student selects which topics they claim to have completed since the last verification.
16. App generates a rapid-fire quiz grounded via RAG on the student's own material, covering only those claimed topics.
17. Student answers one question at a time, each timed, screen auto-advancing (no back/skip/pause).
18. App scores the quiz.
19. If score ≥ threshold (default 70%, configurable): break unlocks immediately.
20. If score < threshold: break remains locked; app shows encouraging, non-judgmental copy and lets the student keep studying and try again later.
21. Break ends (scheduled duration or manual "done"); focus session resumes; restricted apps stay blocked.
22. On session completion, app records study time, quiz history, and topic performance, and updates the analytics dashboard (including surfacing weak topics for extra attention).

## Main Features

- Subject / exam / material management
- PDF upload & processing pipeline (text extraction, topic detection, chunking, embedding)
- Topic extraction & topic completion tracking
- Focus session timer (foreground service, background-durable)
- App restriction (manual selection + best-effort enforcement)
- Break recommendation engine + scheduled/deferred/unscheduled breaks
- AI-generated, RAG-grounded rapid-fire verification quiz
- Quiz scoring, pass/fail, break unlocking
- Study time / quiz / topic performance analytics, weak-topic surfacing
- Rule-based study planner (MVP; not ML)
- Authentication (JWT)
- Notifications for break availability, session completion, etc.
- Basic offline resilience for in-progress sessions

## MVP Boundaries

**In scope:**
- Android app only.
- Text-based PDF uploads only (no OCR/scanned documents).
- Manual, student-driven app selection for restriction (no automatic distraction classification).
- Rule-based study planner (exam date, remaining topics, difficulty, weak topics, available time).
- Single AI provider configured at a time, behind the `AIService` abstraction.
- One quiz "attempt" per unscheduled break request (no retries mid-quiz).

**Out of scope for MVP** (see `FUTURE-ROADMAP.md` for detail):
- OCR / scanned PDF support.
- iOS app.
- Adaptive/ML-based study planning.
- Automatic distraction classification (ML-based "is this app distracting" detection).
- Wearable integration, gamification systems, collaborative study, custom fine-tuned models.

## Product Principles

1. **Study material is the ground truth.** The AI never invents quiz content unrelated to what the student uploaded, except when a feature explicitly asks it to go beyond (none do, in MVP).
2. **Breaks are earned, never denied permanently.** A failed verification is a "not yet," not a "no." The student can always keep studying and try again.
3. **Never punitive language.** Failure states are described in terms of what to do next, never in terms of the student's inadequacy.

   | Never say | Say instead |
   |---|---|
   | "You failed the quiz." | "Break remains locked — keep studying and try again." |
   | "Wrong answers: 4/10. You're not ready." | "You got 6/10 (60%). You need 70% to unlock this break. A little more review should get you there." |
   | "You didn't learn this topic." | "This topic could use another pass — want to jump back into your notes on it?" |
   | "Blocked. Try harder." | "Almost there — take a few more minutes with this topic, then try the check again." |

4. **Honesty about technical limits.** The app never claims to guarantee perfect app blocking (Android doesn't allow that for third-party apps) — see `APP-RESTRICTION.md`.
5. **Respect the student's time.** Quiz is rapid-fire and short by design — it verifies recall, it is not a re-exam.
6. **Privacy by default.** Study material is personal and sometimes sensitive (uploaded notes, textbooks); treat it as private data, never shared across users, minimally logged (see `PRIVACY.md`).

## Future Features (brief — see `FUTURE-ROADMAP.md`)

OCR for scanned PDFs, adaptive AI-driven study planning, smarter distraction classification, behavioral insights, iOS app, wearable integration, richer gamification, custom fine-tuned models, collaborative study, richer spaced-repetition revision system.
