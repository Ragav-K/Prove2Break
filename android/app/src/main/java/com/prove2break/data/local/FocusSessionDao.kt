package com.prove2break.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE isActive = 1 LIMIT 1")
    fun observeActiveSession(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FocusSessionEntity?

    /** Full history, newest first — used by local analytics aggregation (docs/ANALYTICS.md). */
    @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMs DESC")
    suspend fun getAll(): List<FocusSessionEntity>
}
