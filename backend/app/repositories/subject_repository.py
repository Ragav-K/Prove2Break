"""Data-access layer for Subject. Only this module constructs Subject queries."""
import uuid
from typing import Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.subject import Subject


class SubjectRepository:
    def __init__(self, db: Session):
        self.db = db

    def list_for_user(self, user_id: uuid.UUID) -> list[Subject]:
        stmt = select(Subject).where(Subject.user_id == user_id).order_by(Subject.created_at.desc())
        return list(self.db.execute(stmt).scalars().all())

    def get_for_user(self, subject_id: uuid.UUID, user_id: uuid.UUID) -> Optional[Subject]:
        stmt = select(Subject).where(Subject.id == subject_id, Subject.user_id == user_id)
        return self.db.execute(stmt).scalar_one_or_none()

    def create(self, user_id: uuid.UUID, **fields) -> Subject:
        subject = Subject(user_id=user_id, **fields)
        self.db.add(subject)
        self.db.commit()
        self.db.refresh(subject)
        return subject

    def update(self, subject: Subject, **fields) -> Subject:
        for key, value in fields.items():
            if value is not None:
                setattr(subject, key, value)
        self.db.commit()
        self.db.refresh(subject)
        return subject

    def delete(self, subject: Subject) -> None:
        self.db.delete(subject)
        self.db.commit()
