import uuid
from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field


class QuizGenerateRequest(BaseModel):
    subject_id: uuid.UUID
    topic_ids: List[uuid.UUID] = Field(min_length=1)
    num_questions: int = Field(default=5, ge=1, le=20)
    time_limit_seconds: int = Field(default=15, ge=5, le=120)


class QuestionPublic(BaseModel):
    """Served to the client with the answer key + grounding trace stripped
    (docs/QUIZ-SYSTEM.md "Question JSON Schema")."""

    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    topic_id: uuid.UUID
    prompt: str
    options: List[str]
    time_limit_seconds: int
    order_index: int


class QuizResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    subject_id: uuid.UUID
    pass_threshold_used: float
    created_at: datetime
    questions: List[QuestionPublic]


class AttemptStartResponse(BaseModel):
    attempt_id: uuid.UUID
    quiz_id: uuid.UUID
    questions: List[QuestionPublic]


class AnswerSubmit(BaseModel):
    question_id: uuid.UUID
    selected_option_index: Optional[int] = None
    time_taken_seconds: Optional[float] = None


class QuizSubmitRequest(BaseModel):
    answers: List[AnswerSubmit] = Field(min_length=1)


class QuizSubmitResponse(BaseModel):
    attempt_id: uuid.UUID
    score: float
    passed: bool
    correct_count: int
    total_questions: int


class QuizResultResponse(BaseModel):
    attempt_id: uuid.UUID
    quiz_id: uuid.UUID
    score: float
    passed: bool
    correct_count: int
    total_questions: int
    completed_at: Optional[datetime]
