package com.prove2break.data.repository

import com.prove2break.data.local.FocusSessionDao
import com.prove2break.data.local.FocusSessionEntity
import com.prove2break.domain.model.FocusSession
import com.prove2break.domain.model.FocusSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FocusSessionRepositoryImpl @Inject constructor(
    private val dao: FocusSessionDao,
) : FocusSessionRepository {

    override suspend fun startSession(subjectId: String, plannedDurationMillis: Long): Long {
        return dao.insert(
            FocusSessionEntity(
                subjectId = subjectId,
                startedAtEpochMs = System.currentTimeMillis(),
                plannedDurationMillis = plannedDurationMillis,
                status = FocusSessionStatus.FOCUSING.name,
            ),
        )
    }

    override suspend fun getActiveSession(): FocusSession? {
        return dao.getActiveSession()?.toDomain()
    }

    override fun observeActiveSession(): Flow<FocusSession?> {
        return dao.observeActiveSession().map { it?.toDomain() }
    }

    override fun observeAllSessions(): Flow<List<FocusSession>> {
        return dao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun pauseSession(sessionId: Long, nowEpochMs: Long) {
        val entity = dao.getById(sessionId) ?: return
        if (entity.status != FocusSessionStatus.FOCUSING.name) return
        dao.update(
            entity.copy(
                status = FocusSessionStatus.PAUSED.name,
                pausedAtEpochMs = nowEpochMs,
            ),
        )
    }

    override suspend fun resumeSession(sessionId: Long, nowEpochMs: Long) {
        val entity = dao.getById(sessionId) ?: return
        if (entity.status != FocusSessionStatus.PAUSED.name) return
        val pausedAt = entity.pausedAtEpochMs ?: nowEpochMs
        val pauseDuration = (nowEpochMs - pausedAt).coerceAtLeast(0)
        dao.update(
            entity.copy(
                status = FocusSessionStatus.FOCUSING.name,
                pausedAtEpochMs = null,
                totalPausedMillis = entity.totalPausedMillis + pauseDuration,
            ),
        )
    }

    override suspend fun setStatus(sessionId: Long, status: FocusSessionStatus) {
        val entity = dao.getById(sessionId) ?: return
        dao.update(entity.copy(status = status.name))
    }

    override suspend fun completeSession(sessionId: Long, nowEpochMs: Long) {
        val entity = dao.getById(sessionId) ?: return
        if (entity.status == FocusSessionStatus.COMPLETED.name) return
        dao.update(
            entity.copy(
                status = FocusSessionStatus.COMPLETED.name,
                endedAtEpochMs = nowEpochMs,
                isActive = false,
            ),
        )
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
}
