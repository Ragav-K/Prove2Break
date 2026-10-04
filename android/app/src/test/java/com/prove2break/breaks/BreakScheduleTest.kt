package com.prove2break.breaks

import com.prove2break.domain.model.ScheduledBreak
import com.prove2break.domain.model.ScheduledBreakStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Timestamp-boundary coverage for [BreakSchedule.currentAvailableBreak]:
 * exactly at trigger time, just before, just after, and the "continue
 * studying never consumes the break" re-offerability rule.
 */
class BreakScheduleTest {

    private fun pendingBreak(id: Long, offsetMillis: Long) = ScheduledBreak(
        id = id,
        sessionId = 1L,
        offsetMillis = offsetMillis,
        plannedLengthMillis = 5 * 60_000L,
        status = ScheduledBreakStatus.PENDING,
        activeStartedAtEpochMs = null,
        extendedMillis = 0,
        isUnscheduled = false,
    )

    @Test
    fun `just before offset the break is not yet available`() {
        val breaks = listOf(pendingBreak(1, offsetMillis = 50 * 60_000L))
        val result = BreakSchedule.currentAvailableBreak(elapsedMillis = 50 * 60_000L - 1, breaks = breaks)
        assertNull(result)
    }

    @Test
    fun `exactly at offset the break becomes available`() {
        val breaks = listOf(pendingBreak(1, offsetMillis = 50 * 60_000L))
        val result = BreakSchedule.currentAvailableBreak(elapsedMillis = 50 * 60_000L, breaks = breaks)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `just after offset the break stays available`() {
        val breaks = listOf(pendingBreak(1, offsetMillis = 50 * 60_000L))
        val result = BreakSchedule.currentAvailableBreak(elapsedMillis = 50 * 60_000L + 1, breaks = breaks)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `a break already ACTIVE or TAKEN is never offered again as available`() {
        val active = pendingBreak(1, offsetMillis = 10_000L).copy(status = ScheduledBreakStatus.ACTIVE)
        val taken = pendingBreak(2, offsetMillis = 20_000L).copy(status = ScheduledBreakStatus.TAKEN)
        val result = BreakSchedule.currentAvailableBreak(elapsedMillis = 1_000_000L, breaks = listOf(active, taken))
        assertNull(result)
    }

    @Test
    fun `earliest due PENDING break wins when multiple are due`() {
        val breaks = listOf(
            pendingBreak(1, offsetMillis = 50 * 60_000L),
            pendingBreak(2, offsetMillis = 110 * 60_000L),
        )
        val result = BreakSchedule.currentAvailableBreak(elapsedMillis = 150 * 60_000L, breaks = breaks)
        assertEquals(1L, result?.id)
    }

    @Test
    fun `deferred break stays PENDING and keeps being offered on later recomputation`() {
        // Simulates "continue studying": the break entity itself never
        // changes status, so recomputing later with more elapsed time still
        // returns it as available — it was never consumed.
        val breaks = listOf(pendingBreak(1, offsetMillis = 50 * 60_000L))

        val firstCheck = BreakSchedule.currentAvailableBreak(elapsedMillis = 51 * 60_000L, breaks = breaks)
        val laterCheck = BreakSchedule.currentAvailableBreak(elapsedMillis = 70 * 60_000L, breaks = breaks)

        assertEquals(1L, firstCheck?.id)
        assertEquals(1L, laterCheck?.id)
    }
}
