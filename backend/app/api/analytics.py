from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.db import get_db
from app.models.topic_performance import WEAK_TOPIC_THRESHOLD
from app.models.user import User
from app.schemas.analytics import (
    AnalyticsOverviewResponse,
    TopicPerformanceListResponse,
    WeakTopicsResponse,
)
from app.services.analytics_service import AnalyticsService

router = APIRouter(prefix="/api/analytics", tags=["analytics"])


@router.get("/overview", response_model=AnalyticsOverviewResponse)
def get_overview(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = AnalyticsService(db)
    return service.get_overview(current_user.id)


@router.get("/topics", response_model=TopicPerformanceListResponse)
def get_topics(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = AnalyticsService(db)
    return TopicPerformanceListResponse(topics=service.get_topic_performance(current_user.id))


@router.get("/weak-topics", response_model=WeakTopicsResponse)
def get_weak_topics(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    service = AnalyticsService(db)
    topics = service.get_weak_topics(current_user.id, WEAK_TOPIC_THRESHOLD)
    return WeakTopicsResponse(threshold=WEAK_TOPIC_THRESHOLD, topics=topics)
