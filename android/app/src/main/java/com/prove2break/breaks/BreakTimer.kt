package com.prove2break.breaks

/** A single point-in-time read of an active break's derived timing. Never stored. */
data class BreakTimerSnapshot(
    val elapsedMillis: Long,
    val remainingMillis: Long,
    val isComplete: Boolean,
)

/**
 * Pure, stateless derivation of an active break's timing, mirroring
 * [com.prove2break.focus.FocusTimer]'s "always recompute from timestamps"
 * design. Breaks are not pausable, so this is simpler than the focus timer:
 * elapsed is always `now - startedAt`, and the total allowed length is the
 * break's planned length plus whatever extension time has been granted.
 */
object BreakTimer {
    fun computeSnapshot(
        now: Long,
        activeStartedAtEpochMs: Long,
        plannedLengthMillis: Long,
        extendedMillis: Long,
    ): BreakTimerSnapshot {
        val totalLength = plannedLengthMillis + extendedMillis
        val elapsed = (now - activeStartedAtEpochMs).coerceAtLeast(0)
        val remaining = (totalLength - elapsed).coerceAtLeast(0)
        return BreakTimerSnapshot(
            elapsedMillis = elapsed,
            remainingMillis = remaining,
            isComplete = elapsed >= totalLength,
        )
    }
}
