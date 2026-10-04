"""Phase 4 — Study Management: subjects, units, topics, study_materials, document_chunks

Revision ID: 8f2a5c9d1e3b
Revises: 6ca1c2787613
Create Date: 2026-09-28 00:00:00.000000
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = "8f2a5c9d1e3b"
down_revision = "6ca1c2787613"
branch_labels = None
depends_on = None

EMBEDDING_DIM = 1536


def upgrade() -> None:
    # pgvector extension must exist before any `vector(...)` column is created.
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")

    op.create_table(
        "subjects",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("exam_name", sa.String(length=255), nullable=True),
        sa.Column("exam_date", sa.Date(), nullable=True),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_subjects_user_id", "subjects", ["user_id"])

    op.create_table(
        "units",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("subject_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("subjects.id"), nullable=False),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("order", sa.Integer(), nullable=False, server_default="0"),
    )
    op.create_index("ix_units_subject_id", "units", ["subject_id"])

    op.create_table(
        "topics",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("subject_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("subjects.id"), nullable=False),
        sa.Column("unit_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("units.id"), nullable=True),
        sa.Column("name", sa.String(length=255), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("source_reference", sa.String(length=255), nullable=True),
        sa.Column("completion_status", sa.String(length=20), nullable=False, server_default="NOT_STARTED"),
        sa.Column("difficulty", sa.String(length=10), nullable=False, server_default="MEDIUM"),
        sa.Column("performance", sa.Float(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_topics_subject_id", "topics", ["subject_id"])
    op.create_index("ix_topics_unit_id", "topics", ["unit_id"])

    op.create_table(
        "study_materials",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("subject_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("subjects.id"), nullable=False),
        sa.Column("filename", sa.String(length=255), nullable=False),
        sa.Column("s3_key", sa.String(length=512), nullable=False),
        sa.Column("page_count", sa.Integer(), nullable=True),
        sa.Column("status", sa.String(length=20), nullable=False, server_default="PENDING"),
        sa.Column("failure_reason", sa.String(length=255), nullable=True),
        sa.Column("uploaded_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_study_materials_subject_id", "study_materials", ["subject_id"])

    op.create_table(
        "document_chunks",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column(
            "study_material_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("study_materials.id"),
            nullable=False,
        ),
        sa.Column("chunk_text", sa.Text(), nullable=False),
        sa.Column("chunk_index", sa.Integer(), nullable=False),
        # Created as a placeholder Text column here, then altered to a real
        # pgvector `vector(1536)` column below via literal SQL — keeps this
        # migration independent of the `pgvector` python package being
        # importable at migration time.
        sa.Column("embedding", sa.Text(), nullable=True),
    )
    op.create_index("ix_document_chunks_study_material_id", "document_chunks", ["study_material_id"])

    # Replace the placeholder Text column with a real pgvector `vector(1536)` column,
    # then add an ivfflat index for approximate-nearest-neighbor similarity search
    # (the core RAG retrieval index per docs/DATABASE.md). Autogenerate does not know
    # pgvector-specific DDL, so both statements are hand-written per
    # docs/BACKEND-ARCHITECTURE.md's migration guidance.
    op.execute(f"ALTER TABLE document_chunks ALTER COLUMN embedding TYPE vector({EMBEDDING_DIM}) USING NULL")
    op.execute(
        "CREATE INDEX ix_document_chunks_embedding ON document_chunks "
        "USING ivfflat (embedding vector_l2_ops) WITH (lists = 100)"
    )


def downgrade() -> None:
    op.execute("DROP INDEX IF EXISTS ix_document_chunks_embedding")
    op.drop_table("document_chunks")
    op.drop_table("study_materials")
    op.drop_table("topics")
    op.drop_table("units")
    op.drop_table("subjects")
