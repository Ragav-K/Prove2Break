from tests.conftest import register_and_login
from tests.pdf_fixtures import make_text_pdf


def _create_subject_with_topics(client, headers):
    subject_id = client.post("/api/subjects", json={"name": "Math"}, headers=headers).json()["id"]
    client.post(
        "/api/materials/upload",
        data={"subject_id": subject_id},
        files={"file": ("notes.pdf", make_text_pdf(), "application/pdf")},
        headers=headers,
    )
    topics = client.get(f"/api/subjects/{subject_id}/topics", headers=headers).json()
    return subject_id, topics


def test_mark_topic_complete_and_rename(client):
    headers = register_and_login(client, "topics1@example.com")
    _, topics = _create_subject_with_topics(client, headers)
    assert len(topics) >= 1
    topic_id = topics[0]["id"]
    assert topics[0]["completion_status"] == "NOT_STARTED"

    response = client.put(
        f"/api/topics/{topic_id}/completion",
        json={"is_completed": True, "name": "Renamed Topic"},
        headers=headers,
    )
    assert response.status_code == 200
    body = response.json()
    assert body["completion_status"] == "COMPLETED"
    assert body["name"] == "Renamed Topic"

    # Un-complete it again — manual control must always be possible.
    response2 = client.put(
        f"/api/topics/{topic_id}/completion", json={"is_completed": False}, headers=headers
    )
    assert response2.json()["completion_status"] == "NOT_STARTED"


def test_topic_not_found(client):
    headers = register_and_login(client, "topics2@example.com")
    response = client.put(
        "/api/topics/00000000-0000-0000-0000-000000000000/completion",
        json={"is_completed": True},
        headers=headers,
    )
    assert response.status_code == 404


def test_topic_ownership_isolation(client):
    headers_a = register_and_login(client, "topicowner@example.com")
    headers_b = register_and_login(client, "topicother@example.com")
    _, topics = _create_subject_with_topics(client, headers_a)
    topic_id = topics[0]["id"]

    response = client.put(
        f"/api/topics/{topic_id}/completion", json={"is_completed": True}, headers=headers_b
    )
    assert response.status_code == 404
