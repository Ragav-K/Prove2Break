"""Phase 6 — AI Verification: quizzes, questions, quiz_attempts, quiz_answers

Revision ID: 9a1b3e7c5f2d
Revises: 8f2a5c9d1e3b
Create Date: 2026-09-29 00:00:00.000000
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = "9a1b3e7c5f2d"
down_revision = "8f2a5c9d1e3b"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "quizzes",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("subject_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("subjects.id"), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("pass_threshold_used", sa.Float(), nullable=False, server_default="0.70"),
        sa.Column("score", sa.Float(), nullable=True),
        sa.Column("passed", sa.Boolean(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index("ix_quizzes_subject_id", "quizzes", ["subject_id"])
    op.create_index("ix_quizzes_user_id", "quizzes", ["user_id"])

    op.create_table(
        "quiz_topics",
        sa.Column("quiz_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("quizzes.id"), primary_key=True),
        sa.Column("topic_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("topics.id"), primary_key=True),
    )

    op.create_table(
        "questions",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("quiz_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("quizzes.id"), nullable=False),
        sa.Column("topic_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("topics.id"), nullable=False),
        sa.Column("prompt", sa.Text(), nullable=False),
        sa.Column("options", postgresql.JSONB(), nullable=False),
        sa.Column("correct_option_index", sa.Integer(), nullable=False),
        sa.Column("source_chunk_ids", postgresql.JSONB(), nullable=False),
        sa.Column("time_limit_seconds", sa.Integer(), nullable=False, server_default="15"),
        sa.Column("order_index", sa.Integer(), nullable=False, server_default="0"),
    )
    op.create_index("ix_questions_quiz_id", "questions", ["quiz_id"])
    op.create_index("ix_questions_topic_id", "questions", ["topic_id"])

    op.create_table(
        "quiz_attempts",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("quiz_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("quizzes.id"), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("started_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("final_score", sa.Float(), nullable=True),
        sa.Column("passed", sa.Boolean(), nullable=True),
    )
    op.create_index("ix_quiz_attempts_quiz_id", "quiz_attempts", ["quiz_id"])
    op.create_index("ix_quiz_attempts_user_id", "quiz_attempts", ["user_id"])

    op.create_table(
        "quiz_answers",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column(
            "quiz_attempt_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("quiz_attempts.id"), nullable=False
        ),
        sa.Column("question_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("questions.id"), nullable=False),
        sa.Column("selected_option_index", sa.Integer(), nullable=True),
        sa.Column("is_correct", sa.Boolean(), nullable=False, server_default="false"),
        sa.Column("answered_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("time_taken_seconds", sa.Float(), nullable=True),
    )
    op.create_index("ix_quiz_answers_quiz_attempt_id", "quiz_answers", ["quiz_attempt_id"])

    # Phase 6 (AI-RAG): attribute existing document_chunks to a Topic so retrieval
    # can scope precisely (docs/AI-RAG.md). Nullable — Phase 4 processing may not
    # always attribute a chunk to a single topic.
    op.add_column(
        "document_chunks",
        sa.Column("topic_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("topics.id"), nullable=True),
    )
    op.create_index("ix_document_chunks_topic_id", "document_chunks", ["topic_id"])


def downgrade() -> None:
    op.drop_index("ix_document_chunks_topic_id", table_name="document_chunks")
    op.drop_column("document_chunks", "topic_id")
    op.drop_table("quiz_answers")
    op.drop_table("quiz_attempts")
    op.drop_table("questions")
    op.drop_table("quiz_topics")
    op.drop_table("quizzes")
