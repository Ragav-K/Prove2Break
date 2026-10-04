# Prove2Break — AI & RAG Architecture

## RAG Architecture Diagram

```
[Student's claimed-complete Topics]
             │
             ▼
[Fetch DocumentChunks for those Topics' source material]
             │
             ▼
[Embed query context (topic name + representative chunk summary)]
             │
             ▼
[pgvector similarity search over DocumentChunk.embedding]
    (filtered to chunks belonging to the student's own material only)
             │
             ▼
[Top-K relevant chunks retrieved as grounding context]
             │
             ▼
[AIService.generateQuiz(topics, groundingChunks) -> Question[]]
             │
             ▼
[Validate schema + grounding (QUIZ-SYSTEM.md rules)]
             │
             ▼
[Persist Quiz + Questions, serve to Android client (answers hidden)]
```

## Embedding & Chunking Strategy

- Chunks are sized (~300–500 tokens, modest overlap) to balance retrieval precision against embedding cost — see `PDF-PROCESSING.md` for the chunking stage detail.
- Each chunk stores its source page/section and (once topic detection runs) its associated `topic_id`, so retrieval can be scoped precisely to "chunks belonging to this topic" rather than a generic whole-document search.
- Embeddings are generated via `AIService.analyzeDocument`/the embedding capability, keeping the embedding model choice behind the same provider-agnostic boundary as generation.

## pgvector Similarity Retrieval

- Retrieval query: for each claimed topic, fetch the top-K (default K=8–12 combined across topics) chunks by cosine/L2 distance (pgvector operator, e.g. `<=>`) scoped to `WHERE topic_id = ANY(:topic_ids)`.
- Retrieval is always scoped to the requesting user's own material — cross-user retrieval is structurally impossible since chunks are joined through `StudyMaterial.subject_id -> Subject.user_id`.
- If retrieval returns too few chunks for a topic (below a minimum threshold), that topic is flagged as having insufficient grounding material, and quiz generation either excludes it (if other topics are selected) or fails clearly ("not enough material on this topic yet to generate a fair quiz").

## Grounding Rule

**The AI never generates quiz content from outside the student's own uploaded material for that subject**, unless a future feature explicitly asks it to (none do in MVP). Every generated question must be traceable to specific `DocumentChunk` ids, stored on the `Question.source_chunk_ids` field, so grounding can be audited/debugged. This is the mechanism that makes "prove you learned this" meaningful rather than a generic trivia check.

## `AIService` Abstraction

A single interface all AI-backed features depend on — no calling code anywhere else imports a vendor SDK directly:

```
interface AIService {
    fun generateQuiz(topics: List<Topic>, groundingChunks: List<DocumentChunk>, questionCount: Int): List<Question>
    fun analyzeDocument(text: String): DocumentAnalysis   // extraction/cleanup/embedding support
    fun generateTopics(sections: List<DocumentSection>): List<TopicCandidate>
    fun generateStudyPlan(subject: Subject, topics: List<Topic>, examDate: Date?, weakTopics: List<Topic>): StudyPlan
    fun generateRecommendations(context: RecommendationContext): List<Recommendation>
}
```

(Signatures shown conceptually; actual backend implementation is Python/Pydantic-typed, living in `backend/ai/`.)

## Provider-Swappability Requirement

- No hard dependency on a single LLM vendor. The active provider is selected via `AI_PROVIDER` + `AI_API_KEY` + `AI_MODEL_NAME` env vars (see `ENVIRONMENT.md`), and `backend/ai/` contains one implementation module per supported provider behind the shared `AIService` interface.
- Business logic (quiz scoring, break unlocking, topic completion, study planning) never checks "which provider is this," so switching providers — or running a different provider per environment (e.g. cheaper model in dev) — requires only a config change, not a code change.
- This also enables testing: unit tests use a fake/mock `AIService` implementation instead of hitting a real provider.

## Implementation Status (Phase 6)

No real LLM/embedding API key is available in this environment, so:

- **`MockAIProvider`** (`backend/app/ai/providers.py`) is the active, fully-working
  implementation (`AI_PROVIDER` unset/`"mock"`, the default). It never calls a
  network API: `generate_quiz` extracts a sentence from a retrieved
  `DocumentChunk`, blanks a salient term, and builds distractors from
  vocabulary in the other retrieved chunks; `embed_text` is a deterministic
  hashed bag-of-words vector. Both are clearly-marked heuristics, never
  presented as real model output.
- **`OpenAICompatibleProvider`** is the seam for a real provider — reads
  `AI_PROVIDER`/`AI_API_KEY`/`AI_MODEL_NAME` and is wired into the same
  `get_ai_service()` factory, but its methods raise `NotImplementedError`
  until a real HTTP integration is added.
- **Retrieval** (`backend/app/ai/retrieval.py`) branches on the bound
  SQLAlchemy dialect: real pgvector cosine-distance ANN search on Postgres,
  or a keyword/text-overlap scoring fallback on SQLite (this environment's
  test suite — Docker's daemon was not running here, see `PROCESS.md` "Known
  Deferred Items"). The SQLite path is a documented approximation, not real
  semantic similarity, and is the only path exercised/verified so far.
