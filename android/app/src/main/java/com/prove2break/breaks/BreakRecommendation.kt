package com.prove2break.breaks

import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * Pure break-recommendation engine — mirrors [com.prove2break.focus.FocusTimer]'s
 * "pure function, no state" style. Implements BREAK-SYSTEM.md's recommendation
 * table exactly for the four fixed tiers (1h/2h/3h/4h) and the documented
 * `ceil(duration / 55min)` formula for any other ("custom") duration.
 *
 * These are recommendations, not enforced (BREAK-SYSTEM.md, FR-032/FR-033):
 * the returned list is a plain, immutable value the caller can freely copy,
 * edit, reorder, or replace entries of before a session starts — see
 * [withUpdatedBreak] / [withRemovedBreak] for small helpers, or just build a
 * new `List<RecommendedBreak>` directly.
 */
object BreakRecommendation {

    private const val MINUTE_MS = 60_000L
    private const val ONE_HOUR = 60 * MINUTE_MS
    private const val TWO_HOURS = 120 * MINUTE_MS
    private const val THREE_HOURS = 180 * MINUTE_MS
    private const val FOUR_HOURS = 240 * MINUTE_MS
    private const val CUSTOM_INTERVAL = 55 * MINUTE_MS

    /** One recommended break: when it fires (offset from session start) and how long. */
    data class RecommendedBreak(
        val offsetMillis: Long,
        val lengthMillis: Long,
    )

    /**
     * Returns the recommended break schedule for [plannedDurationMillis], per
     * BREAK-SYSTEM.md's table. The four documented tiers are matched exactly
     * on duration; anything else falls back to the "Custom" formula.
     */
    fun recommend(plannedDurationMillis: Long): List<RecommendedBreak> {
        require(plannedDurationMillis > 0) { "plannedDurationMillis must be positive" }
        return when (plannedDurationMillis) {
            ONE_HOUR -> listOf(
                RecommendedBreak(offsetMillis = 50 * MINUTE_MS, lengthMillis = 5 * MINUTE_MS),
            )
            TWO_HOURS -> listOf(
                RecommendedBreak(offsetMillis = 50 * MINUTE_MS, lengthMillis = 5 * MINUTE_MS),
                RecommendedBreak(offsetMillis = 110 * MINUTE_MS, lengthMillis = 10 * MINUTE_MS),
            )
            THREE_HOURS -> listOf(
                RecommendedBreak(offsetMillis = 55 * MINUTE_MS, lengthMillis = 10 * MINUTE_MS),
                RecommendedBreak(offsetMillis = 110 * MINUTE_MS, lengthMillis = 10 * MINUTE_MS),
                RecommendedBreak(offsetMillis = 165 * MINUTE_MS, lengthMillis = 10 * MINUTE_MS),
            )
            FOUR_HOURS -> listOf(
                RecommendedBreak(offsetMillis = 55 * MINUTE_MS, lengthMillis = 12 * MINUTE_MS),
                RecommendedBreak(offsetMillis = 110 * MINUTE_MS, lengthMillis = 12 * MINUTE_MS),
                RecommendedBreak(offsetMillis = 165 * MINUTE_MS, lengthMillis = 12 * MINUTE_MS),
                RecommendedBreak(offsetMillis = 220 * MINUTE_MS, lengthMillis = 15 * MINUTE_MS),
            )
            else -> recommendCustom(plannedDurationMillis)
        }
    }

    /**
     * Custom durations: `ceil(duration / 55min)` breaks, evenly spaced across
     * the session (not just at fixed 55-min offsets past the end, which could
     * push the last break past the session's own end for short overshoots),
     * length scaled modestly (5–10 min) with total duration.
     */
    private fun recommendCustom(plannedDurationMillis: Long): List<RecommendedBreak> {
        val breakCount = ceil(plannedDurationMillis.toDouble() / CUSTOM_INTERVAL).toInt().coerceAtLeast(1)
        val lengthMillis = scaledCustomLengthMillis(plannedDurationMillis)
        // Evenly space breaks across the session: for N breaks, use N+1 equal
        // segments so no break lands exactly at session start or end.
        val segment = plannedDurationMillis / (breakCount + 1)
        return (1..breakCount).map { index ->
            RecommendedBreak(offsetMillis = segment * index, lengthMillis = lengthMillis)
        }
    }

    /** Scales break length 5–10 min modestly with total duration (BREAK-SYSTEM.md). */
    private fun scaledCustomLengthMillis(plannedDurationMillis: Long): Long {
        val hours = plannedDurationMillis.toDouble() / ONE_HOUR
        val minutes = (5.0 + hours).coerceIn(5.0, 10.0)
        return (minutes * MINUTE_MS).roundToLong()
    }

    /** Returns a copy of [breaks] with the entry at [index] replaced. User-adjustment helper. */
    fun withUpdatedBreak(
        breaks: List<RecommendedBreak>,
        index: Int,
        offsetMillis: Long = breaks[index].offsetMillis,
        lengthMillis: Long = breaks[index].lengthMillis,
    ): List<RecommendedBreak> {
        return breaks.toMutableList().also {
            it[index] = RecommendedBreak(offsetMillis, lengthMillis)
        }
    }

    /** Returns a copy of [breaks] with the entry at [index] removed. User-adjustment helper. */
    fun withRemovedBreak(breaks: List<RecommendedBreak>, index: Int): List<RecommendedBreak> {
        return breaks.toMutableList().also { it.removeAt(index) }
    }
}
