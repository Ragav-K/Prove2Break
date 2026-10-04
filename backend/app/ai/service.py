"""AIService abstraction (Phase 6 — AI-RAG).

Business logic (services/) depends on this interface only — no code outside
`app/ai/` imports a vendor SDK or knows which provider is active, per
docs/AI-RAG.md's "Provider-Swappability Requirement" and the decision log in
docs/PROCESS.md.

Two implementations exist:

- `MockAIProvider` — a fully-working, deterministic, template-based provider.
  It does NOT call any external LLM/embedding API. It is the ONLY provider
  exercised by this codebase and its tests today, because no real API key is
  available in this environment. It generates genuinely grounded MCQ
  questions by extracting sentences from the retrieved chunk text and
  blanking a term, and produces embeddings via a deterministic hashed
  bag-of-words vector. This is NEVER "real AI" — every question it produces
  is template-derived from the student's own material, not model-generated
  free text.
- `OpenAICompatibleProvider` — the seam for a real provider later. It reads
  `AI_PROVIDER`/`AI_API_KEY`/`AI_MODEL_NAME` (see app/core/config.py) and is
  structurally wired (same interface, picked up by the same factory), but its
  methods raise `NotImplementedError` until a real HTTP client integration is
  added — dropping in a real call is then a change isolated to this one file.

`get_ai_service()` (app/ai/factory.py) is the single place that decides which
implementation to hand out, based on `Settings.AI_PROVIDER`.
"""
from abc import ABC, abstractmethod
from typing import Any, Dict, List


class AIService(ABC):
    """Provider-agnostic interface. See module docstring for the two implementations."""

    @abstractmethod
    def generate_quiz(self, topic_chunks: List[Dict[str, Any]], num_questions: int) -> List[Dict[str, Any]]:
        """Generate quiz questions grounded in the given document chunks.

        `topic_chunks` is a list of:
            {"topic_id": str, "topic_name": str, "chunks": [{"id": str, "text": str}, ...]}

        Returns a list of question dicts matching the docs/QUIZ-SYSTEM.md schema:
            {"topic_id", "prompt", "options" (list[str], len 4),
             "correct_option_index" (int), "source_chunk_ids" (list[str])}
        """
        raise NotImplementedError

    @abstractmethod
    def embed_text(self, text: str, dim: int) -> List[float]:
        """Return an embedding vector of length `dim` for `text`."""
        raise NotImplementedError

    def analyze_document(self, text: str) -> Dict[str, Any]:
        """Extraction/cleanup support beyond app/document_processing/pipeline.py.

        Out of scope for this phase (the deterministic PDF pipeline already
        covers extraction/cleaning/section+topic detection/chunking — see
        docs/PDF-PROCESSING.md). Kept as a thin stub so the interface matches
        docs/AI-RAG.md; callers should not currently depend on this.
        """
        raise NotImplementedError("analyze_document is out of scope for Phase 6 — see app/document_processing/")

    def generate_topics(self, sections: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Out of scope for this phase — topic detection is handled by the
        heuristic section detector in app/document_processing/pipeline.py
        (Phase 4). Stub kept for interface completeness only."""
        raise NotImplementedError("generate_topics is out of scope for Phase 6 — see app/document_processing/")

    def generate_study_plan(self, *args: Any, **kwargs: Any) -> Dict[str, Any]:
        """Out of scope for this phase (belongs to Phase 7 — Analytics / STUDY-PLANNER.md). Stub only."""
        raise NotImplementedError("generate_study_plan is out of scope for Phase 6")

    def generate_recommendations(self, *args: Any, **kwargs: Any) -> List[Dict[str, Any]]:
        """Out of scope for this phase (belongs to Phase 7 — Analytics). Stub only."""
        raise NotImplementedError("generate_recommendations is out of scope for Phase 6")


class NotImplementedAIService(AIService):
    """Kept for backward compatibility with the Phase 1 stub name/behavior.

    Superseded by `MockAIProvider` (app/ai/providers.py) as the default via
    `get_ai_service()`; this class is no longer wired up anywhere but is left
    in place in case anything still imports it directly.
    """

    def generate_quiz(self, topic_chunks: List[Dict[str, Any]], num_questions: int) -> List[Dict[str, Any]]:
        raise NotImplementedError("AI quiz generation is not implemented yet (Phase 1 stub).")

    def embed_text(self, text: str, dim: int) -> List[float]:
        raise NotImplementedError("Embedding generation is not implemented yet (Phase 1 stub).")
