package com.prove2break.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prove2break.domain.model.FocusSessionStatus

/**
 * Authoritative local record for an in-progress (or completed) focus session.
 * See ANDROID-ARCHITECTURE.md / FOCUS-ENGINE.md — written on session start,
 * read back by the foreground service and ViewModel on process restart,
 * updated on every state transition.
 *
 * Timestamp-authoritative design (FOCUS-ENGINE.md, PROCESS.md Section 6):
 * [startedAtEpochMs] + [plannedDurationMillis] + [totalPausedMillis] +
 * [pausedAtEpochMs] are the only facts stored; elapsed/remaining duration are
 * always derived from these plus the current clock, never persisted as a
 * counter.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Backend subject id (UUID string) — subjects are server-owned resources
    // (Phase 4 Study Management), not local Room rows, so this stores the
    // remote SubjectResponse.id verbatim rather than a local foreign key.
    val subjectId: String,
    val startedAtEpochMs: Long,
    val plannedDurationMillis: Long,
    val pausedAtEpochMs: Long? = null,
    val totalPausedMillis: Long = 0,
    val status: String = FocusSessionStatus.FOCUSING.name,
    val endedAtEpochMs: Long? = null,
    val isActive: Boolean = true,
)
