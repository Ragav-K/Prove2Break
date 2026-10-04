package com.prove2break.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BreakBudgetDao {
    @Insert
    suspend fun insert(entity: BreakBudgetEntity)

    @Update
    suspend fun update(entity: BreakBudgetEntity)

    @Query("SELECT * FROM break_budgets WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getForSession(sessionId: Long): BreakBudgetEntity?
}
