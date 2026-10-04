import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.core.exceptions import NotFoundError
from app.db import get_db
from app.models.user import User
from app.schemas.subjects import SubjectCreate, SubjectListItem, SubjectResponse, SubjectUpdate
from app.services.subject_service import SubjectService

router = APIRouter(prefix="/api/subjects", tags=["subjects"])


@router.get("", response_model=list[SubjectListItem])
def list_subjects(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = SubjectService(db)
    return service.list_subjects(current_user.id)


@router.post("", response_model=SubjectResponse, status_code=status.HTTP_201_CREATED)
def create_subject(
    payload: SubjectCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = SubjectService(db)
    return service.create_subject(current_user.id, **payload.model_dump())


@router.get("/{subject_id}", response_model=SubjectResponse)
def get_subject(
    subject_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = SubjectService(db)
    try:
        return service.get_subject(subject_id, current_user.id)
    except NotFoundError:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Subject not found")


@router.put("/{subject_id}", response_model=SubjectResponse)
def update_subject(
    subject_id: uuid.UUID,
    payload: SubjectUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = SubjectService(db)
    try:
        return service.update_subject(subject_id, current_user.id, **payload.model_dump())
    except NotFoundError:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Subject not found")


@router.delete("/{subject_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_subject(
    subject_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = SubjectService(db)
    try:
        service.delete_subject(subject_id, current_user.id)
    except NotFoundError:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Subject not found")
