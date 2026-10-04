package com.prove2break.data.repository

import com.prove2break.data.local.FocusSessionDao
import com.prove2break.data.local.FocusSessionEntity
import com.prove2break.data.local.ScheduledBreakDao
import com.prove2break.data.local.ScheduledBreakEntity
import com.prove2break.domain.model.FocusSession
import com.prove2break.domain.model.FocusSessionStatus
import com.prove2break.domain.model.LocalStudyStats
import com.prove2break.domain.model.ScheduledBreak
import com.prove2break.domain.model.ScheduledBreakStatus
import com.prove2break.domain.usecase.GetLocalStudyStatsUseCase
import javax.inject.Inject

class LocalAnalyticsRepositoryImpl @Inject constructor(
    private val focusSessionDao: FocusSessionDao,
    private val scheduledBreakDao: ScheduledBreakDao,
    private val getLocalStudyStats: GetLocalStudyStatsUseCase,
) : LocalAnalyticsRepository {

    override suspend fun getLocalStudyStats(): LocalStudyStats {
        val sessions = focusSessionDao.getAll().map { it.toDomain() }
        val breaks = scheduledBreakDao.getAll().map { it.toDomain() }
        return getLocalStudyStats(sessions, breaks)
    }

    private fun FocusSessionEntity.toDomain() = FocusSession(
        id = id,
        subjectId = subjectId,
        startedAtEpochMs = startedAtEpochMs,
        plannedDurationMillis = plannedDurationMillis,
        pausedAtEpochMs = pausedAtEpochMs,
        totalPausedMillis = totalPausedMillis,
        status = runCatching { FocusSessionStatus.valueOf(status) }.getOrDefault(FocusSessionStatus.FOCUSING),
        endedAtEpochMs = endedAtEpochMs,
        isActive = isActive,
    )

    private fun ScheduledBreakEntity.toDomain() = ScheduledBreak(
        id = id,
        sessionId = sessionId,
        offsetMillis = offsetMillis,
        plannedLengthMillis = plannedLengthMillis,
        status = runCatching { ScheduledBreakStatus.valueOf(status) }.getOrDefault(ScheduledBreakStatus.PENDING),
        activeStartedAtEpochMs = activeStartedAtEpochMs,
        extendedMillis = extendedMillis,
        isUnscheduled = isUnscheduled,
    )
}
