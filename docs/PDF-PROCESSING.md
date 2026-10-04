# Prove2Break — PDF Processing Pipeline

## Scope (MVP)

**Text-based PDFs only.** Scanned/image-only PDFs are explicitly out of scope for MVP (OCR is future scope, see `FUTURE-ROADMAP.md`). The pipeline validates this up front and rejects with a clear message rather than silently producing empty/garbage output.

## Pipeline Stages

```
Upload -> Validation -> Text Extraction -> Cleaning -> Section Detection
       -> Topic Detection -> Chunking -> Embedding -> Vector DB (pgvector)
```

1. **Upload** — Android sends the PDF via multipart upload; backend stores the raw file in S3-compatible storage and creates a `StudyMaterial` row with `status=UPLOADED`.
2. **Validation** — file type check (must be `application/pdf`), size limit (default 50MB), page count limit (default 500 pages), and a text-content sanity check (sample a handful of pages; if extracted text density is near zero, flag as likely scanned/image-only and fail fast with a clear reason rather than proceeding).
3. **Text Extraction** — extract raw text per page using a PDF text-extraction library, preserving page boundaries for later chunk provenance.
4. **Cleaning** — strip headers/footers/page numbers/repeated boilerplate, normalize whitespace, fix common ligature/encoding artifacts.
5. **Section Detection** — heuristically detect heading-like structure (font-size/style cues if available, or numbering/pattern heuristics) to group text into candidate sections/units, feeding `Unit` creation.
6. **Topic Detection** — cleaned, sectioned text is passed to `AIService.generateTopics`, producing candidate `Topic` records tied back to their source sections.
7. **Chunking** — text is split into overlapping chunks sized for good embedding/retrieval quality (e.g. ~300–500 tokens with modest overlap), each chunk retaining its source page/section and (once assigned) topic reference.
8. **Embedding** — each chunk is embedded via `AIService.analyzeDocument`/the embedding call behind `AIService`, and the resulting `DocumentChunk` row (with `vector` column) is written to PostgreSQL/pgvector.
9. **Vector DB** — chunks become queryable via pgvector similarity search, which the RAG-grounded quiz generation flow (`AI-RAG.md`) depends on.

## Validation Rules

- File extension and MIME type must indicate PDF.
- Size ≤ 50MB (configurable).
- Page count ≤ 500 (configurable).
- Must contain extractable text (heuristic check in stage 2); otherwise rejected as "This looks like a scanned document — text-based PDFs are supported for now."
- Duplicate-upload detection (same subject + same content hash) surfaces a warning rather than silently reprocessing, to avoid wasted embedding cost.

## Failure Handling

- Every stage can fail independently; `StudyMaterial.status` moves to `FAILED` with a `failure_reason` string chosen from a small fixed set of user-facing categories (unsupported format, scanned document detected, extraction error, processing timeout, internal error) — never a raw stack trace surfaced to the client.
- Failures are retryable: the user can re-trigger processing from the Material Processing Status screen without re-uploading, unless the failure was a validation failure (wrong file), in which case they must upload a different file.
- Partial success (e.g. topic detection fails but chunking succeeded) still surfaces the chunks for retrieval while flagging topics as needing manual creation — decided at implementation time whether MVP treats this as a hard fail-fast pipeline instead for simplicity (default: fail-fast, whole pipeline stops on first stage failure, kept simple for MVP).
