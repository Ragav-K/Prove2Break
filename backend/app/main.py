from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.api.analytics import router as analytics_router
from app.api.auth import router as auth_router
from app.api.materials import router as materials_router
from app.api.quiz import router as quiz_router
from app.api.subjects import router as subjects_router
from app.api.topics import router as topics_router
from app.api.topics import subject_topics_router
from app.core.config import get_settings
from app.db import get_db

settings = get_settings()

app = FastAPI(title="Prove2Break API", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(subjects_router)
app.include_router(materials_router)
app.include_router(subject_topics_router)
app.include_router(topics_router)
app.include_router(quiz_router)
app.include_router(analytics_router)


@app.get("/health")
def health(db: Session = Depends(get_db)):
    """Readiness/liveness check per docs/DEPLOYMENT.md — checks DB connectivity,
    not just process liveness, so a deploy platform can distinguish "process up
    but DB unreachable" from a genuinely healthy instance."""
    try:
        db.execute(text("SELECT 1"))
    except Exception:
        return {"status": "error", "db": "unreachable"}
    return {"status": "ok", "db": "reachable"}
