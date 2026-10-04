package com.prove2break.data.repository

import com.prove2break.breaks.BreakExtension
import com.prove2break.breaks.BreakRecommendation
import com.prove2break.data.local.BreakBudgetDao
import com.prove2break.data.local.BreakBudgetEntity
import com.prove2break.data.local.ScheduledBreakDao
import com.prove2break.data.local.ScheduledBreakEntity
import com.prove2break.domain.model.BreakBudget
import com.prove2break.domain.model.ScheduledBreak
import com.prove2break.domain.model.ScheduledBreakStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BreakRepositoryImpl @Inject constructor(
    private val breakDao: ScheduledBreakDao,
    private val budgetDao: BreakBudgetDao,
) : BreakRepository {

    override suspend fun createSchedule(
        sessionId: Long,
        breaks: List<BreakRecommendation.RecommendedBreak>,
        totalExtensionBudgetMillis: Long,
    ) {
        breakDao.insertAll(
            breaks.map { recommended ->
                ScheduledBreakEntity(
                    sessionId = sessionId,
                    offsetMillis = recommended.offsetMillis,
                    plannedLengthMillis = recommended.lengthMillis,
                )
            },
        )
        budgetDao.insert(
            BreakBudgetEntity(
                sessionId = sessionId,
                totalBudgetMillis = totalExtensionBudgetMillis,
            ),
        )
    }

    override suspend fun getScheduledBreaks(sessionId: Long): List<ScheduledBreak> {
        return breakDao.getForSession(sessionId).map { it.toDomain() }
    }

    override fun observeScheduledBreaks(sessionId: Long): Flow<List<ScheduledBreak>> {
        return breakDao.observeForSession(sessionId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun startBreak(breakId: Long, nowEpochMs: Long) {
        val entity = breakDao.getById(breakId) ?: return
        if (entity.status != ScheduledBreakStatus.PENDING.name) return
        breakDao.update(
            entity.copy(
                status = ScheduledBreakStatus.ACTIVE.name,
                activeStartedAtEpochMs = nowEpochMs,
            ),
        )
    }

    override suspend fun completeBreak(breakId: Long) {
        val entity = breakDao.getById(breakId) ?: return
        if (entity.status == ScheduledBreakStatus.TAKEN.name) return
        breakDao.update(entity.copy(status = ScheduledBreakStatus.TAKEN.name))
    }

    override suspend fun extendBreak(breakId: Long, incrementMillis: Long): Boolean {
        val entity = breakDao.getById(breakId) ?: return false
        val budget = budgetDao.getForSession(entity.sessionId) ?: return false
        val result = BreakExtension.requestExtension(
            currentUsedMillis = budget.usedMillis,
            totalBudgetMillis = budget.totalBudgetMillis,
            incrementMillis = incrementMillis,
        )
        if (!result.allowed) return false
        budgetDao.update(budget.copy(usedMillis = result.newUsedMillis))
        breakDao.update(entity.copy(extendedMillis = entity.extendedMillis + incrementMillis))
        return true
    }

    override suspend fun getBudget(sessionId: Long): BreakBudget? {
        return budgetDao.getForSession(sessionId)?.toDomain()
    }

    override suspend fun createUnscheduledBreak(sessionId: Long, plannedLengthMillis: Long, nowEpochMs: Long): Long {
        return breakDao.insert(
            ScheduledBreakEntity(
                sessionId = sessionId,
                offsetMillis = 0,
                plannedLengthMillis = plannedLengthMillis,
                status = ScheduledBreakStatus.ACTIVE.name,
                activeStartedAtEpochMs = nowEpochMs,
                isUnscheduled = true,
            ),
        )
    }

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

    private fun BreakBudgetEntity.toDomain() = BreakBudget(
        sessionId = sessionId,
        totalBudgetMillis = totalBudgetMillis,
        usedMillis = usedMillis,
    )
}
