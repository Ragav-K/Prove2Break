import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.core.config import Settings, get_settings
from app.core.exceptions import BadRequestError, NotFoundError
from app.db import get_db
from app.models.user import User
from app.schemas.quiz import (
    AttemptStartResponse,
    QuizGenerateRequest,
    QuizResponse,
    QuizResultResponse,
    QuizSubmitRequest,
    QuizSubmitResponse,
)
from app.services.quiz_service import QuizService

router = APIRouter(prefix="/api/quiz", tags=["quiz"])


@router.post("/generate", response_model=QuizResponse, status_code=status.HTTP_201_CREATED)
def generate_quiz(
    payload: QuizGenerateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
):
    service = QuizService(db, settings)
    try:
        quiz = service.generate_quiz(
            user_id=current_user.id,
            subject_id=payload.subject_id,
            topic_ids=payload.topic_ids,
            num_questions=payload.num_questions,
            time_limit_seconds=payload.time_limit_seconds,
        )
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc))
    except BadRequestError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc))
    return quiz


@router.post("/{quiz_id}/attempt", response_model=AttemptStartResponse, status_code=status.HTTP_201_CREATED)
def start_attempt(
    quiz_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
):
    service = QuizService(db, settings)
    try:
        attempt = service.start_attempt(quiz_id, current_user.id)
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc))
    quiz = service.quizzes.get_for_user(quiz_id, current_user.id)
    return AttemptStartResponse(attempt_id=attempt.id, quiz_id=quiz_id, questions=quiz.questions)


@router.post("/{quiz_id}/submit", response_model=QuizSubmitResponse)
def submit_quiz(
    quiz_id: uuid.UUID,
    payload: QuizSubmitRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
):
    service = QuizService(db, settings)
    try:
        attempt, correct_count, total_questions = service.submit_attempt(
            quiz_id, current_user.id, [a.model_dump() for a in payload.answers]
        )
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc))
    except BadRequestError as exc:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail=str(exc))
    return QuizSubmitResponse(
        attempt_id=attempt.id,
        score=attempt.final_score,
        passed=attempt.passed,
        correct_count=correct_count,
        total_questions=total_questions,
    )


@router.get("/{quiz_id}/result", response_model=QuizResultResponse)
def get_quiz_result(
    quiz_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
):
    service = QuizService(db, settings)
    try:
        attempt, total_questions = service.get_result(quiz_id, current_user.id)
    except NotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc))
    correct_count = round((attempt.final_score or 0.0) * total_questions)
    return QuizResultResponse(
        attempt_id=attempt.id,
        quiz_id=quiz_id,
        score=attempt.final_score,
        passed=attempt.passed,
        correct_count=correct_count,
        total_questions=total_questions,
        completed_at=attempt.completed_at,
    )
