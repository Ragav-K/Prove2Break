package com.prove2break.breaks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Proves extension requests are clamped/rejected at the session's budget boundary. */
class BreakExtensionTest {

    private val fiveMinutes = 5 * 60_000L

    @Test
    fun `extension well within budget is allowed`() {
        val result = BreakExtension.requestExtension(
            currentUsedMillis = 0,
            totalBudgetMillis = 20 * 60_000L,
            incrementMillis = fiveMinutes,
        )
        assertTrue(result.allowed)
        assertEquals(fiveMinutes, result.newUsedMillis)
    }

    @Test
    fun `extension exactly reaching the budget boundary is allowed`() {
        val result = BreakExtension.requestExtension(
            currentUsedMillis = 15 * 60_000L,
            totalBudgetMillis = 20 * 60_000L,
            incrementMillis = fiveMinutes,
        )
        assertTrue(result.allowed)
        assertEquals(20 * 60_000L, result.newUsedMillis)
    }

    @Test
    fun `extension that would exceed the budget by even one millisecond is rejected`() {
        val result = BreakExtension.requestExtension(
            currentUsedMillis = 16 * 60_000L,
            totalBudgetMillis = 20 * 60_000L,
            incrementMillis = fiveMinutes,
        )
        assertFalse(result.allowed)
        // Rejected request must not change the used total.
        assertEquals(16 * 60_000L, result.newUsedMillis)
    }

    @Test
    fun `extension requested when budget is already exhausted is rejected`() {
        val result = BreakExtension.requestExtension(
            currentUsedMillis = 20 * 60_000L,
            totalBudgetMillis = 20 * 60_000L,
            incrementMillis = fiveMinutes,
        )
        assertFalse(result.allowed)
        assertEquals(20 * 60_000L, result.newUsedMillis)
    }

    @Test
    fun `total budget for a schedule scales with break count and max extensions per break`() {
        val budget = BreakExtension.totalBudgetForSchedule(breakCount = 3)
        assertEquals(3L * BreakExtension.MAX_EXTENSIONS_PER_BREAK * BreakExtension.EXTENSION_INCREMENT_MILLIS, budget)
    }
}
