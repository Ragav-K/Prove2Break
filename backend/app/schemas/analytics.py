import uuid
from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel


class AnalyticsOverviewResponse(BaseModel):
    total_attempts: int
    passed_attempts: int
    pass_rate: float
    average_score: float
    total_quizzes: int


class TopicPerformanceResponse(BaseModel):
    topic_id: uuid.UUID
    topic_name: Optional[str]
    attempt_count: int
    average_score: float
    last_attempt_at: Optional[datetime]


class TopicPerformanceListResponse(BaseModel):
    topics: List[TopicPerformanceResponse]


class WeakTopicsResponse(BaseModel):
    threshold: float
    topics: List[TopicPerformanceResponse]
