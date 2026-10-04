from app.ai.providers import MockAIProvider
from tests.conftest import register_and_login
from tests.pdf_fixtures import make_empty_pdf, make_text_pdf


def _create_subject(client, headers):
    return client.post("/api/subjects", json={"name": "History"}, headers=headers).json()["id"]


def test_upload_valid_pdf_processes_to_ready(client):
    headers = register_and_login(client, "upload1@example.com")
    subject_id = _create_subject(client, headers)

    pdf_bytes = make_text_pdf()
    response = client.post(
        "/api/materials/upload",
        data={"subject_id": subject_id},
        files={"file": ("notes.pdf", pdf_bytes, "application/pdf")},
        headers=headers,
    )
    assert response.status_code == 201
    body = response.json()
    assert body["status"] == "READY"
    material_id = body["id"]

    status_response = client.get(f"/api/materials/{material_id}/status", headers=headers)
    assert status_response.status_code == 200
    assert status_response.json()["status"] == "READY"

    # Topics should have been detected from the "Unit N" headings.
    topics = client.get(f"/api/subjects/{subject_id}/topics", headers=headers).json()
    assert len(topics) >= 1
    names = [t["name"] for t in topics]
    assert any("Unit 1" in n for n in names)


def test_upload_rejects_non_pdf(client):
    headers = register_and_login(client, "upload2@example.com")
    subject_id = _create_subject(client, headers)

    response = client.post(
        "/api/materials/upload",
        data={"subject_id": subject_id},
        files={"file": ("notes.txt", b"just some text", "text/plain")},
        headers=headers,
    )
    assert response.status_code == 415


def test_upload_scanned_like_pdf_fails_gracefully(client):
    """A structurally valid PDF with no extractable text should FAIL with a clear reason,
    not silently produce empty output (docs/PDF-PROCESSING.md)."""
    headers = register_and_login(client, "upload3@example.com")
    subject_id = _create_subject(client, headers)

    pdf_bytes = make_empty_pdf()
    response = client.post(
        "/api/materials/upload",
        data={"subject_id": subject_id},
        files={"file": ("scanned.pdf", pdf_bytes, "application/pdf")},
        headers=headers,
    )
    assert response.status_code == 201
    body = response.json()
    assert body["status"] == "FAILED"
    assert body["failure_reason"] == "scanned document detected"


def test_upload_to_nonexistent_subject_404s(client):
    headers = register_and_login(client, "upload4@example.com")
    pdf_bytes = make_text_pdf()
    response = client.post(
        "/api/materials/upload",
        data={"subject_id": "00000000-0000-0000-0000-000000000000"},
        files={"file": ("notes.pdf", pdf_bytes, "application/pdf")},
        headers=headers,
    )
    assert response.status_code == 404


def test_upload_marks_ai_error_on_embedding_failure(client, monkeypatch):
    """docs/ERROR-HANDLING.md "AI failure" row: an AIService exception during embedding
    must never leak as a raw 500 — the material should end up FAILED/ai_error instead."""

    def _boom(self, text, dim):
        raise RuntimeError("simulated embedding provider outage")

    monkeypatch.setattr(MockAIProvider, "embed_text", _boom)

    headers = register_and_login(client, "upload5@example.com")
    subject_id = _create_subject(client, headers)

    response = client.post(
        "/api/materials/upload",
        data={"subject_id": subject_id},
        files={"file": ("notes.pdf", make_text_pdf(), "application/pdf")},
        headers=headers,
    )
    assert response.status_code == 201
    body = response.json()
    assert body["status"] == "FAILED"
    assert body["failure_reason"] == "ai_error"

    # No orphaned topics should have been committed from the partially-processed material.
    topics = client.get(f"/api/subjects/{subject_id}/topics", headers=headers).json()
    assert topics == []


def test_material_ownership_isolation(client):
    headers_a = register_and_login(client, "matowner@example.com")
    headers_b = register_and_login(client, "matother@example.com")
    subject_id = _create_subject(client, headers_a)

    upload = client.post(
        "/api/materials/upload",
        data={"subject_id": subject_id},
        files={"file": ("notes.pdf", make_text_pdf(), "application/pdf")},
        headers=headers_a,
    )
    material_id = upload.json()["id"]

    forbidden = client.get(f"/api/materials/{material_id}", headers=headers_b)
    assert forbidden.status_code == 404
