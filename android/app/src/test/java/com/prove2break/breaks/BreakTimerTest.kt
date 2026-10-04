package com.prove2break.breaks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakTimerTest {

    @Test
    fun `break just started has zero elapsed and full remaining`() {
        val startedAt = 1_000_000L
        val snapshot = BreakTimer.computeSnapshot(
            now = startedAt,
            activeStartedAtEpochMs = startedAt,
            plannedLengthMillis = 5 * 60_000L,
            extendedMillis = 0,
        )
        assertEquals(0L, snapshot.elapsedMillis)
        assertEquals(5 * 60_000L, snapshot.remainingMillis)
        assertFalse(snapshot.isComplete)
    }

    @Test
    fun `extension time adds to the total allowed length`() {
        val startedAt = 1_000_000L
        val now = startedAt + 6 * 60_000L
        val snapshot = BreakTimer.computeSnapshot(
            now = now,
            activeStartedAtEpochMs = startedAt,
            plannedLengthMillis = 5 * 60_000L,
            extendedMillis = 5 * 60_000L,
        )
        // 6 minutes elapsed, but 5+5=10 minute total allowance, so not complete yet.
        assertFalse(snapshot.isComplete)
        assertEquals(4 * 60_000L, snapshot.remainingMillis)
    }

    @Test
    fun `exactly at total length boundary is complete`() {
        val startedAt = 1_000_000L
        val now = startedAt + 5 * 60_000L
        val snapshot = BreakTimer.computeSnapshot(
            now = now,
            activeStartedAtEpochMs = startedAt,
            plannedLengthMillis = 5 * 60_000L,
            extendedMillis = 0,
        )
        assertTrue(snapshot.isComplete)
        assertEquals(0L, snapshot.remainingMillis)
    }
}
