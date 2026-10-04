from tests.conftest import register_and_login


def test_create_and_list_subject(client):
    headers = register_and_login(client, "alice@example.com")

    response = client.post(
        "/api/subjects",
        json={"name": "Biology", "exam_name": "Final", "exam_date": "2026-12-01"},
        headers=headers,
    )
    assert response.status_code == 201
    body = response.json()
    assert body["name"] == "Biology"
    assert body["exam_name"] == "Final"

    listing = client.get("/api/subjects", headers=headers)
    assert listing.status_code == 200
    items = listing.json()
    assert len(items) == 1
    assert items[0]["topics_total"] == 0


def test_get_update_delete_subject(client):
    headers = register_and_login(client, "bob@example.com")
    created = client.post("/api/subjects", json={"name": "Chemistry"}, headers=headers).json()
    subject_id = created["id"]

    got = client.get(f"/api/subjects/{subject_id}", headers=headers)
    assert got.status_code == 200
    assert got.json()["name"] == "Chemistry"

    updated = client.put(f"/api/subjects/{subject_id}", json={"name": "Chemistry II"}, headers=headers)
    assert updated.status_code == 200
    assert updated.json()["name"] == "Chemistry II"

    deleted = client.delete(f"/api/subjects/{subject_id}", headers=headers)
    assert deleted.status_code == 204

    missing = client.get(f"/api/subjects/{subject_id}", headers=headers)
    assert missing.status_code == 404


def test_subject_not_found_for_bad_id(client):
    headers = register_and_login(client, "carol@example.com")
    response = client.get("/api/subjects/00000000-0000-0000-0000-000000000000", headers=headers)
    assert response.status_code == 404


def test_ownership_isolation_between_users(client):
    headers_a = register_and_login(client, "usera@example.com")
    headers_b = register_and_login(client, "userb@example.com")

    created = client.post("/api/subjects", json={"name": "Physics"}, headers=headers_a).json()
    subject_id = created["id"]

    # User B cannot see user A's subject.
    forbidden_get = client.get(f"/api/subjects/{subject_id}", headers=headers_b)
    assert forbidden_get.status_code == 404

    # User B cannot edit it either.
    forbidden_put = client.put(f"/api/subjects/{subject_id}", json={"name": "Hacked"}, headers=headers_b)
    assert forbidden_put.status_code == 404

    # User B's subject list is empty.
    listing_b = client.get("/api/subjects", headers=headers_b)
    assert listing_b.json() == []

    # User A's subject list still has exactly one entry.
    listing_a = client.get("/api/subjects", headers=headers_a)
    assert len(listing_a.json()) == 1


def test_requires_auth(client):
    response = client.get("/api/subjects")
    assert response.status_code in (401, 403)
