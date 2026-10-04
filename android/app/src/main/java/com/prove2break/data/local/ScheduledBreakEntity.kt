package com.prove2break.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.prove2break.domain.model.ScheduledBreakStatus

/**
 * A persisted scheduled (or unscheduled, see [isUnscheduled]) break occurrence
 * for a focus session. See BREAK-SYSTEM.md and [com.prove2break.breaks.BreakSchedule].
 *
 * Timestamp-authoritative like [FocusSessionEntity]: [activeStartedAtEpochMs]
 * plus [plannedLengthMillis] and [extendedMillis] are the only stored facts —
 * elapsed/remaining break time is always derived (see
 * [com.prove2break.breaks.BreakTimer]), never persisted as a counter.
 */
@Entity(tableName = "scheduled_breaks")
data class ScheduledBreakEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val offsetMillis: Long,
    val plannedLengthMillis: Long,
    val status: String = ScheduledBreakStatus.PENDING.name,
    val activeStartedAtEpochMs: Long? = null,
    val extendedMillis: Long = 0,
    val isUnscheduled: Boolean = false,
)
