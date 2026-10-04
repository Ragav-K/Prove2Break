package com.prove2break.data.repository

import com.prove2break.domain.model.EmergencyUnlock
import com.prove2break.domain.model.EmergencyUnlockReason
import kotlinx.coroutines.flow.Flow

/** Records every emergency-unlock grant; never bypasses restriction silently (APP-RESTRICTION.md). */
interface EmergencyUnlockRepository {
    suspend fun grant(
        packageName: String,
        reason: EmergencyUnlockReason,
        durationMillis: Long,
        focusSessionId: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Long

    fun observeForSession(sessionId: Long): Flow<List<EmergencyUnlock>>
    suspend fun getForSession(sessionId: Long): List<EmergencyUnlock>
}
