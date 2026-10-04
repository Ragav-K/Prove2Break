from tests.conftest import register_and_login
from tests.quiz_pdf_fixtures import make_quiz_source_pdf


def _setup_subject_with_topics(client, headers):
    subject_id = client.post("/api/subjects", json={"name": "Biology"}, headers=headers).json()["id"]
    upload = client.post(
        "/api/materials/upload",
        data={"subject_id": subject_id},
        files={"file": ("bio.pdf", make_quiz_source_pdf(), "application/pdf")},
        headers=headers,
    )
    assert upload.status_code == 201, upload.text
    topics = client.get(f"/api/subjects/{subject_id}/topics", headers=headers).json()
    return subject_id, topics


def test_generate_quiz_produces_valid_schema(client):
    headers = register_and_login(client, "quiz1@example.com")
    subject_id, topics = _setup_subject_with_topics(client, headers)
    assert len(topics) >= 2
    topic_ids = [t["id"] for t in topics]

    response = client.post(
        "/api/quiz/generate",
        json={"subject_id": subject_id, "topic_ids": topic_ids, "num_questions": 4, "time_limit_seconds": 15},
        headers=headers,
    )
    assert response.status_code == 201, response.text
    body = response.json()

    assert body["subject_id"] == subject_id
    assert 0 < len(body["questions"]) <= 4
    seen_topic_ids = {t["id"] for t in topics}
    for q in body["questions"]:
        assert set(q.keys()) == {"id", "topic_id", "prompt", "options", "time_limit_seconds", "order_index"}
        assert q["topic_id"] in seen_topic_ids
        assert len(q["options"]) == 4
        normalized = {o.strip().lower() for o in q["options"]}
        assert len(normalized) == 4  # no duplicate options
        # correct_option_index / source_chunk_ids must never be exposed to the client.
        assert "correct_option_index" not in q
        assert "source_chunk_ids" not in q


def _generate_quiz(client, headers, num_questions=4):
    subject_id, topics = _setup_subject_with_topics(client, headers)
    topic_ids = [t["id"] for t in topics]
    response = client.post(
        "/api/quiz/generate",
        json={"subject_id": subject_id, "topic_ids": topic_ids, "num_questions": num_questions},
        headers=headers,
    )
    assert response.status_code == 201, response.text
    return response.json()


def test_quiz_generation_rejects_unowned_topic(client):
    headers_a = register_and_login(client, "quizowner@example.com")
    headers_b = register_and_login(client, "quizother@example.com")
    subject_id, topics = _setup_subject_with_topics(client, headers_a)
    topic_ids = [t["id"] for t in topics]

    response = client.post(
        "/api/quiz/generate",
        json={"subject_id": subject_id, "topic_ids": topic_ids, "num_questions": 3},
        headers=headers_b,
    )
    assert response.status_code == 404


def test_attempt_submit_and_result_pass_case(client):
    headers = register_and_login(client, "quizpass@example.com")
    quiz = _generate_quiz(client, headers)
    quiz_id = quiz["id"]

    attempt_resp = client.post(f"/api/quiz/{quiz_id}/attempt", headers=headers)
    assert attempt_resp.status_code == 201, attempt_resp.text
    attempt_body = attempt_resp.json()
    questions = attempt_body["questions"]
    assert len(questions) == len(quiz["questions"])

    # We don't know the correct index from the client-facing payload (by design),
    # so fetch the quiz's internal question data isn't exposed. Instead brute-force:
    # answer index 0 for everything, then use /result to check consistency of the
    # score math rather than asserting a specific pass/fail outcome from this angle.
    answers = [{"question_id": q["id"], "selected_option_index": 0, "time_taken_seconds": 3.5} for q in questions]
    submit_resp = client.post(
        f"/api/quiz/{quiz_id}/submit", json={"answers": answers}, headers=headers
    )
    assert submit_resp.status_code == 200, submit_resp.text
    submit_body = submit_resp.json()
    assert submit_body["total_questions"] == len(questions)
    assert 0.0 <= submit_body["score"] <= 1.0
    expected_pass = submit_body["correct_count"] / submit_body["total_questions"] >= 0.70
    assert submit_body["passed"] == expected_pass

    result_resp = client.get(f"/api/quiz/{quiz_id}/result", headers=headers)
    assert result_resp.status_code == 200
    result_body = result_resp.json()
    assert result_body["score"] == submit_body["score"]
    assert result_body["passed"] == submit_body["passed"]
    assert result_body["correct_count"] == submit_body["correct_count"]


def test_submit_timed_out_answer_counts_as_incorrect(client):
    headers = register_and_login(client, "quiztimeout@example.com")
    quiz = _generate_quiz(client, headers, num_questions=3)
    quiz_id = quiz["id"]

    client.post(f"/api/quiz/{quiz_id}/attempt", headers=headers)
    questions = quiz["questions"]
    # All timed out (selected_option_index=null) -> score must be 0 and fail.
    answers = [{"question_id": q["id"], "selected_option_index": None} for q in questions]
    submit_resp = client.post(f"/api/quiz/{quiz_id}/submit", json={"answers": answers}, headers=headers)
    assert submit_resp.status_code == 200
    body = submit_resp.json()
    assert body["correct_count"] == 0
    assert body["score"] == 0.0
    assert body["passed"] is False


def test_submit_without_open_attempt_conflicts(client):
    headers = register_and_login(client, "quiznoattempt@example.com")
    quiz = _generate_quiz(client, headers, num_questions=2)
    quiz_id = quiz["id"]
    answers = [{"question_id": q["id"], "selected_option_index": 0} for q in quiz["questions"]]
    response = client.post(f"/api/quiz/{quiz_id}/submit", json={"answers": answers}, headers=headers)
    assert response.status_code == 409


def test_result_without_completed_attempt_not_found(client):
    headers = register_and_login(client, "quiznoresult@example.com")
    quiz = _generate_quiz(client, headers, num_questions=2)
    response = client.get(f"/api/quiz/{quiz['id']}/result", headers=headers)
    assert response.status_code == 404


def test_quiz_ownership_isolation_on_attempt_and_result(client):
    headers_a = register_and_login(client, "quizisoowner@example.com")
    headers_b = register_and_login(client, "quizisoother@example.com")
    quiz = _generate_quiz(client, headers_a, num_questions=2)
    quiz_id = quiz["id"]

    attempt_resp = client.post(f"/api/quiz/{quiz_id}/attempt", headers=headers_b)
    assert attempt_resp.status_code == 404

    result_resp = client.get(f"/api/quiz/{quiz_id}/result", headers=headers_b)
    assert result_resp.status_code == 404


def test_generate_quiz_rejects_no_material_topics(client):
    headers = register_and_login(client, "quiznomat@example.com")
    subject_id = client.post("/api/subjects", json={"name": "Empty"}, headers=headers).json()["id"]
    # No topics exist at all for this subject -> referencing any topic id 404s.
    fake_topic_id = "00000000-0000-0000-0000-000000000000"
    response = client.post(
        "/api/quiz/generate",
        json={"subject_id": subject_id, "topic_ids": [fake_topic_id], "num_questions": 3},
        headers=headers,
    )
    assert response.status_code == 404
