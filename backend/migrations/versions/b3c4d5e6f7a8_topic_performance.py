"""Phase 7 — Analytics: topic_performance

Revision ID: b3c4d5e6f7a8
Revises: 9a1b3e7c5f2d
Create Date: 2026-09-30 00:00:00.000000
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = "b3c4d5e6f7a8"
down_revision = "9a1b3e7c5f2d"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "topic_performance",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True, nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("topic_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("topics.id"), nullable=False),
        sa.Column("attempt_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("average_score", sa.Float(), nullable=False, server_default="0"),
        sa.Column("last_attempt_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("user_id", "topic_id", name="uq_topic_performance_user_topic"),
    )
    op.create_index("ix_topic_performance_user_id", "topic_performance", ["user_id"])
    op.create_index("ix_topic_performance_topic_id", "topic_performance", ["topic_id"])


def downgrade() -> None:
    op.drop_index("ix_topic_performance_topic_id", table_name="topic_performance")
    op.drop_index("ix_topic_performance_user_id", table_name="topic_performance")
    op.drop_table("topic_performance")
