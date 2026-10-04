package com.prove2break.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledBreakDao {
    @Insert
    suspend fun insert(entity: ScheduledBreakEntity): Long

    @Insert
    suspend fun insertAll(entities: List<ScheduledBreakEntity>)

    @Update
    suspend fun update(entity: ScheduledBreakEntity)

    @Query("SELECT * FROM scheduled_breaks WHERE sessionId = :sessionId ORDER BY offsetMillis ASC")
    suspend fun getForSession(sessionId: Long): List<ScheduledBreakEntity>

    @Query("SELECT * FROM scheduled_breaks WHERE sessionId = :sessionId ORDER BY offsetMillis ASC")
    fun observeForSession(sessionId: Long): Flow<List<ScheduledBreakEntity>>

    @Query("SELECT * FROM scheduled_breaks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScheduledBreakEntity?

    /** Full history across all sessions — used by local analytics aggregation (docs/ANALYTICS.md). */
    @Query("SELECT * FROM scheduled_breaks")
    suspend fun getAll(): List<ScheduledBreakEntity>
}
