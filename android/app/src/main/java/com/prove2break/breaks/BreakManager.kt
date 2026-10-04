package com.prove2break.breaks

import com.prove2break.data.repository.BreakRepository
import com.prove2break.data.repository.FocusSessionRepository
import com.prove2break.domain.model.FocusSessionStatus
import com.prove2break.domain.model.ScheduledBreak
import com.prove2break.focus.FocusSessionSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collaborates with [com.prove2break.focus.FocusSessionManager] to own the
 * break side of a focus session's state machine (BREAK-SYSTEM.md). Room (via
 * [BreakRepository] / [FocusSessionRepository]) remains the single source of
 * truth, exactly like [com.prove2break.focus.FocusSessionManager] — this class
 * holds no session state of its own.
 *
 * State machine implemented here (BREAK-SYSTEM.md):
 *
 * Scheduled path:
 *   FOCUSING -> BREAK_AVAILABLE (derived from elapsed time vs. schedule, not stored)
 *   BREAK_AVAILABLE -> BREAK_ACTIVE   only via [takeBreak] (explicit student action)
 *   BREAK_AVAILABLE -> FOCUSING       via [continueStudying] (break stays PENDING/re-offerable)
 *   BREAK_ACTIVE -> FOCUSING          via [endBreak] (timer complete or ended early)
 *
 * Unscheduled path:
 *   FOCUSING -> BREAK_VERIFICATION -> QUIZ_ACTIVE -> BREAK_ACTIVE | FOCUSING
 *   via [requestUnscheduledBreak], [startQuiz], [resolveQuizResult].
 */
@Singleton
class BreakManager @Inject constructor(
    private val breakRepository: BreakRepository,
    private val focusSessionRepository: FocusSessionRepository,
) {

    /** Generates and persists the recommended break schedule for a just-started session. */
    suspend fun scheduleBreaksForSession(
        sessionId: Long,
        plannedDurationMillis: Long,
        recommendedBreaks: List<BreakRecommendation.RecommendedBreak> = BreakRecommendation.recommend(plannedDurationMillis),
    ) {
        val extensionBudget = BreakExtension.totalBudgetForSchedule(recommendedBreaks.size)
        breakRepository.createSchedule(sessionId, recommendedBreaks, extensionBudget)
    }

    /**
     * The break (if any) currently available to offer the student, recomputed
     * on every emission of [focusSnapshots] — never a one-shot decision, so a
     * session that sits FOCUSING past a break's offset keeps seeing it as
     * available (deferred, not skipped) exactly like BREAK-SYSTEM.md's
     * "Deferred break" type.
     */
    fun observeAvailableBreak(sessionId: Long, focusSnapshots: Flow<FocusSessionSnapshot?>): Flow<ScheduledBreak?> {
        return combine(focusSnapshots, breakRepository.observeScheduledBreaks(sessionId)) { snapshot, breaks ->
            if (snapshot == null) null else BreakSchedule.currentAvailableBreak(snapshot.timer.elapsedMillis, breaks)
        }.distinctUntilChanged()
    }

    /** Live countdown for whichever break is currently ACTIVE, ticking every second like [FocusTimer]. */
    fun observeActiveBreakTimer(sessionId: Long): Flow<Pair<ScheduledBreak, BreakTimerSnapshot>?> {
        return combine(breakRepository.observeScheduledBreaks(sessionId), ticker(1000L)) { breaks, now ->
            val active = breaks.firstOrNull { it.status == com.prove2break.domain.model.ScheduledBreakStatus.ACTIVE }
            if (active?.activeStartedAtEpochMs == null) {
                null
            } else {
                active to BreakTimer.computeSnapshot(
                    now = now,
                    activeStartedAtEpochMs = active.activeStartedAtEpochMs,
                    plannedLengthMillis = active.plannedLengthMillis,
                    extendedMillis = active.extendedMillis,
                )
            }
        }.distinctUntilChanged()
    }

    private fun ticker(tickMillis: Long): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(tickMillis)
        }
    }

    /** Explicit "Take Break" action — the ONLY path from available to active. */
    suspend fun takeBreak(sessionId: Long, breakId: Long) {
        breakRepository.startBreak(breakId, System.currentTimeMillis())
        focusSessionRepository.setStatus(sessionId, FocusSessionStatus.BREAK_ACTIVE)
    }

    /** "Continue Studying" — the break is left PENDING, so it is offered again later. */
    suspend fun continueStudying(sessionId: Long) {
        focusSessionRepository.setStatus(sessionId, FocusSessionStatus.FOCUSING)
    }

    /** Break timer completed, or the student ended it early. */
    suspend fun endBreak(sessionId: Long, breakId: Long) {
        breakRepository.completeBreak(breakId)
        focusSessionRepository.setStatus(sessionId, FocusSessionStatus.FOCUSING)
    }

    /** Bounded "Extend Break" request; returns false if it would exceed the session's budget. */
    suspend fun extendBreak(
        breakId: Long,
        incrementMillis: Long = BreakExtension.EXTENSION_INCREMENT_MILLIS,
    ): Boolean {
        return breakRepository.extendBreak(breakId, incrementMillis)
    }

    suspend fun getBudget(sessionId: Long) = breakRepository.getBudget(sessionId)

    // --- Unscheduled break path (BREAK-SYSTEM.md "Unscheduled path") ---

    /** Student tapped "Break Now" — enter topic-claim selection. */
    suspend fun requestUnscheduledBreak(sessionId: Long) {
        focusSessionRepository.setStatus(sessionId, FocusSessionStatus.BREAK_VERIFICATION)
    }

    /** Claimed topics confirmed — enter the rapid-fire quiz. */
    suspend fun startQuiz(sessionId: Long) {
        focusSessionRepository.setStatus(sessionId, FocusSessionStatus.QUIZ_ACTIVE)
    }

    /**
     * Resolves the outcome of a real, backend-scored quiz attempt
     * (QUIZ-SYSTEM.md "Scoring") into the session's break state.
     *
     * On PASS: creates and activates an unscheduled break (BREAK_ACTIVE).
     * On FAIL: returns to FOCUSING with non-judgmental copy — the break stays
     * locked but future attempts are never blocked (PRODUCT.md principle 2).
     */
    suspend fun resolveQuizResult(sessionId: Long, pass: Boolean, breakLengthMillis: Long): Long? {
        return if (pass) {
            val breakId = breakRepository.createUnscheduledBreak(sessionId, breakLengthMillis, System.currentTimeMillis())
            focusSessionRepository.setStatus(sessionId, FocusSessionStatus.BREAK_ACTIVE)
            breakId
        } else {
            focusSessionRepository.setStatus(sessionId, FocusSessionStatus.FOCUSING)
            null
        }
    }

    companion object {
        /** Default length for an unscheduled break once verification passes. */
        const val DEFAULT_UNSCHEDULED_BREAK_LENGTH_MILLIS = 10 * 60_000L
    }
}
