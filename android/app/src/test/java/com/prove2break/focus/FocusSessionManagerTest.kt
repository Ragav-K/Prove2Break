package com.prove2break.focus

import com.prove2break.data.repository.FocusSessionRepository
import com.prove2break.domain.model.FocusSession
import com.prove2break.domain.model.FocusSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A fake repository backed by a single in-memory row, standing in for Room.
 * Used to prove session recovery: a brand-new [FocusSessionManager] instance
 * (simulating a fresh process/service after death) reads the existing row
 * and computes the correct remaining time from timestamps alone â€” never
 * resetting to zero.
 */
private class FakeFocusSessionRepository : FocusSessionRepository {
    private var stored: FocusSession? = null
    private val activeFlow = MutableStateFlow<FocusSession?>(null)
    private var nextId = 1L

    override suspend fun startSession(subjectId: String, plannedDurationMillis: Long): Long {
        val session = FocusSession(
            id = nextId++,
            subjectId = subjectId,
            startedAtEpochMs = System.currentTimeMillis(),
            plannedDurationMillis = plannedDurationMillis,
            pausedAtEpochMs = null,
            totalPausedMillis = 0,
            status = FocusSessionStatus.FOCUSING,
            endedAtEpochMs = null,
            isActive = true,
        )
        stored = session
        activeFlow.value = session
        return session.id
    }

    /** Test seam: directly install a row, as if it were already in Room from a prior process. */
    fun seed(session: FocusSession) {
        stored = session
        activeFlow.value = session.takeIf { it.isActive }
    }

    override suspend fun getActiveSession(): FocusSession? = stored?.takeIf { it.isActive }

    override fun observeActiveSession(): Flow<FocusSession?> = activeFlow

    override fun observeAllSessions(): Flow<List<FocusSession>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun pauseSession(sessionId: Long, nowEpochMs: Long) {
        val s = stored ?: return
        if (s.id != sessionId || s.status != FocusSessionStatus.FOCUSING) return
        stored = s.copy(status = FocusSessionStatus.PAUSED, pausedAtEpochMs = nowEpochMs)
        activeFlow.value = stored
    }

    override suspend fun resumeSession(sessionId: Long, nowEpochMs: Long) {
        val s = stored ?: return
        if (s.id != sessionId || s.status != FocusSessionStatus.PAUSED) return
        val pausedAt = s.pausedAtEpochMs ?: nowEpochMs
        stored = s.copy(
            status = FocusSessionStatus.FOCUSING,
            pausedAtEpochMs = null,
            totalPausedMillis = s.totalPausedMillis + (nowEpochMs - pausedAt).coerceAtLeast(0),
        )
        activeFlow.value = stored
    }

    override suspend fun completeSession(sessionId: Long, nowEpochMs: Long) {
        val s = stored ?: return
        if (s.id != sessionId) return
        stored = s.copy(status = FocusSessionStatus.COMPLETED, endedAtEpochMs = nowEpochMs, isActive = false)
        activeFlow.value = null
    }

    override suspend fun setStatus(sessionId: Long, status: FocusSessionStatus) {
        val s = stored ?: return
        if (s.id != sessionId) return
        stored = s.copy(status = status)
        activeFlow.value = stored?.takeIf { it.isActive }
    }
}

class FocusSessionManagerTest {

    @Test
    fun `fresh manager instance recovers an active session and computes correct remaining time`() = runBlocking {
        val repository = FakeFocusSessionRepository()
        val plannedDuration = 60 * 60 * 1000L // 1 hour
        // Simulate a session that had already been running for 20 minutes
        // before the process died â€” this row exists in "Room" only.
        val startedAt = System.currentTimeMillis() - 20 * 60 * 1000L
        repository.seed(
            FocusSession(
                id = 99L,
                subjectId = "subj-1",
                startedAtEpochMs = startedAt,
                plannedDurationMillis = plannedDuration,
                pausedAtEpochMs = null,
                totalPausedMillis = 0,
                status = FocusSessionStatus.FOCUSING,
                endedAtEpochMs = null,
                isActive = true,
            ),
        )

        // A brand-new manager instance â€” no in-memory state carried over.
        val freshManager = FocusSessionManager(repository)
        val recovered = freshManager.recoverActiveSession()

        assertTrue(recovered != null)
        assertEquals(FocusSessionStatus.FOCUSING, recovered!!.session.status)
        // ~20 minutes elapsed, ~40 minutes remaining â€” never reset to zero/full.
        assertTrue(recovered.timer.elapsedMillis in (19 * 60 * 1000L)..(21 * 60 * 1000L))
        assertTrue(recovered.timer.remainingMillis in (39 * 60 * 1000L)..(41 * 60 * 1000L))
        assertTrue(!recovered.timer.isComplete)
    }

    @Test
    fun `recovery reconciles to COMPLETED when planned duration elapsed while dead`() = runBlocking {
        val repository = FakeFocusSessionRepository()
        val plannedDuration = 30 * 60 * 1000L
        // Session should have finished 10 minutes ago, but the process died
        // before it could be marked COMPLETED.
        val startedAt = System.currentTimeMillis() - 40 * 60 * 1000L
        repository.seed(
            FocusSession(
                id = 5L,
                subjectId = "subj-1",
                startedAtEpochMs = startedAt,
                plannedDurationMillis = plannedDuration,
                pausedAtEpochMs = null,
                totalPausedMillis = 0,
                status = FocusSessionStatus.FOCUSING,
                endedAtEpochMs = null,
                isActive = true,
            ),
        )

        val freshManager = FocusSessionManager(repository)
        val recovered = freshManager.recoverActiveSession()

        assertTrue(recovered != null)
        assertEquals(FocusSessionStatus.COMPLETED, recovered!!.session.status)
        assertTrue(recovered.timer.isComplete)
        // The repository itself must have been written through to, not just the snapshot.
        assertNull(repository.getActiveSession())
    }

    @Test
    fun `no active session recovers as null`() = runBlocking {
        val repository = FakeFocusSessionRepository()
        val freshManager = FocusSessionManager(repository)

        assertNull(freshManager.recoverActiveSession())
    }
}
