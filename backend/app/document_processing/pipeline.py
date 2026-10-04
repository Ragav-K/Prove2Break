"""PDF upload/processing pipeline (per docs/PDF-PROCESSING.md).

Scope (MVP, matches docs/PDF-PROCESSING.md exactly):
    Upload -> Validation -> Text Extraction -> Cleaning -> Section Detection
           -> Topic Detection -> Chunking

Explicitly OUT of scope for this phase:
    - OCR / scanned-image PDFs (future scope, see docs/FUTURE-ROADMAP.md). A PDF
      with near-zero extractable text density is rejected with a clear reason
      rather than silently producing empty output.
    - Embedding generation (Phase 6 / AI-RAG) — chunks are persisted with
      `embedding=None`; a later phase fills them in via `AIService`.

Library choice: `pypdf` (pure-Python, no native/system dependencies such as
poppler that `pdfplumber` requires) — simpler to install reliably in this
environment and sufficient for page-level text extraction on text-based PDFs.

Failure handling: fail-fast — the whole pipeline stops on the first stage
failure (per docs/PDF-PROCESSING.md's stated MVP default), returning a
`PipelineResult` with `status=FAILED` and a `failure_reason` drawn from a small
fixed set of user-facing categories (never a raw stack trace).
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field

from pypdf import PdfReader
from pypdf.errors import PdfReadError

# --- Failure reason categories (docs/PDF-PROCESSING.md "Failure Handling") ---
REASON_UNSUPPORTED_FORMAT = "unsupported format"
REASON_SCANNED_DOCUMENT = "scanned document detected"
REASON_EXTRACTION_ERROR = "extraction error"
REASON_TOO_LARGE = "file too large"
REASON_TOO_MANY_PAGES = "too many pages"
REASON_INTERNAL_ERROR = "internal error"

MIN_TEXT_DENSITY_CHARS_PER_PAGE = 20  # below this, a page is considered "no extractable text"
MIN_TEXT_PAGE_RATIO = 0.5  # at least half the sampled pages must have real text
CHUNK_SIZE_CHARS = 800
CHUNK_OVERLAP_CHARS = 150

_HEADING_PATTERNS = [
    re.compile(r"^\s*(Unit|Chapter|Module|Section)\s+\d+[:.\-]?\s*(.*)$", re.IGNORECASE),
    re.compile(r"^\s*\d+(\.\d+)*\s+[A-Z][A-Za-z0-9 ,'\-]{2,80}$"),
]


@dataclass
class DetectedSection:
    title: str
    text: str


@dataclass
class DetectedChunk:
    text: str
    index: int


@dataclass
class PipelineResult:
    status: str  # "READY" or "FAILED"
    failure_reason: str | None = None
    page_count: int = 0
    sections: list[DetectedSection] = field(default_factory=list)
    chunks: list[DetectedChunk] = field(default_factory=list)


class PipelineError(Exception):
    def __init__(self, reason: str):
        super().__init__(reason)
        self.reason = reason


def validate_upload(filename: str, content_type: str | None, size_bytes: int, max_size_bytes: int) -> None:
    """Stage 2 (partial): file type/size checks that don't require parsing the PDF."""
    if not filename.lower().endswith(".pdf") or (content_type and content_type != "application/pdf"):
        raise PipelineError(REASON_UNSUPPORTED_FORMAT)
    if size_bytes <= 0:
        raise PipelineError(REASON_UNSUPPORTED_FORMAT)
    if size_bytes > max_size_bytes:
        raise PipelineError(REASON_TOO_LARGE)


def extract_text_per_page(pdf_bytes: bytes, max_pages: int) -> list[str]:
    """Stage 3: text extraction, preserving page boundaries."""
    try:
        reader = PdfReader(__import__("io").BytesIO(pdf_bytes))
    except (PdfReadError, Exception) as exc:  # noqa: BLE001 - any parse failure -> unsupported format
        raise PipelineError(REASON_UNSUPPORTED_FORMAT) from exc

    if reader.is_encrypted:
        try:
            reader.decrypt("")
        except Exception:  # noqa: BLE001
            raise PipelineError(REASON_EXTRACTION_ERROR)

    if len(reader.pages) > max_pages:
        raise PipelineError(REASON_TOO_MANY_PAGES)

    pages_text: list[str] = []
    try:
        for page in reader.pages:
            pages_text.append(page.extract_text() or "")
    except Exception as exc:  # noqa: BLE001
        raise PipelineError(REASON_EXTRACTION_ERROR) from exc

    if not pages_text:
        raise PipelineError(REASON_EXTRACTION_ERROR)

    _assert_text_based(pages_text)
    return pages_text


