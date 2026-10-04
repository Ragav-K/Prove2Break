from app.document_processing.pipeline import (
    REASON_SCANNED_DOCUMENT,
    REASON_UNSUPPORTED_FORMAT,
    chunk_text,
    clean_text,
    detect_sections,
    run_pipeline,
)
from tests.pdf_fixtures import make_empty_pdf, make_text_pdf


def test_run_pipeline_on_valid_text_pdf():
    result = run_pipeline(
        pdf_bytes=make_text_pdf(),
        filename="notes.pdf",
        content_type="application/pdf",
        max_size_bytes=50 * 1024 * 1024,
        max_pages=500,
    )
    assert result.status == "READY"
    assert result.page_count == 2
    assert len(result.chunks) >= 1
    assert any("Unit 1" in s.title for s in result.sections)
    assert any("Unit 2" in s.title for s in result.sections)


def test_run_pipeline_rejects_scanned_like_pdf():
    result = run_pipeline(
        pdf_bytes=make_empty_pdf(),
        filename="scan.pdf",
        content_type="application/pdf",
        max_size_bytes=50 * 1024 * 1024,
        max_pages=500,
    )
    assert result.status == "FAILED"
    assert result.failure_reason == REASON_SCANNED_DOCUMENT


def test_run_pipeline_rejects_non_pdf_bytes():
    result = run_pipeline(
        pdf_bytes=b"not a pdf at all",
        filename="fake.pdf",
        content_type="application/pdf",
        max_size_bytes=50 * 1024 * 1024,
        max_pages=500,
    )
    assert result.status == "FAILED"
    assert result.failure_reason == REASON_UNSUPPORTED_FORMAT


def test_clean_text_strips_page_numbers_and_normalizes_whitespace():
    raw = "Some   text\n\n\n12\nPage 3\nMore text"
    cleaned = clean_text(raw)
    assert "12" not in cleaned.split("\n")
    assert "Page 3" not in cleaned
    assert "Some text" in cleaned


def test_detect_sections_splits_on_headings():
    pages = ["Unit 1: Intro\nBody text one.", "Unit 2: Advanced\nBody text two."]
    sections = detect_sections(pages)
    assert len(sections) == 2
    assert sections[0].title.startswith("Unit 1")
    assert sections[1].title.startswith("Unit 2")


def test_chunk_text_respects_size_and_overlap():
    text = "x" * 2000
    chunks = chunk_text(text, chunk_size=800, overlap=150)
    assert len(chunks) > 1
    assert all(len(c) <= 800 for c in chunks)


def test_chunk_text_empty_returns_no_chunks():
    assert chunk_text("   ") == []
