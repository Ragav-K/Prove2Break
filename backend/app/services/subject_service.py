import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import NotFoundError
from app.models.subject import Subject
from app.models.topic import Topic, TopicCompletionStatus
from app.repositories.subject_repository import SubjectRepository


class SubjectService:
    def __init__(self, db: Session):
        self.db = db
        self.subjects = SubjectRepository(db)

    def list_subjects(self, user_id: uuid.UUID) -> list[dict]:
        subjects = self.subjects.list_for_user(user_id)
        results = []
        for subject in subjects:
            topics_total = len(subject.topics)
            topics_completed = sum(
                1 for t in subject.topics if t.completion_status == TopicCompletionStatus.COMPLETED
            )
            results.append(
                {
                    "id": subject.id,
                    "name": subject.name,
                    "exam_name": subject.exam_name,
                    "exam_date": subject.exam_date,
                    "description": subject.description,
                    "created_at": subject.created_at,
                    "topics_total": topics_total,
                    "topics_completed": topics_completed,
                }
            )
        return results

    def get_subject(self, subject_id: uuid.UUID, user_id: uuid.UUID) -> Subject:
        subject = self.subjects.get_for_user(subject_id, user_id)
        if subject is None:
            raise NotFoundError("Subject not found")
        return subject

    def create_subject(self, user_id: uuid.UUID, **fields) -> Subject:
        return self.subjects.create(user_id=user_id, **fields)

    def update_subject(self, subject_id: uuid.UUID, user_id: uuid.UUID, **fields) -> Subject:
        subject = self.get_subject(subject_id, user_id)
        return self.subjects.update(subject, **fields)

    def delete_subject(self, subject_id: uuid.UUID, user_id: uuid.UUID) -> None:
        subject = self.get_subject(subject_id, user_id)
        self.subjects.delete(subject)
