package com.prove2break.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyUnlockDao {
    @Insert
    suspend fun insert(unlock: EmergencyUnlockEntity): Long

    @Query("SELECT * FROM emergency_unlocks WHERE focusSessionId = :sessionId ORDER BY timestampMillis DESC")
    fun observeForSession(sessionId: Long): Flow<List<EmergencyUnlockEntity>>

    @Query("SELECT * FROM emergency_unlocks ORDER BY timestampMillis DESC")
    suspend fun getAll(): List<EmergencyUnlockEntity>

    @Query("SELECT * FROM emergency_unlocks WHERE focusSessionId = :sessionId ORDER BY timestampMillis DESC")
    suspend fun getForSession(sessionId: Long): List<EmergencyUnlockEntity>
}
