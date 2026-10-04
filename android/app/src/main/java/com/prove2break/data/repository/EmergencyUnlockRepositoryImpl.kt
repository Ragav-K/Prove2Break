package com.prove2break.data.repository

import com.prove2break.data.local.EmergencyUnlockDao
import com.prove2break.data.local.EmergencyUnlockEntity
import com.prove2break.domain.model.EmergencyUnlock
import com.prove2break.domain.model.EmergencyUnlockReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EmergencyUnlockRepositoryImpl @Inject constructor(
    private val dao: EmergencyUnlockDao,
) : EmergencyUnlockRepository {

    override suspend fun grant(
        packageName: String,
        reason: EmergencyUnlockReason,
        durationMillis: Long,
        focusSessionId: Long,
        nowEpochMs: Long,
    ): Long {
        return dao.insert(
            EmergencyUnlockEntity(
                packageName = packageName,
                reason = reason.name,
                timestampMillis = nowEpochMs,
                durationMillis = durationMillis,
                focusSessionId = focusSessionId,
            ),
        )
    }

    override fun observeForSession(sessionId: Long): Flow<List<EmergencyUnlock>> =
        dao.observeForSession(sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun getForSession(sessionId: Long): List<EmergencyUnlock> =
        dao.getForSession(sessionId).map { it.toDomain() }

    private fun EmergencyUnlockEntity.toDomain() = EmergencyUnlock(
        id = id,
        packageName = packageName,
        reason = runCatching { EmergencyUnlockReason.valueOf(reason) }.getOrDefault(EmergencyUnlockReason.OTHER),
        timestampMillis = timestampMillis,
        durationMillis = durationMillis,
        focusSessionId = focusSessionId,
    )
}
