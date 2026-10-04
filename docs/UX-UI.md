# Prove2Break — UX / UI Design

## Design Language

- **Material 3** as the foundation — dynamic color where available, but with a Prove2Break-specific seed palette (calm blues/greens for focus states, warm amber for break states, never harsh red for "failed" states — see Product Principles in `PRODUCT.md`).
- **Clean, purposeful cards** — information density kept low; a screen should read at a glance. Avoid stacking more than 2-3 cards' worth of primary content per screen.
- **Typography hierarchy** — one clear headline per screen, supporting text de-emphasized (lower opacity/weight, not smaller-and-smaller sizes stacked infinitely).
- **Accessible contrast** — all text/background pairs meet WCAG AA at minimum; never rely on color alone to convey pass/fail or locked/unlocked state (always paired with icon + text).
- **Subtle animation** — state transitions (timer ticking, break unlocking, quiz question advancing) use short, purposeful motion (150–300ms) to communicate cause and effect, not decoration for its own sake.
- **Meaningful progress indicators** — the focus timer, quiz countdown, and topic-completion bars are the app's signature visual motif; they should feel consistent across screens (same shape language: ring or bar, not both mixed inconsistently).
- **Explicitly NOT** a generic chatbot look (no message-bubble UI, no "AI assistant" persona chrome) and NOT overloaded with cards competing for attention — this is a focus tool, its own UI should not be a source of distraction.

## Screen Inventory

| # | Screen | Purpose |
|---|---|---|
| 1 | Splash / Auth Check | Determine if a valid session exists, route to Login or Home. |
| 2 | Login | Authenticate an existing user. |
| 3 | Register | Create a new account. |
| 4 | Home / Dashboard | Entry point: quick-start focus session, subject overview, analytics snapshot. |
| 5 | Subject List | Browse all subjects with progress summary. |
| 6 | Subject Detail | View a subject's materials, topics, and exam date. |
| 7 | Create/Edit Subject | Set subject name, description, exam date. |
| 8 | Material Upload | Pick and upload a PDF for a subject. |
| 9 | Material Processing Status | Show pipeline progress (extracting/chunking/embedding) with failure/retry state. |
| 10 | Topic Review | Review, edit, merge/split, confirm AI-extracted topics. |
| 11 | Topic Completion Toggle (in-context) | Mark a topic as studied/complete. |
| 12 | App Restriction Setup | Pick which installed apps to restrict. |
| 13 | Permission Explainer | Explain why a specific sensitive permission is needed before requesting it. |
| 14 | Focus Session Setup | Choose subject + duration before starting. |
| 15 | Active Focus Session | Live timer, current state, quick actions (Break Now, End Session). |
| 16 | Blocked-App Interstitial | Shown when a restricted app is opened during focus. |
| 17 | Scheduled Break Prompt | Notify break is available; Take / Defer / Continue. |
| 18 | Active Break Screen | Break countdown, Extend / End Break Now. |
| 19 | Topic Selection for Unscheduled Break | Choose which claimed-complete topics the quiz should cover. |
| 20 | Rapid-Fire Quiz Screen | One question at a time, timed, auto-advancing (see rules below). |
| 21 | Quiz Result Screen | Pass (break unlocked) or lock-with-encouragement message. |
| 22 | Session Summary | End-of-session recap: time studied, breaks taken, quiz results. |
| 23 | Analytics Dashboard | Study time, sessions, quiz history, topic performance, weak topics. |
| 24 | Weak Topics Detail | List of topics below threshold with gentle framing and a "study this" shortcut. |
| 25 | Study Planner | Rule-based suggested study order ahead of the exam date. |
| 26 | Emergency Access Confirmation | Confirm exiting restriction for a genuine emergency. |
| 27 | Settings | Quiz threshold, notification preferences, restricted-app list management, account. |

## Rapid-Fire Quiz Screen — Key UI Rules

1. **One question at a time.** Never show a scrollable list of questions or a progress list revealing future questions' content.
2. **Visible countdown per question.** Both a numeric timer and a shrinking progress bar/ring — never color alone.
3. **No back, no skip, no pause.** These controls are intentionally absent; the quiz is a rapid-fire check of recall, not a re-take-able exam.
4. **Auto-advance** on answer selection (short confirm animation) or on timeout (counted as incorrect, brief "time's up" indicator before advancing).
5. **No score shown mid-quiz.** Running score/tally is not displayed during the quiz to avoid gaming behavior or anxiety spirals — only the final result screen shows the outcome.
6. **Exit confirmation only for accidental navigation** (e.g. system back/home) — leaving mid-quiz counts as an incomplete attempt, which counts toward FAIL, and this consequence is stated plainly before they leave.
