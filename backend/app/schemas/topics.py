import uuid
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field

from app.models.topic import TopicCompletionStatus, TopicDifficulty


class TopicResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    subject_id: uuid.UUID
    unit_id: Optional[uuid.UUID]
    name: str
    description: Optional[str]
    source_reference: Optional[str]
    completion_status: TopicCompletionStatus
    difficulty: TopicDifficulty
    performance: Optional[float]


class TopicUpdate(BaseModel):
    """Manual topic edits — the AI-generated topic structure must never be forced
    on the user without control (docs/PROCESS.md acceptance criteria #3)."""

    name: Optional[str] = Field(default=None, min_length=1, max_length=255)
    is_completed: Optional[bool] = None
