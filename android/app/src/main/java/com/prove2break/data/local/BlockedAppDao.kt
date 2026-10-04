package com.prove2break.data.local

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {
    @Upsert
    suspend fun upsert(app: BlockedAppEntity)

    @Query("SELECT * FROM blocked_apps ORDER BY appLabel")
    fun observeAll(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE isSelected = 1")
    fun observeSelected(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE isSelected = 1")
    suspend fun getSelected(): List<BlockedAppEntity>

    @Query("UPDATE blocked_apps SET isSelected = :selected WHERE packageName = :packageName")
    suspend fun setSelected(packageName: String, selected: Boolean)
}
