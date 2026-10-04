"""Data-access layer for Topic and Unit."""
import uuid
from typing import Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.subject import Subject
from app.models.topic import Topic
from app.models.unit import Unit


class TopicRepository:
    def __init__(self, db: Session):
        self.db = db

    def list_for_subject(self, subject_id: uuid.UUID) -> list[Topic]:
        stmt = select(Topic).where(Topic.subject_id == subject_id).order_by(Topic.created_at.asc())
        return list(self.db.execute(stmt).scalars().all())

    def get_for_user(self, topic_id: uuid.UUID, user_id: uuid.UUID) -> Optional[Topic]:
        stmt = (
            select(Topic)
            .join(Subject, Topic.subject_id == Subject.id)
            .where(Topic.id == topic_id, Subject.user_id == user_id)
        )
        return self.db.execute(stmt).scalar_one_or_none()

    def create(self, **fields) -> Topic:
        topic = Topic(**fields)
        self.db.add(topic)
        return topic

    def get_or_create_unit(self, subject_id: uuid.UUID, name: str, order: int) -> Unit:
        stmt = select(Unit).where(Unit.subject_id == subject_id, Unit.name == name)
        unit = self.db.execute(stmt).scalar_one_or_none()
        if unit is None:
            unit = Unit(subject_id=subject_id, name=name, order=order)
            self.db.add(unit)
            self.db.flush()
        return unit

    def update(self, topic: Topic, **fields) -> Topic:
        for key, value in fields.items():
            if value is not None:
                setattr(topic, key, value)
        self.db.commit()
        self.db.refresh(topic)
        return topic

    def commit(self) -> None:
        self.db.commit()
