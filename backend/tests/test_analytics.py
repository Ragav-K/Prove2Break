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


def test_analytics_overview_empty_for_new_user(client):
    headers = register_and_login(client, "analytics-empty@example.com")
    response = client.get("/api/analytics/overview", headers=headers)
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["total_attempts"] == 0
    assert body["pass_rate"] == 0.0
    assert body["total_quizzes"] == 0


def _run_full_quiz(client, headers, subject_id, topic_ids, force_all_correct: bool):
    gen = client.post(
        "/api/quiz/generate",
        json={"subject_id": subject_id, "topic_ids": topic_ids, "num_questions": 4},
        headers=headers,
    )
    assert gen.status_code == 201, gen.text
    quiz = gen.json()
    quiz_id = quiz["id"]

    start = client.post(f"/api/quiz/{quiz_id}/attempt", headers=headers)
    assert start.status_code == 201, start.text
    questions = start.json()["questions"]

    # We don't know the correct index (stripped from the client schema), so to
    # deterministically produce a pass/fail we peek at the DB-level Question
    # records is not available via API; instead submit index 0 for all and
    # rely on that some quizzes will pass/fail naturally. Because this is
    # nondeterministic for pass/fail assertions, we only assert structural
    # correctness of the analytics response here (see test below for score math
    # via the repository directly, which is deterministic).
    answers = [{"question_id": q["id"], "selected_option_index": 0, "time_taken_seconds": 5} for q in questions]
    submit = client.post(f"/api/quiz/{quiz_id}/submit", json={"answers": answers}, headers=headers)
    assert submit.status_code == 200, submit.text
    return quiz_id, submit.json()


def test_analytics_updates_after_quiz_submission(client):
    headers = register_and_login(client, "analytics-quiz@example.com")
    subject_id, topics = _setup_subject_with_topics(client, headers)
    topic_ids = [t["id"] for t in topics]

    _run_full_quiz(client, headers, subject_id, topic_ids, force_all_correct=True)

    overview = client.get("/api/analytics/overview", headers=headers).json()
    assert overview["total_attempts"] == 1
    assert overview["total_quizzes"] == 1

    topics_resp = client.get("/api/analytics/topics", headers=headers).json()
    assert len(topics_resp["topics"]) >= 1
    for row in topics_resp["topics"]:
        assert row["attempt_count"] == 1
        assert 0.0 <= row["average_score"] <= 1.0
        assert row["topic_name"] is not None


def test_analytics_ownership_isolation(client):
    headers_a = register_and_login(client, "analytics-owner@example.com")
    headers_b = register_and_login(client, "analytics-other@example.com")

    subject_id, topics = _setup_subject_with_topics(client, headers_a)
    topic_ids = [t["id"] for t in topics]
    _run_full_quiz(client, headers_a, subject_id, topic_ids, force_all_correct=True)

    overview_b = client.get("/api/analytics/overview", headers=headers_b).json()
    assert overview_b["total_attempts"] == 0
    assert overview_b["total_quizzes"] == 0

    topics_b = client.get("/api/analytics/topics", headers=headers_b).json()
    assert topics_b["topics"] == []


def test_weak_topics_threshold(client, db_session):
    """Directly exercises the repository/service weak-topic math with a known
    score, since the API can't force a deterministic wrong-answer rate.
    """
    import uuid

    from app.repositories.analytics_repository import AnalyticsRepository
    from app.services.analytics_service import AnalyticsService

    headers = register_and_login(client, "weak-topics@example.com")
    subject_id, topics = _setup_subject_with_topics(client, headers)
    topic_id = uuid.UUID(topics[0]["id"])

    # Fetch the user id via a protected endpoint isn't exposed directly; derive
    # it through the analytics repository using the topic's owning user, found
    # via the DB session shared with the app under test.
    from app.models.user import User

    user = db_session.query(User).filter(User.email == "weak-topics@example.com").one()

    repo = AnalyticsRepository(db_session)
    repo.upsert_topic_performance(user.id, topic_id, 0.5)  # below 0.70 threshold
    db_session.commit()

    service = AnalyticsService(db_session)
    weak = service.get_weak_topics(user.id)
    assert len(weak) == 1
    assert weak[0]["average_score"] == 0.5

    all_topics = service.get_topic_performance(user.id)
    assert len(all_topics) == 1


def test_topic_performance_rolling_average(db_session):
    import uuid

    from app.repositories.analytics_repository import AnalyticsRepository

    user_id = uuid.uuid4()
    topic_id = uuid.uuid4()
    repo = AnalyticsRepository(db_session)

    repo.upsert_topic_performance(user_id, topic_id, 1.0)
    row = repo.upsert_topic_performance(user_id, topic_id, 0.0)
    db_session.commit()

    assert row.attempt_count == 2
    assert row.average_score == 0.5
