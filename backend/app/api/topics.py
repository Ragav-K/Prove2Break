import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.core.exceptions import NotFoundError
from app.db import get_db
from app.models.user import User
from app.schemas.topics import TopicResponse, TopicUpdate
from app.services.topic_service import TopicService

# Mounted under /api/subjects/{id}/topics (list) per docs/API.md.
subject_topics_router = APIRouter(prefix="/api/subjects", tags=["topics"])

# Mounted under /api/topics/{id} (edit) per docs/API.md.
router = APIRouter(prefix="/api/topics", tags=["topics"])


@subject_topics_router.get("/{subject_id}/topics", response_model=list[TopicResponse])
def list_topics(
    subject_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = TopicService(db)
    try:
        return service.list_for_subject(subject_id, current_user.id)
    except NotFoundError:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Subject not found")


@router.put("/{topic_id}/completion", response_model=TopicResponse)
def update_topic_completion(
    topic_id: uuid.UUID,
    payload: TopicUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = TopicService(db)
    try:
        return service.update_topic(
            topic_id, current_user.id, name=payload.name, is_completed=payload.is_completed
        )
    except NotFoundError:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Topic not found")
