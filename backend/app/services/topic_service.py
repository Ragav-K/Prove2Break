import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import NotFoundError
from app.models.topic import Topic, TopicCompletionStatus
from app.repositories.subject_repository import SubjectRepository
from app.repositories.topic_repository import TopicRepository


class TopicService:
    def __init__(self, db: Session):
        self.db = db
        self.topics = TopicRepository(db)
        self.subjects = SubjectRepository(db)

    def list_for_subject(self, subject_id: uuid.UUID, user_id: uuid.UUID) -> list[Topic]:
        subject = self.subjects.get_for_user(subject_id, user_id)
        if subject is None:
            raise NotFoundError("Subject not found")
        return self.topics.list_for_subject(subject_id)

    def update_topic(
        self,
        topic_id: uuid.UUID,
        user_id: uuid.UUID,
        name: str | None = None,
        is_completed: bool | None = None,
    ) -> Topic:
        topic = self.topics.get_for_user(topic_id, user_id)
        if topic is None:
            raise NotFoundError("Topic not found")

        fields: dict = {}
        if name is not None:
            fields["name"] = name
        if is_completed is not None:
            fields["completion_status"] = (
                TopicCompletionStatus.COMPLETED if is_completed else TopicCompletionStatus.NOT_STARTED
            )
        return self.topics.update(topic, **fields)
