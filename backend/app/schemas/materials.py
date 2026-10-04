import uuid
from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict

from app.models.study_material import StudyMaterialStatus


class StudyMaterialResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    subject_id: uuid.UUID
    filename: str
    status: StudyMaterialStatus
    failure_reason: Optional[str]
    page_count: Optional[int]
    uploaded_at: datetime


class StudyMaterialStatusResponse(BaseModel):
    id: uuid.UUID
    status: StudyMaterialStatus
    failure_reason: Optional[str] = None
