package com.prove2break.breaks

/**
 * Pure clamping logic for "Extend Break" requests against a session's
 * [com.prove2break.domain.model.BreakBudget] (BREAK-SYSTEM.md "Break
 * Extension"): bounded increments, up to a maximum number of extensions per
 * break and never past the session's total extension budget.
 */
object BreakExtension {

    /** Default bounded increment offered by the "Extend Break" action (+5 min). */
    const val EXTENSION_INCREMENT_MILLIS = 5 * 60_000L

    /** Default maximum number of extensions allowed per break (BREAK-SYSTEM.md). */
    const val MAX_EXTENSIONS_PER_BREAK = 2

    data class Result(val allowed: Boolean, val newUsedMillis: Long)

    /**
     * Whether granting [incrementMillis] more of extension is allowed given
     * [currentUsedMillis] already used out of [totalBudgetMillis]. Never
     * allows exceeding the budget — a request that would overshoot is
     * rejected outright (not partially granted), and the used total is left
     * unchanged in that case.
     */
    fun requestExtension(
        currentUsedMillis: Long,
        totalBudgetMillis: Long,
        incrementMillis: Long = EXTENSION_INCREMENT_MILLIS,
    ): Result {
        val newUsed = currentUsedMillis + incrementMillis
        return if (newUsed <= totalBudgetMillis) {
            Result(allowed = true, newUsedMillis = newUsed)
        } else {
            Result(allowed = false, newUsedMillis = currentUsedMillis)
        }
    }

    /** The total extension budget for a session's break schedule, given [breakCount] scheduled breaks. */
    fun totalBudgetForSchedule(breakCount: Int): Long {
        return breakCount.toLong() * MAX_EXTENSIONS_PER_BREAK * EXTENSION_INCREMENT_MILLIS
    }
}
