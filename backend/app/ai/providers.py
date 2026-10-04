"""AIService implementations. See app/ai/service.py for the interface contract
and the mock-vs-real explanation; this module holds the actual code.

- `MockAIProvider`: the ONLY provider exercised in this environment (no real
  LLM/embedding API key available). Fully deterministic and template-based —
  never calls a network API. Produces genuinely grounded MCQ questions by
  extracting a sentence from a chunk of the student's own material and
  blanking a salient term, with distractors drawn from vocabulary in other
  chunks. Embeddings are a deterministic hashed bag-of-words vector — a
  placeholder standing in for a real embedding model, NOT a trained
  representation. This is explicitly NOT "real AI"; it is a heuristic stand-in
  behind the same interface a real provider will later implement.

- `OpenAICompatibleProvider`: the seam for a real provider. Reads
  AI_PROVIDER/AI_API_KEY/AI_MODEL_NAME (app/core/config.py). Structurally
  wired into the same interface/factory, but its methods are not implemented
  yet — dropping in a real HTTP call becomes a change isolated to this class.
"""
from __future__ import annotations

import hashlib
import random
import re
from typing import Any, Dict, List

from app.ai.service import AIService

REQUIRED_OPTION_COUNT = 4

_STOPWORDS = {
    "the", "and", "for", "are", "but", "not", "you", "with", "this", "that",
    "from", "have", "has", "had", "was", "were", "will", "would", "can",
    "could", "should", "into", "than", "then", "them", "they", "their",
    "there", "which", "when", "where", "what", "who", "whom", "your", "its",
    "his", "her", "our", "these", "those", "such", "also", "all", "any",
    "each", "more", "most", "some", "over", "under", "about", "after",
    "before", "between", "within", "while", "because", "since", "using",
}

_WORD_RE = re.compile(r"[A-Za-z][A-Za-z\-']{3,}")
_SENTENCE_RE = re.compile(r"(?<=[.!?])\s+")


def _candidate_terms(sentence: str) -> List[str]:
    """Salient-word heuristic: alphabetic words of length >= 4 that aren't stopwords,
    preferring capitalized/longer words (proxy for "important term")."""
    words = _WORD_RE.findall(sentence)
    terms = [w for w in words if w.lower() not in _STOPWORDS]
    # Prefer longer / capitalized terms first (rough salience proxy).
    terms.sort(key=lambda w: (w[:1].isupper(), len(w)), reverse=True)
    return terms


def _dedupe_options(options: List[str]) -> bool:
    seen = set()
    for opt in options:
        key = opt.strip().lower()
        if key in seen:
            return False
        seen.add(key)
    return True


