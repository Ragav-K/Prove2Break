package com.prove2break.domain.model

/**
 * Domain model for a focus session. Timestamp-authoritative per FOCUS-ENGINE.md:
 * elapsed/remaining duration are never stored, only derived (see
 * com.prove2break.focus.FocusTimer) from [startedAtEpochMs], [plannedDurationMillis],
 * [totalPausedMillis] and [pausedAtEpochMs] against the current clock.
 */
data class FocusSession(
    val id: Long,
    val subjectId: String,
    val startedAtEpochMs: Long,
    val plannedDurationMillis: Long,
    val pausedAtEpochMs: Long?,
    val totalPausedMillis: Long,
    val status: FocusSessionStatus,
    val endedAtEpochMs: Long?,
    val isActive: Boolean,
)
