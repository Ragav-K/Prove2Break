package com.prove2break.domain.model

/**
 * Aggregate study/break stats computed entirely from local Room data
 * (FocusSession/ScheduledBreak) — never synced to the backend, per the
 * Phase 2/5 "Android-local-only" decision documented in docs/PROCESS.md
 * ("Data split for analytics"). See docs/ANALYTICS.md "Metrics Tracked".
 */
data class LocalStudyStats(
    /** Sum of completed/ended-early session durations, excluding break time. */
    val totalStudyTimeMillis: Long,
    val sessionCount: Int,
    val completedSessionCount: Int,
    val endedEarlyCount: Int,
    val currentStreakDays: Int,
    val scheduledBreaksTaken: Int,
    val unscheduledBreaksTaken: Int,
    val averageBreakLengthMillis: Long,
    val averageExtensionMillis: Long,
) {
    companion object {
        val EMPTY = LocalStudyStats(
            totalStudyTimeMillis = 0,
            sessionCount = 0,
            completedSessionCount = 0,
            endedEarlyCount = 0,
            currentStreakDays = 0,
            scheduledBreaksTaken = 0,
            unscheduledBreaksTaken = 0,
            averageBreakLengthMillis = 0,
            averageExtensionMillis = 0,
        )
    }
}
