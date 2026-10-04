"""Data-access layer for StudyMaterial/DocumentChunk."""
import uuid
from typing import Optional

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.document_chunk import DocumentChunk
from app.models.study_material import StudyMaterial
from app.models.subject import Subject


class MaterialRepository:
    def __init__(self, db: Session):
        self.db = db

    def create(self, subject_id: uuid.UUID, filename: str, s3_key: str) -> StudyMaterial:
        material = StudyMaterial(subject_id=subject_id, filename=filename, s3_key=s3_key)
        self.db.add(material)
        self.db.commit()
        self.db.refresh(material)
        return material

    def get_for_user(self, material_id: uuid.UUID, user_id: uuid.UUID) -> Optional[StudyMaterial]:
        stmt = (
            select(StudyMaterial)
            .join(Subject, StudyMaterial.subject_id == Subject.id)
            .where(StudyMaterial.id == material_id, Subject.user_id == user_id)
        )
        return self.db.execute(stmt).scalar_one_or_none()

    def update_status(self, material: StudyMaterial, **fields) -> StudyMaterial:
        for key, value in fields.items():
            setattr(material, key, value)
        self.db.commit()
        self.db.refresh(material)
        return material

    def add_chunks(self, chunks: list[DocumentChunk]) -> None:
        self.db.add_all(chunks)
        self.db.commit()
