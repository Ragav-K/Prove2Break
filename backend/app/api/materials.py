import uuid

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from sqlalchemy.orm import Session

from app.auth.dependencies import get_current_user
from app.core.config import Settings, get_settings
from app.core.exceptions import NotFoundError
from app.db import get_db
from app.models.user import User
from app.schemas.materials import StudyMaterialResponse, StudyMaterialStatusResponse
from app.services.material_service import MaterialService

router = APIRouter(prefix="/api/materials", tags=["materials"])


@router.post("/upload", response_model=StudyMaterialResponse, status_code=status.HTTP_201_CREATED)
async def upload_material(
    subject_id: uuid.UUID = Form(...),
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
):
    if file.content_type not in ("application/pdf", None) and not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE, detail="Only PDF files are supported")

    file_bytes = await file.read()
    if not file_bytes:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="Empty file")
    if len(file_bytes) > settings.MAX_UPLOAD_SIZE_BYTES:
        raise HTTPException(status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, detail="File too large")

    service = MaterialService(db, settings)
    try:
        material = service.upload_and_process(
            subject_id=subject_id,
            user_id=current_user.id,
            filename=file.filename,
            content_type=file.content_type,
            file_bytes=file_bytes,
        )
    except NotFoundError:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Subject not found")

    return material


@router.get("/{material_id}", response_model=StudyMaterialResponse)
def get_material(
    material_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
):
    service = MaterialService(db, settings)
    try:
        return service.get_material(material_id, current_user.id)
    except NotFoundError:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Study material not found")


@router.get("/{material_id}/status", response_model=StudyMaterialStatusResponse)
def get_material_status(
    material_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    settings: Settings = Depends(get_settings),
):
    service = MaterialService(db, settings)
    try:
        material = service.get_material(material_id, current_user.id)
    except NotFoundError:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Study material not found")
    return StudyMaterialStatusResponse(
        id=material.id, status=material.status, failure_reason=material.failure_reason
    )
