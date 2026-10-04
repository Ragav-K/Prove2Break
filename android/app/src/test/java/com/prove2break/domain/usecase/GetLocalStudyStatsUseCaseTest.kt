package com.prove2break.domain.usecase

import com.prove2break.domain.model.FocusSession
import com.prove2break.domain.model.FocusSessionStatus
import com.prove2break.domain.model.ScheduledBreak
import com.prove2break.domain.model.ScheduledBreakStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** Pins down the study-time-summing / pass-rate-adjacent aggregation math (docs/ANALYTICS.md). */
class GetLocalStudyStatsUseCaseTest {

    private val useCase = GetLocalStudyStatsUseCase()

    private fun session(
        id: Long,
        startedAtEpochMs: Long,
        plannedDurationMillis: Long,
        endedAtEpochMs: Long?,
        totalPausedMillis: Long = 0,
        status: FocusSessionStatus = FocusSessionStatus.COMPLETED,
    ) = FocusSession(
        id = id,
        subjectId = "subj-1",
        startedAtEpochMs = startedAtEpochMs,
        plannedDurationMillis = plannedDurationMillis,
        pausedAtEpochMs = null,
        totalPausedMillis = totalPausedMillis,
        status = status,
        endedAtEpochMs = endedAtEpochMs,
        isActive = false,
    )

    private fun scheduledBreak(
        id: Long,
        plannedLengthMillis: Long,
        extendedMillis: Long = 0,
        isUnscheduled: Boolean = false,
        status: ScheduledBreakStatus = ScheduledBreakStatus.TAKEN,
    ) = ScheduledBreak(
        id = id,
        sessionId = 1,
        offsetMillis = 0,
        plannedLengthMillis = plannedLengthMillis,
        status = status,
        activeStartedAtEpochMs = null,
        extendedMillis = extendedMillis,
        isUnscheduled = isUnscheduled,
    )

    @Test
    fun `sums study time across completed sessions excluding paused time`() {
        val sessions = listOf(
            session(1, startedAtEpochMs = 0, plannedDurationMillis = 60_000, endedAtEpochMs = 60_000),
            session(2, startedAtEpochMs = 100_000, plannedDurationMillis = 30_000, endedAtEpochMs = 140_000, totalPausedMillis = 10_000),
        )

        val stats = useCase(sessions, emptyList())

        // Session 1: 60_000 - 0 = 60_000. Session 2: (140_000-100_000) - 10_000 = 30_000.
        assertEquals(90_000L, stats.totalStudyTimeMillis)
        assertEquals(2, stats.sessionCount)
    }

    @Test
    fun `classifies sessions as completed vs ended early based on planned duration`() {
        val sessions = listOf(
            // Ran the full planned duration.
            session(1, startedAtEpochMs = 0, plannedDurationMillis = 60_000, endedAtEpochMs = 60_000),
            // Ended before the planned duration elapsed.
            session(2, startedAtEpochMs = 0, plannedDurationMillis = 60_000, endedAtEpochMs = 30_000),
        )

        val stats = useCase(sessions, emptyList())

        assertEquals(1, stats.completedSessionCount)
        assertEquals(1, stats.endedEarlyCount)
    }

    @Test
    fun `ignores sessions still in progress (no endedAt) for study time and completion counts`() {
        val sessions = listOf(
            session(1, startedAtEpochMs = 0, plannedDurationMillis = 60_000, endedAtEpochMs = null, status = FocusSessionStatus.FOCUSING),
        )

        val stats = useCase(sessions, emptyList())

        assertEquals(0L, stats.totalStudyTimeMillis)
        assertEquals(0, stats.completedSessionCount)
        assertEquals(0, stats.endedEarlyCount)
        assertEquals(1, stats.sessionCount) // still counted in the raw session total
    }

    @Test
    fun `counts scheduled vs unscheduled taken breaks separately and ignores pending ones`() {
        val breaks = listOf(
            scheduledBreak(1, plannedLengthMillis = 300_000, isUnscheduled = false),
            scheduledBreak(2, plannedLengthMillis = 300_000, isUnscheduled = true),
            scheduledBreak(3, plannedLengthMillis = 300_000, isUnscheduled = false, status = ScheduledBreakStatus.PENDING),
        )

        val stats = useCase(emptyList(), breaks)

        assertEquals(1, stats.scheduledBreaksTaken)
        assertEquals(1, stats.unscheduledBreaksTaken)
    }

    @Test
    fun `averages extension time only over breaks that were actually extended`() {
        val breaks = listOf(
            scheduledBreak(1, plannedLengthMillis = 300_000, extendedMillis = 60_000),
            scheduledBreak(2, plannedLengthMillis = 300_000, extendedMillis = 0),
            scheduledBreak(3, plannedLengthMillis = 300_000, extendedMillis = 120_000),
        )

        val stats = useCase(emptyList(), breaks)

        // Average over the two extended breaks only: (60_000 + 120_000) / 2 = 90_000.
        assertEquals(90_000L, stats.averageExtensionMillis)
        // Average break length (planned + extension) over all three taken breaks: sum(360k,300k,420k)/3.
        assertEquals((360_000L + 300_000L + 420_000L) / 3, stats.averageBreakLengthMillis)
    }

    @Test
    fun `empty history yields zeroed stats`() {
        val stats = useCase(emptyList(), emptyList())
        assertEquals(com.prove2break.domain.model.LocalStudyStats.EMPTY.copy(), stats)
    }

    @Test
    fun `current streak counts consecutive days of study back from today`() {
        val utc = TimeZone.getTimeZone("UTC")
        val calendar = Calendar.getInstance(utc)
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val today = calendar.timeInMillis
        val oneDayMs = 24L * 60 * 60 * 1000

        val sessions = listOf(
            session(1, startedAtEpochMs = today, plannedDurationMillis = 60_000, endedAtEpochMs = today + 60_000),
            session(2, startedAtEpochMs = today - oneDayMs, plannedDurationMillis = 60_000, endedAtEpochMs = today - oneDayMs + 60_000),
            // Gap of one day here (today - 2*oneDayMs is skipped).
            session(3, startedAtEpochMs = today - 3 * oneDayMs, plannedDurationMillis = 60_000, endedAtEpochMs = today - 3 * oneDayMs + 60_000),
        )

        val stats = useCase(sessions, emptyList())

        assertEquals(2, stats.currentStreakDays)
    }
}