def _assert_text_based(pages_text: list[str]) -> None:
    """Validation stage: reject scanned/image-only PDFs (near-zero text density)."""
    pages_with_text = sum(1 for p in pages_text if len(p.strip()) >= MIN_TEXT_DENSITY_CHARS_PER_PAGE)
    ratio = pages_with_text / len(pages_text) if pages_text else 0
    if ratio < MIN_TEXT_PAGE_RATIO:
        raise PipelineError(REASON_SCANNED_DOCUMENT)


def clean_text(raw: str) -> str:
    """Stage 4: whitespace normalization + light header/footer/boilerplate stripping."""
    lines = [ln.strip() for ln in raw.splitlines()]
    cleaned_lines = []
    for ln in lines:
        if not ln:
            continue
        # Heuristic: standalone page-number-only lines ("12", "Page 12", "- 12 -").
        if re.fullmatch(r"(page\s*)?[-–]?\s*\d{1,4}\s*[-–]?", ln, re.IGNORECASE):
            continue
        cleaned_lines.append(ln)
    text = "\n".join(cleaned_lines)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def detect_sections(cleaned_pages: list[str]) -> list[DetectedSection]:
    """Stage 5/6 heuristic: split on heading-like lines (e.g. "Unit 1", "Chapter 2",
    numbered headings like "1.2 Some Topic"). MVP heuristic, not ML — good enough to
    seed Unit/Topic records without forcing structure the user can't override."""
    full_text = "\n".join(cleaned_pages)
    lines = full_text.split("\n")

    sections: list[DetectedSection] = []
    current_title = "General"
    current_lines: list[str] = []

    def _flush():
        body = "\n".join(current_lines).strip()
        if body:
            sections.append(DetectedSection(title=current_title, text=body))

    for line in lines:
        matched_title = None
        for pattern in _HEADING_PATTERNS:
            m = pattern.match(line)
            if m:
                matched_title = line.strip()
                break
        if matched_title:
            _flush()
            current_title = matched_title[:255]
            current_lines = []
        else:
            current_lines.append(line)
    _flush()

    if not sections:
        sections.append(DetectedSection(title="General", text=full_text.strip()))
    return sections


def chunk_text(text: str, chunk_size: int = CHUNK_SIZE_CHARS, overlap: int = CHUNK_OVERLAP_CHARS) -> list[str]:
    """Stage 7: overlapping character-based chunking (~500-1000 chars, modest overlap)."""
    text = text.strip()
    if not text:
        return []
    chunks: list[str] = []
    start = 0
    length = len(text)
    while start < length:
        end = min(start + chunk_size, length)
        chunk = text[start:end].strip()
        if chunk:
            chunks.append(chunk)
        if end >= length:
            break
        start = end - overlap
        if start < 0:
            start = 0
    return chunks


def run_pipeline(
    pdf_bytes: bytes,
    filename: str,
    content_type: str | None,
    max_size_bytes: int,
    max_pages: int,
) -> PipelineResult:
    """Runs the full fail-fast pipeline and returns a structured result — never raises
    for expected validation/processing failures (those become `status=FAILED` +
    `failure_reason`); only truly unexpected errors propagate as PipelineError with
    REASON_INTERNAL_ERROR having already been substituted for the raw exception.
    """
    try:
        validate_upload(filename, content_type, len(pdf_bytes), max_size_bytes)
        pages_text = extract_text_per_page(pdf_bytes, max_pages)
        cleaned_pages = [clean_text(p) for p in pages_text]
        sections = detect_sections(cleaned_pages)

        chunks: list[DetectedChunk] = []
        idx = 0
        for section in sections:
            for piece in chunk_text(section.text):
                chunks.append(DetectedChunk(text=piece, index=idx))
                idx += 1

        return PipelineResult(
            status="READY",
            page_count=len(pages_text),
            sections=sections,
            chunks=chunks,
        )
    except PipelineError as exc:
        return PipelineResult(status="FAILED", failure_reason=exc.reason)
    except Exception:  # noqa: BLE001 - never leak raw stack traces to the client
        return PipelineResult(status="FAILED", failure_reason=REASON_INTERNAL_ERROR)


def process_study_material(study_material_id: str) -> None:
    """Kept for interface-compat with the Phase 1 stub signature; the real MVP
    processing flow is synchronous and driven by `services/material_service.py`
    (see docs/PROCESS.md: "synchronous is fine for MVP" — a background task/queue
    would be the right upgrade for real async processing but is out of scope here).
    """
    raise NotImplementedError(
        "process_study_material(id) is superseded by services.material_service.MaterialService"
        " — call the synchronous pipeline via run_pipeline() instead."
    )
