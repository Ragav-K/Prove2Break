# Prove2Break — Future Roadmap (Post-MVP)

Everything below is explicitly **not MVP scope**. It's recorded here so decisions made now (e.g. keeping `Quiz`/`QuizAttempt` separate, versioning `StudyPlan.algorithm_version`) can support these later without a schema rewrite.

- **OCR for scanned PDFs** — extend `PDF-PROCESSING.md`'s pipeline with an OCR stage for image-only documents, currently rejected outright in MVP.
- **Advanced adaptive AI study planner** — replace/augment the rule-based planner (`STUDY-PLANNER.md`) with a model that learns an individual student's retention/forgetting curve and adapts scheduling accordingly.
- **Smarter distraction classification** — automatically suggest which installed apps are likely distracting (ML-based), rather than requiring fully manual selection (`APP-RESTRICTION.md`).
- **Behavioral insights** — deeper pattern analysis in `ANALYTICS.md` (e.g. time-of-day performance correlation, procrastination pattern detection), surfaced gently and only if genuinely useful, not gamified pressure.
- **iOS app** — the backend is already built API-first and platform-agnostic (`ARCHITECTURE.md`) specifically to make this feasible without backend rework.
- **Wearable integration** — surface focus-session status and break notifications on a paired wearable.
- **Advanced gamification** — streaks/achievements beyond the simple factual streak count in MVP analytics — approached carefully so it doesn't conflict with the non-judgmental tone principle in `PRODUCT.md`.
- **Custom AI model** — a Prove2Break-specific fine-tuned or purpose-built model for quiz generation/topic extraction, instead of a general-purpose provider behind `AIService` — the abstraction already in place makes this a swap-in, not a rewrite.
- **Collaborative study** — shared subjects/study groups, shared (opt-in) material, group accountability features — requires new privacy/sharing model beyond MVP's strictly single-user data isolation (`PRIVACY.md`).
- **Richer revision system** — spaced-repetition-style recurring review scheduling for previously completed topics, beyond the current one-shot "mark complete" + weak-topic resurfacing model.
