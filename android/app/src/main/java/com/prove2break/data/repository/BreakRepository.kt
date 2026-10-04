package com.prove2break.data.repository

import com.prove2break.breaks.BreakRecommendation
import com.prove2break.domain.model.BreakBudget
import com.prove2break.domain.model.ScheduledBreak
import kotlinx.coroutines.flow.Flow

/** Single source of truth for a session's scheduled breaks + extension budget. */
interface BreakRepository {
    /** Persists [breaks] as the session's schedule and sets up its extension budget. */
    suspend fun createSchedule(
        sessionId: Long,
        breaks: List<BreakRecommendation.RecommendedBreak>,
        totalExtensionBudgetMillis: Long,
    )

    suspend fun getScheduledBreaks(sessionId: Long): List<ScheduledBreak>
    fun observeScheduledBreaks(sessionId: Long): Flow<List<ScheduledBreak>>

    /** Marks [breakId] ACTIVE — the only way a break moves from PENDING/available to running. */
    suspend fun startBreak(breakId: Long, nowEpochMs: Long)

    /** Marks [breakId] TAKEN (break timer completed, or ended early). */
    suspend fun completeBreak(breakId: Long)

    /**
     * Requests a bounded extension for [breakId], clamped against the
     * session's remaining budget. Returns false (and changes nothing) if the
     * request would exceed the budget.
     */
    suspend fun extendBreak(breakId: Long, incrementMillis: Long): Boolean

    suspend fun getBudget(sessionId: Long): BreakBudget?

    /** Creates and immediately activates an ad-hoc break for the unscheduled path. Returns its id. */
    suspend fun createUnscheduledBreak(sessionId: Long, plannedLengthMillis: Long, nowEpochMs: Long): Long
}
