package com.prove2break.domain.model

/**
 * A session's break-extension budget (BREAK-SYSTEM.md "Break Extension"):
 * bounded total allocation so extension requests can never silently consume
 * the whole session. This is separate from the scheduled breaks' own planned
 * lengths — it tracks only the *extra* time granted via "Extend Break".
 */
data class BreakBudget(
    val sessionId: Long,
    val totalBudgetMillis: Long,
    val usedMillis: Long,
) {
    val remainingMillis: Long get() = (totalBudgetMillis - usedMillis).coerceAtLeast(0)
}
