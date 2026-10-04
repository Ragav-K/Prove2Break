def test_register_success(client):
    response = client.post(
        "/api/auth/register", json={"email": "alice@example.com", "password": "correct-horse"}
    )
    assert response.status_code == 201
    body = response.json()
    assert body["email"] == "alice@example.com"
    assert "id" in body


def test_register_duplicate_email(client):
    payload = {"email": "bob@example.com", "password": "correct-horse"}
    first = client.post("/api/auth/register", json=payload)
    assert first.status_code == 201

    second = client.post("/api/auth/register", json=payload)
    assert second.status_code == 409
    assert second.json()["detail"] == "Email already registered"


def test_login_success(client):
    payload = {"email": "carol@example.com", "password": "correct-horse"}
    client.post("/api/auth/register", json=payload)

    response = client.post("/api/auth/login", json=payload)
    assert response.status_code == 200
    body = response.json()
    assert "access_token" in body
    assert "refresh_token" in body
    assert body["token_type"] == "bearer"
    assert body["expires_in"] > 0


def test_login_wrong_password(client):
    payload = {"email": "dave@example.com", "password": "correct-horse"}
    client.post("/api/auth/register", json=payload)

    response = client.post(
        "/api/auth/login", json={"email": "dave@example.com", "password": "wrong-password"}
    )
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid email or password"


def test_login_nonexistent_email_same_error_as_wrong_password(client):
    response = client.post(
        "/api/auth/login", json={"email": "nobody@example.com", "password": "whatever"}
    )
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid email or password"


def test_refresh_token_flow(client):
    payload = {"email": "erin@example.com", "password": "correct-horse"}
    client.post("/api/auth/register", json=payload)
    login_response = client.post("/api/auth/login", json=payload)
    refresh_token = login_response.json()["refresh_token"]

    response = client.post("/api/auth/refresh", json={"refresh_token": refresh_token})
    assert response.status_code == 200
    body = response.json()
    assert "access_token" in body
    assert body["expires_in"] > 0


def test_refresh_with_invalid_token(client):
    response = client.post("/api/auth/refresh", json={"refresh_token": "not-a-real-token"})
    assert response.status_code == 401


def test_refresh_with_access_token_rejected(client):
    """An access token must not be usable at /auth/refresh (wrong token type)."""
    payload = {"email": "frank@example.com", "password": "correct-horse"}
    client.post("/api/auth/register", json=payload)
    login_response = client.post("/api/auth/login", json=payload)
    access_token = login_response.json()["access_token"]

    response = client.post("/api/auth/refresh", json={"refresh_token": access_token})
    assert response.status_code == 401


def test_health_check(client):
    """docs/DEPLOYMENT.md: /health must check DB connectivity, not just process liveness."""
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["db"] == "reachable"
