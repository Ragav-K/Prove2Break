package com.prove2break.focus

import com.prove2break.domain.model.FocusSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Thorough coverage of the timestamp-derived timer math — this is the most
 * important testable piece of the Focus Engine (PROCESS.md Section 2).
 * Covers: not started, mid-session, paused, exactly-at-boundary, overrun.
 */
class FocusTimerTest {

    private val plannedDuration = 60 * 60 * 1000L // 1 hour

    @Test
    fun `session just started has zero elapsed and full remaining`() {
        val startedAt = 1_000_000L
        val snapshot = FocusTimer.computeSnapshot(
            now = startedAt,
            startedAtEpochMs = startedAt,
            plannedDurationMillis = plannedDuration,
            totalPausedMillis = 0,
            pausedAtEpochMs = null,
            status = FocusSessionStatus.FOCUSING,
        )

        assertEquals(0L, snapshot.elapsedMillis)
        assertEquals(plannedDuration, snapshot.remainingMillis)
        assertFalse(snapshot.isOverrun)
        assertFalse(snapshot.isComplete)
    }

    @Test
    fun `mid-session elapsed and remaining derive correctly from now`() {
        val startedAt = 1_000_000L
        val tenMinutes = 10 * 60 * 1000L
        val now = startedAt + tenMinutes

        val snapshot = FocusTimer.computeSnapshot(
            now = now,
            startedAtEpochMs = startedAt,
            plannedDurationMillis = plannedDuration,
            totalPausedMillis = 0,
            pausedAtEpochMs = null,
            status = FocusSessionStatus.FOCUSING,
        )

        assertEquals(tenMinutes, snapshot.elapsedMillis)
        assertEquals(plannedDuration - tenMinutes, snapshot.remainingMillis)
        assertFalse(snapshot.isComplete)
    }

    @Test
    fun `mid-session elapsed subtracts previously completed pauses`() {
        val startedAt = 1_000_000L
        val twentyMinutes = 20 * 60 * 1000L
        val fiveMinutesPaused = 5 * 60 * 1000L
        val now = startedAt + twentyMinutes

        val snapshot = FocusTimer.computeSnapshot(
            now = now,
            startedAtEpochMs = startedAt,
            plannedDurationMillis = plannedDuration,
            totalPausedMillis = fiveMinutesPaused,
            pausedAtEpochMs = null,
            status = FocusSessionStatus.FOCUSING,
        )

        // 20 minutes of wall-clock time passed, but 5 of it was a completed pause.
        assertEquals(twentyMinutes - fiveMinutesPaused, snapshot.elapsedMillis)
    }

    @Test
    fun `paused session freezes elapsed at the moment it was paused`() {
        val startedAt = 1_000_000L
        val tenMinutes = 10 * 60 * 1000L
        val pausedAt = startedAt + tenMinutes
        // Wall clock keeps moving forward long after the pause began...
        val now = pausedAt + 30 * 60 * 1000L

        val snapshot = FocusTimer.computeSnapshot(
            now = now,
            startedAtEpochMs = startedAt,
            plannedDurationMillis = plannedDuration,
            totalPausedMillis = 0,
            pausedAtEpochMs = pausedAt,
            status = FocusSessionStatus.PAUSED,
        )

        // ...but elapsed time must stay pinned at 10 minutes, not 40.
        assertEquals(tenMinutes, snapshot.elapsedMillis)
        assertEquals(plannedDuration - tenMinutes, snapshot.remainingMillis)
        assertFalse(snapshot.isComplete)
    }

    @Test
    fun `exactly at the planned-duration boundary is complete with zero remaining`() {
        val startedAt = 1_000_000L
        val now = startedAt + plannedDuration

        val snapshot = FocusTimer.computeSnapshot(
            now = now,
            startedAtEpochMs = startedAt,
            plannedDurationMillis = plannedDuration,
            totalPausedMillis = 0,
            pausedAtEpochMs = null,
            status = FocusSessionStatus.FOCUSING,
        )

        assertEquals(plannedDuration, snapshot.elapsedMillis)
        assertEquals(0L, snapshot.remainingMillis)
        assertFalse(snapshot.isOverrun)
        assertTrue(snapshot.isComplete)
    }

    @Test
    fun `overrun past the planned duration clamps remaining to zero but flags overrun`() {
        val startedAt = 1_000_000L
        val tenMinutesPast = 10 * 60 * 1000L
        val now = startedAt + plannedDuration + tenMinutesPast

        val snapshot = FocusTimer.computeSnapshot(
            now = now,
            startedAtEpochMs = startedAt,
            plannedDurationMillis = plannedDuration,
            totalPausedMillis = 0,
            pausedAtEpochMs = null,
            status = FocusSessionStatus.FOCUSING,
        )

        assertEquals(plannedDuration + tenMinutesPast, snapshot.elapsedMillis)
        assertEquals(0L, snapshot.remainingMillis)
        assertTrue(snapshot.isOverrun)
        assertTrue(snapshot.isComplete)
    }

    @Test
    fun `completed status short-circuits to a pinned, fully-elapsed snapshot`() {
        val startedAt = 1_000_000L
        // "now" is irrelevant once COMPLETED — even if it looks like the
        // session barely started, the snapshot must reflect completion.
        val now = startedAt + 5000L

        val snapshot = FocusTimer.computeSnapshot(
            now = now,
            startedAtEpochMs = startedAt,
            plannedDurationMillis = plannedDuration,
            totalPausedMillis = 0,
            pausedAtEpochMs = null,
            status = FocusSessionStatus.COMPLETED,
        )

        assertEquals(plannedDuration, snapshot.elapsedMillis)
        assertEquals(0L, snapshot.remainingMillis)
        assertFalse(snapshot.isOverrun)
        assertTrue(snapshot.isComplete)
    }

    @Test
    fun `recovery after process death recomputes correctly from stale timestamps`() {
        // Simulates: session started, process died while FOCUSING, app relaunches
        // much later and reads the same Room row — must recompute (not reset).
        val startedAt = 1_000_000L
        val fortyFiveMinutes = 45 * 60 * 1000L
        val now = startedAt + fortyFiveMinutes

        val snapshot = FocusTimer.computeSnapshot(
            now = now,
            startedAtEpochMs = startedAt,
            plannedDurationMillis = plannedDuration,
            totalPausedMillis = 0,
            pausedAtEpochMs = null,
            status = FocusSessionStatus.FOCUSING,
        )

        assertEquals(fortyFiveMinutes, snapshot.elapsedMillis)
        assertEquals(plannedDuration - fortyFiveMinutes, snapshot.remainingMillis)
        assertFalse(snapshot.isComplete)
    }
}
