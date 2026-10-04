package com.prove2break.focus

import com.prove2break.data.repository.FocusSessionRepository
import com.prove2break.domain.model.FocusSession
import com.prove2break.domain.model.FocusSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns start/pause/resume/complete for the active focus session and exposes
 * its live, timestamp-derived state as a [Flow] for the UI/service to
 * observe. Room (via [FocusSessionRepository]) is the single source of
 * truth — this class never keeps its own copy of "the" session; every start
 * operation and every emitted snapshot round-trips through the repository.
 *
 * See FOCUS-ENGINE.md and ANDROID-ARCHITECTURE.md's Foreground Service Design.
 */
@Singleton
class FocusSessionManager @Inject constructor(
    private val repository: FocusSessionRepository,
) {

    /** Emits the current time once immediately, then every [tickMillis]. */
    private fun ticker(tickMillis: Long): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(tickMillis)
        }
    }

    /**
     * The live state of whatever session Room currently reports as active,
     * recomputed every second. Emits `null` when there is no active session
     * (nothing started yet, or the last one completed/ended).
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeCurrentSession(tickMillis: Long = 1000L): Flow<FocusSessionSnapshot?> {
        return repository.observeActiveSession().flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                ticker(tickMillis).map { now -> buildSnapshot(session, now) }
            }
        }
    }

    suspend fun start(subjectId: String, plannedDurationMillis: Long): Long {
        return repository.startSession(subjectId, plannedDurationMillis)
    }

    suspend fun pause(sessionId: Long) {
        repository.pauseSession(sessionId, System.currentTimeMillis())
    }

    suspend fun resume(sessionId: Long) {
        repository.resumeSession(sessionId, System.currentTimeMillis())
    }

    /** Explicit user-initiated end, or reconciliation once time is up. */
    suspend fun complete(sessionId: Long) {
        repository.completeSession(sessionId, System.currentTimeMillis())
    }

    /**
     * Session-recovery + completion-reconciliation entry point: reads whatever
     * Room currently has as the active session (covers process/service
     * restart, per FOCUS-ENGINE.md "Survival Requirements") and, if its
     * planned duration has already elapsed, immediately reconciles it to
     * COMPLETED before returning the snapshot — so a session that finished
     * while the app/service was dead is never shown as still running.
     */
    suspend fun recoverActiveSession(): FocusSessionSnapshot? {
        val session = repository.getActiveSession() ?: return null
        val snapshot = buildSnapshot(session, System.currentTimeMillis())
        if (snapshot.timer.isComplete && session.status != FocusSessionStatus.COMPLETED) {
            complete(session.id)
            return snapshot.copy(session = snapshot.session.copy(status = FocusSessionStatus.COMPLETED))
        }
        return snapshot
    }

    /** Reconciles a still-active session to COMPLETED once its timer says it's done. */
    suspend fun completeIfDue(snapshot: FocusSessionSnapshot) {
        if (snapshot.timer.isComplete && snapshot.session.status != FocusSessionStatus.COMPLETED) {
            complete(snapshot.session.id)
        }
    }

    private fun buildSnapshot(session: FocusSession, now: Long): FocusSessionSnapshot {
        val timer = FocusTimer.computeSnapshot(
            now = now,
            startedAtEpochMs = session.startedAtEpochMs,
            plannedDurationMillis = session.plannedDurationMillis,
            totalPausedMillis = session.totalPausedMillis,
            pausedAtEpochMs = session.pausedAtEpochMs,
            status = session.status,
        )
        return FocusSessionSnapshot(session, timer)
    }
}
