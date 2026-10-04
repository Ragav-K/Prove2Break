import uuid
from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class SubjectCreate(BaseModel):
    name: str = Field(min_length=1, max_length=255)
    exam_name: Optional[str] = Field(default=None, max_length=255)
    exam_date: Optional[date] = None
    description: Optional[str] = None


class SubjectUpdate(BaseModel):
    name: Optional[str] = Field(default=None, min_length=1, max_length=255)
    exam_name: Optional[str] = Field(default=None, max_length=255)
    exam_date: Optional[date] = None
    description: Optional[str] = None


class SubjectResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    name: str
    exam_name: Optional[str]
    exam_date: Optional[date]
    description: Optional[str]
    created_at: datetime


class SubjectListItem(SubjectResponse):
    topics_total: int
    topics_completed: int
