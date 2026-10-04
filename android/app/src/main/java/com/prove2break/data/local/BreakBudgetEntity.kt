package com.prove2break.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per focus session: its break-extension budget. See
 * [com.prove2break.breaks.BreakExtension] and BREAK-SYSTEM.md "Break Extension".
 */
@Entity(tableName = "break_budgets")
data class BreakBudgetEntity(
    @PrimaryKey val sessionId: Long,
    val totalBudgetMillis: Long,
    val usedMillis: Long = 0,
)