class MockAIProvider(AIService):
    """Deterministic, template-based provider. See module docstring — NOT real AI."""

    def generate_quiz(self, topic_chunks: List[Dict[str, Any]], num_questions: int) -> List[Dict[str, Any]]:
        if num_questions <= 0:
            return []

        # Pool of all chunk texts across ALL topics, used as a source of distractor
        # vocabulary (never as a source of question content for a topic other than
        # its own — grounding stays scoped to the question's own topic's chunks).
        global_vocab: List[str] = []
        for entry in topic_chunks:
            for chunk in entry["chunks"]:
                global_vocab.extend(_candidate_terms(chunk["text"]))

        rng = random.Random(42)  # deterministic ordering/shuffling
        questions: List[Dict[str, Any]] = []

        # Round-robin across topics so questions spread across all selected topics.
        topic_cycle = [t for t in topic_chunks if t["chunks"]]
        if not topic_cycle:
            return []

        idx = 0
        attempts = 0
        max_attempts = num_questions * 20 + 50
        while len(questions) < num_questions and attempts < max_attempts:
            attempts += 1
            entry = topic_cycle[idx % len(topic_cycle)]
            idx += 1
            question = self._build_one_question(entry, global_vocab, rng)
            if question is not None:
                questions.append(question)

        return questions

    def _build_one_question(
        self, entry: Dict[str, Any], global_vocab: List[str], rng: random.Random
    ) -> Dict[str, Any] | None:
        chunks = entry["chunks"]
        if not chunks:
            return None
        chunk = rng.choice(chunks)
        sentences = [s.strip() for s in _SENTENCE_RE.split(chunk["text"]) if len(s.strip()) >= 25]
        if not sentences:
            return None
        sentence = rng.choice(sentences)
        terms = _candidate_terms(sentence)
        if not terms:
            return None
        answer_term = terms[0]

        # Blank the term (first case-insensitive occurrence) to build the prompt.
        pattern = re.compile(re.escape(answer_term), re.IGNORECASE)
        prompt_text, count = pattern.subn("_____", sentence, count=1)
        if count == 0:
            return None

        # Distractors: other salient words from the broader vocabulary, excluding
        # the answer itself and case-insensitive duplicates, of roughly similar length.
        candidates = [
            w for w in global_vocab
            if w.lower() != answer_term.lower()
        ]
        rng.shuffle(candidates)
        distractors: List[str] = []
        seen = {answer_term.strip().lower()}
        for w in candidates:
            key = w.strip().lower()
            if key in seen:
                continue
            seen.add(key)
            distractors.append(w)
            if len(distractors) == REQUIRED_OPTION_COUNT - 1:
                break

        if len(distractors) < REQUIRED_OPTION_COUNT - 1:
            return None  # not enough material to build 4 distinct options

        options = [answer_term] + distractors
        rng.shuffle(options)
        if not _dedupe_options(options):
            return None
        correct_index = options.index(answer_term)

        topic_chunk_ids = {c["id"] for c in chunks}
        return {
            "topic_id": entry["topic_id"],
            "prompt": f"Fill in the blank: {prompt_text}",
            "options": options,
            "correct_option_index": correct_index,
            "source_chunk_ids": [chunk["id"]] if chunk["id"] in topic_chunk_ids else list(topic_chunk_ids)[:1],
        }

    def embed_text(self, text: str, dim: int) -> List[float]:
        """Deterministic hashed bag-of-words placeholder embedding — NOT a trained
        embedding model. Each word hashes into a bucket in a `dim`-length vector,
        giving lexically-similar text similar (not identical) vectors, which is
        enough to exercise the storage/retrieval plumbing without a real model."""
        vector = [0.0] * dim
        words = re.findall(r"[a-z0-9]+", text.lower())
        if not words:
            return vector
        for word in words:
            digest = hashlib.sha256(word.encode("utf-8")).digest()
            bucket = int.from_bytes(digest[:4], "big") % dim
            sign = 1.0 if digest[4] % 2 == 0 else -1.0
            vector[bucket] += sign
        # L2-normalize so magnitude doesn't just track document length.
        norm = sum(v * v for v in vector) ** 0.5
        if norm > 0:
            vector = [v / norm for v in vector]
        return vector


class OpenAICompatibleProvider(AIService):
    """Seam for a real provider. Reads AI_PROVIDER/AI_API_KEY/AI_MODEL_NAME but
    does not call any network API yet — see module docstring. Wiring a real
    HTTP client here is the intended future change; nothing outside app/ai/
    needs to change when that happens."""

    def __init__(self, api_key: str, model_name: str):
        self.api_key = api_key
        self.model_name = model_name

    def generate_quiz(self, topic_chunks: List[Dict[str, Any]], num_questions: int) -> List[Dict[str, Any]]:
        raise NotImplementedError(
            "OpenAICompatibleProvider.generate_quiz is a seam for a real provider "
            "integration and is not implemented — set AI_PROVIDER to the default "
            "(mock) to use the working deterministic provider."
        )

    def embed_text(self, text: str, dim: int) -> List[float]:
        raise NotImplementedError(
            "OpenAICompatibleProvider.embed_text is a seam for a real embedding "
            "provider integration and is not implemented yet."
        )
