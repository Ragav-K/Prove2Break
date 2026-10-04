import os
import uuid

from sqlalchemy.orm import Session

from app.ai.factory import get_ai_service
from app.core.config import Settings
from app.core.exceptions import NotFoundError
from app.document_processing.pipeline import run_pipeline
from app.models.document_chunk import EMBEDDING_DIM, DocumentChunk
from app.models.study_material import StudyMaterial, StudyMaterialStatus
from app.repositories.material_repository import MaterialRepository
from app.repositories.subject_repository import SubjectRepository
from app.repositories.topic_repository import TopicRepository


class MaterialService:
    def __init__(self, db: Session, settings: Settings):
        self.db = db
        self.settings = settings
        self.materials = MaterialRepository(db)
        self.subjects = SubjectRepository(db)
        self.topics = TopicRepository(db)
        self.ai = get_ai_service(settings)

    def upload_and_process(
        self,
        subject_id: uuid.UUID,
        user_id: uuid.UUID,
        filename: str,
        content_type: str | None,
        file_bytes: bytes,
    ) -> StudyMaterial:
        subject = self.subjects.get_for_user(subject_id, user_id)
        if subject is None:
            raise NotFoundError("Subject not found")

        storage_path = self._store_file(subject_id, filename, file_bytes)
        material = self.materials.create(subject_id=subject_id, filename=filename, s3_key=storage_path)

        self.materials.update_status(material, status=StudyMaterialStatus.PROCESSING)

        result = run_pipeline(
            pdf_bytes=file_bytes,
            filename=filename,
            content_type=content_type,
            max_size_bytes=self.settings.MAX_UPLOAD_SIZE_BYTES,
            max_pages=self.settings.MAX_PDF_PAGES,
        )

        if result.status == "FAILED":
            self.materials.update_status(
                material,
                status=StudyMaterialStatus.FAILED,
                failure_reason=result.failure_reason,
            )
            return material

        # Naive section -> Unit/Topic creation (MVP heuristic; user can freely edit
        # per docs/PROCESS.md acceptance criteria #3 — AI/heuristic structure is
        # never forced without user control).
        #
        # Phase 6 (AI-RAG): chunks are re-derived per-section here (rather than
        # from the pipeline's flat `result.chunks`) so each DocumentChunk can be
        # attributed to the Topic created from its own section — this is what
        # lets retrieval scope precisely to "chunks belonging to this topic"
        # (docs/AI-RAG.md). Each chunk's embedding is generated immediately via
        # AIService.embed_text (see app/ai/providers.py for the mock/placeholder
        # nature of that embedding).
        from app.document_processing.pipeline import chunk_text as _chunk_text

        chunk_index = 0
        chunks: list[DocumentChunk] = []
        try:
            for order, section in enumerate(result.sections):
                unit = self.topics.get_or_create_unit(subject_id, section.title[:255], order)
                topic = self.topics.create(
                    subject_id=subject_id,
                    unit_id=unit.id,
                    name=section.title[:255],
                    description=None,
                    source_reference=material.filename,
                )
                self.db.flush()  # assign topic.id before use below

                for piece in _chunk_text(section.text):
                    embedding = self.ai.embed_text(piece, EMBEDDING_DIM)
                    chunks.append(
                        DocumentChunk(
                            study_material_id=material.id,
                            topic_id=topic.id,
                            chunk_text=piece,
                            chunk_index=chunk_index,
                            embedding=embedding,
                        )
                    )
                    chunk_index += 1
        except Exception:
            # AI/embedding failure (docs/ERROR-HANDLING.md "AI failure" row): never let
            # a raw provider exception propagate as an unhandled 500 — roll back any
            # partial units/topics and mark the material FAILED with reason ai_error so
            # the client gets the documented "we couldn't process this material" copy.
            self.db.rollback()
            self.materials.update_status(
                material,
                status=StudyMaterialStatus.FAILED,
                failure_reason="ai_error",
            )
            return material

        self.topics.commit()
        self.materials.add_chunks(chunks)

        self.materials.update_status(
            material,
            status=StudyMaterialStatus.READY,
            page_count=result.page_count,
        )
        return material

    def get_material(self, material_id: uuid.UUID, user_id: uuid.UUID) -> StudyMaterial:
        material = self.materials.get_for_user(material_id, user_id)
        if material is None:
            raise NotFoundError("Study material not found")
        return material

    def _store_file(self, subject_id: uuid.UUID, filename: str, file_bytes: bytes) -> str:
        directory = os.path.join(self.settings.STORAGE_DIR, str(subject_id))
        os.makedirs(directory, exist_ok=True)
        safe_name = f"{uuid.uuid4()}_{os.path.basename(filename)}"
        path = os.path.join(directory, safe_name)
        with open(path, "wb") as f:
            f.write(file_bytes)
        return path
