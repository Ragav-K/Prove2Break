package com.prove2break.domain.usecase

import com.prove2break.domain.model.FocusSession
import com.prove2break.domain.model.FocusSessionStatus
import com.prove2break.domain.model.LocalStudyStats
import com.prove2break.domain.model.ScheduledBreak
import com.prove2break.domain.model.ScheduledBreakStatus
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Pure aggregation of already-fetched Room data into [LocalStudyStats]
 * (docs/ANALYTICS.md "Metrics Tracked" — study time, sessions, breaks,
 * streak). Kept as a plain function over domain lists (no repository/DB
 * dependency) so the math is unit-testable without Room/Robolectric.
 */
class GetLocalStudyStatsUseCase @Inject constructor() {

    operator fun invoke(sessions: List<FocusSession>, breaks: List<ScheduledBreak>): LocalStudyStats {
        val endedSessions = sessions.filter { it.endedAtEpochMs != null }

        val totalStudyTimeMillis = endedSessions.sumOf { session ->
            sessionDurationMillis(session).coerceAtLeast(0)
        }

        val completedCount = endedSessions.count { session ->
            session.status == FocusSessionStatus.COMPLETED &&
                sessionDurationMillis(session) >= session.plannedDurationMillis
        }
        val endedEarlyCount = endedSessions.size - completedCount

        val takenBreaks = breaks.filter { it.status == ScheduledBreakStatus.TAKEN }
        val scheduledTaken = takenBreaks.count { !it.isUnscheduled }
        val unscheduledTaken = takenBreaks.count { it.isUnscheduled }

        val averageBreakLengthMillis = if (takenBreaks.isNotEmpty()) {
            takenBreaks.sumOf { it.plannedLengthMillis + it.extendedMillis } / takenBreaks.size
        } else {
            0L
        }

        val extendedBreaks = takenBreaks.filter { it.extendedMillis > 0 }
        val averageExtensionMillis = if (extendedBreaks.isNotEmpty()) {
            extendedBreaks.sumOf { it.extendedMillis } / extendedBreaks.size
        } else {
            0L
        }

        return LocalStudyStats(
            totalStudyTimeMillis = totalStudyTimeMillis,
            sessionCount = sessions.size,
            completedSessionCount = completedCount,
            endedEarlyCount = endedEarlyCount,
            currentStreakDays = currentStreakDays(endedSessions),
            scheduledBreaksTaken = scheduledTaken,
            unscheduledBreaksTaken = unscheduledTaken,
            averageBreakLengthMillis = averageBreakLengthMillis,
            averageExtensionMillis = averageExtensionMillis,
        )
    }

    private fun sessionDurationMillis(session: FocusSession): Long {
        val endedAt = session.endedAtEpochMs ?: return 0
        return (endedAt - session.startedAtEpochMs) - session.totalPausedMillis
    }

    /** Consecutive-day streak of study, counting back from today (UTC calendar days). */
    private fun currentStreakDays(endedSessions: List<FocusSession>): Int {
        if (endedSessions.isEmpty()) return 0
        val studyDays = endedSessions.map { dayKey(it.startedAtEpochMs) }.toSet()

        var streak = 0
        var cursor = dayKey(System.currentTimeMillis())
        while (studyDays.contains(cursor)) {
            streak++
            cursor = cursor - 1
        }
        return streak
    }

    /** Number of whole days since epoch, in UTC — a stable, comparable "day key". */
    private fun dayKey(epochMs: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = epochMs
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis / DAY_MILLIS
    }

    companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
