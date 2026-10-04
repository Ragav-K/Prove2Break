package com.prove2break.data.repository

import com.prove2break.domain.model.FocusSession
import com.prove2break.domain.model.FocusSessionStatus
import kotlinx.coroutines.flow.Flow

/** Single source of truth for focus sessions, abstracting Room from callers. */
interface FocusSessionRepository {
    suspend fun startSession(subjectId: String, plannedDurationMillis: Long): Long
    suspend fun getActiveSession(): FocusSession?
    fun observeActiveSession(): Flow<FocusSession?>
    fun observeAllSessions(): Flow<List<FocusSession>>

    /**
     * Directly sets the session's status — used by the break system
     * (BREAK-SYSTEM.md's state machine) for transitions that aren't a plain
     * pause/resume/complete, e.g. FOCUSING <-> BREAK_AVAILABLE/BREAK_ACTIVE
     * and the unscheduled BREAK_VERIFICATION -> QUIZ_ACTIVE path.
     */
    suspend fun setStatus(sessionId: Long, status: FocusSessionStatus)

    /** Marks the session paused, freezing elapsed time as of [nowEpochMs]. */
    suspend fun pauseSession(sessionId: Long, nowEpochMs: Long)

    /** Resumes a paused session, folding the just-finished pause into totalPausedMillis. */
    suspend fun resumeSession(sessionId: Long, nowEpochMs: Long)

    /** Marks the session COMPLETED and no longer active (planned duration elapsed, or ended early). */
    suspend fun completeSession(sessionId: Long, nowEpochMs: Long)
}
