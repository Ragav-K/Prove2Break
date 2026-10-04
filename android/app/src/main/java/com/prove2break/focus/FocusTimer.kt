package com.prove2break.focus

import com.prove2break.domain.model.FocusSessionStatus

/**
 * A single point-in-time read of a focus session's derived timing.
 * Never stored — always recomputed from timestamps (FOCUS-ENGINE.md).
 */
data class TimerSnapshot(
    val elapsedMillis: Long,
    val remainingMillis: Long,
    val isOverrun: Boolean,
    val isComplete: Boolean,
)

/**
 * Pure, stateless derivation of focus-session timing from timestamps.
 *
 * Binding decision (FOCUS-ENGINE.md / PROCESS.md Section 6): the timer is
 * NEVER a decrementing in-memory counter. Every read recomputes
 * elapsed/remaining duration from (now, startedAt, plannedDuration,
 * totalPausedMillis, pausedAtMillis) — so it is immune to backgrounding,
 * process death, or the UI simply not running for a while.
 */
object FocusTimer {

    /**
     * @param now current wall-clock time (epoch millis).
     * @param startedAtEpochMs when the session was started.
     * @param plannedDurationMillis the fixed planned duration, set at session start.
     * @param totalPausedMillis accumulated duration of all *completed* pauses.
     * @param pausedAtEpochMs non-null only while [status] is PAUSED — the moment
     *   the current (still-open) pause began. While set, elapsed time is frozen
     *   as of this timestamp rather than advancing with [now].
     * @param status the session's current status. COMPLETED short-circuits to a
     *   snapshot pinned at the planned duration.
     */
    fun computeSnapshot(
        now: Long,
        startedAtEpochMs: Long,
        plannedDurationMillis: Long,
        totalPausedMillis: Long,
        pausedAtEpochMs: Long?,
        status: FocusSessionStatus,
    ): TimerSnapshot {
        if (status == FocusSessionStatus.COMPLETED) {
            return TimerSnapshot(
                elapsedMillis = plannedDurationMillis,
                remainingMillis = 0,
                isOverrun = false,
                isComplete = true,
            )
        }

        // While paused, "now" for elapsed-time purposes is frozen at the moment
        // the pause began — the clock does not advance again until resumed.
        val effectiveNow = if (status == FocusSessionStatus.PAUSED && pausedAtEpochMs != null) {
            pausedAtEpochMs
        } else {
            now
        }

        val rawElapsed = (effectiveNow - startedAtEpochMs - totalPausedMillis).coerceAtLeast(0)
        val remaining = plannedDurationMillis - rawElapsed

        return TimerSnapshot(
            elapsedMillis = rawElapsed,
            remainingMillis = remaining.coerceAtLeast(0),
            isOverrun = remaining < 0,
            isComplete = remaining <= 0,
        )
    }
}
