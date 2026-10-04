package com.prove2break.breaks

import com.prove2break.data.repository.BreakRepository
import com.prove2break.data.repository.FocusSessionRepository
import com.prove2break.domain.model.BreakBudget
import com.prove2break.domain.model.FocusSession
import com.prove2break.domain.model.FocusSessionStatus
import com.prove2break.domain.model.ScheduledBreak
import com.prove2break.domain.model.ScheduledBreakStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory fake standing in for Room, mirroring FocusSessionManagerTest's fake style. */
private class FakeBreakRepository : BreakRepository {
    val breaksById = mutableMapOf<Long, ScheduledBreak>()
    var budget: BreakBudget? = null
    private var nextId = 1L
    val flows = mutableMapOf<Long, MutableStateFlow<List<ScheduledBreak>>>()

    private fun flowFor(sessionId: Long) = flows.getOrPut(sessionId) { MutableStateFlow(emptyList()) }

    private fun publish(sessionId: Long) {
        flowFor(sessionId).value = breaksById.values.filter { it.sessionId == sessionId }
    }

    override suspend fun createSchedule(
        sessionId: Long,
        breaks: List<com.prove2break.breaks.BreakRecommendation.RecommendedBreak>,
        totalExtensionBudgetMillis: Long,
    ) {
        breaks.forEach { recommended ->
            val id = nextId++
            breaksById[id] = ScheduledBreak(
                id = id,
                sessionId = sessionId,
                offsetMillis = recommended.offsetMillis,
                plannedLengthMillis = recommended.lengthMillis,
                status = ScheduledBreakStatus.PENDING,
                activeStartedAtEpochMs = null,
                extendedMillis = 0,
                isUnscheduled = false,
            )
        }
        budget = BreakBudget(sessionId, totalExtensionBudgetMillis, 0)
        publish(sessionId)
    }

    override suspend fun getScheduledBreaks(sessionId: Long): List<ScheduledBreak> =
        breaksById.values.filter { it.sessionId == sessionId }

    override fun observeScheduledBreaks(sessionId: Long): Flow<List<ScheduledBreak>> = flowFor(sessionId)

    override suspend fun startBreak(breakId: Long, nowEpochMs: Long) {
        val b = breaksById[breakId] ?: return
        if (b.status != ScheduledBreakStatus.PENDING) return
        breaksById[breakId] = b.copy(status = ScheduledBreakStatus.ACTIVE, activeStartedAtEpochMs = nowEpochMs)
        publish(b.sessionId)
    }

    override suspend fun completeBreak(breakId: Long) {
        val b = breaksById[breakId] ?: return
        breaksById[breakId] = b.copy(status = ScheduledBreakStatus.TAKEN)
        publish(b.sessionId)
    }

    override suspend fun extendBreak(breakId: Long, incrementMillis: Long): Boolean {
        val b = breaksById[breakId] ?: return false
        val currentBudget = budget ?: return false
        val result = BreakExtension.requestExtension(currentBudget.usedMillis, currentBudget.totalBudgetMillis, incrementMillis)
        if (!result.allowed) return false
        budget = currentBudget.copy(usedMillis = result.newUsedMillis)
        breaksById[breakId] = b.copy(extendedMillis = b.extendedMillis + incrementMillis)
        publish(b.sessionId)
        return true
    }

    override suspend fun getBudget(sessionId: Long): BreakBudget? = budget

    override suspend fun createUnscheduledBreak(sessionId: Long, plannedLengthMillis: Long, nowEpochMs: Long): Long {
        val id = nextId++
        breaksById[id] = ScheduledBreak(
            id = id,
            sessionId = sessionId,
            offsetMillis = 0,
            plannedLengthMillis = plannedLengthMillis,
            status = ScheduledBreakStatus.ACTIVE,
            activeStartedAtEpochMs = nowEpochMs,
            extendedMillis = 0,
            isUnscheduled = true,
        )
        publish(sessionId)
        return id
    }
}

private class FakeFocusSessionRepository : FocusSessionRepository {
    var session: FocusSession? = null

    override suspend fun startSession(subjectId: String, plannedDurationMillis: Long): Long = 1L
    override suspend fun getActiveSession(): FocusSession? = session
    override fun observeActiveSession(): Flow<FocusSession?> = MutableStateFlow(session)
    override fun observeAllSessions(): Flow<List<FocusSession>> = MutableStateFlow(emptyList())
    override suspend fun setStatus(sessionId: Long, status: FocusSessionStatus) {
        session = session?.copy(status = status)
    }
    override suspend fun pauseSession(sessionId: Long, nowEpochMs: Long) {}
    override suspend fun resumeSession(sessionId: Long, nowEpochMs: Long) {}
    override suspend fun completeSession(sessionId: Long, nowEpochMs: Long) {}
}

class BreakManagerTest {

    private fun newSession(id: Long = 1L) = FocusSession(
        id = id,
        subjectId = "subj-1",
        startedAtEpochMs = 0L,
        plannedDurationMillis = 60 * 60_000L,
        pausedAtEpochMs = null,
        totalPausedMillis = 0,
        status = FocusSessionStatus.FOCUSING,
        endedAtEpochMs = null,
        isActive = true,
    )

    @Test
    fun `continue studying never auto-starts a break and leaves it PENDING re-offerable`() = runBlocking {
        val breakRepo = FakeBreakRepository()
        val sessionRepo = FakeFocusSessionRepository().apply { session = newSession() }
        val manager = BreakManager(breakRepo, sessionRepo)

        manager.scheduleBreaksForSession(1L, 60 * 60_000L)
        val theBreak = breakRepo.getScheduledBreaks(1L).first()

        // "Continue Studying" without ever calling takeBreak().
        manager.continueStudying(1L)

        // Session goes back to FOCUSING...
        assertEquals(FocusSessionStatus.FOCUSING, sessionRepo.session?.status)
        // ...and the break itself was never touched: still PENDING, never ACTIVE.
        val stillPending = breakRepo.getScheduledBreaks(1L).first { it.id == theBreak.id }
        assertEquals(ScheduledBreakStatus.PENDING, stillPending.status)

        // Recomputing availability later still offers the same break (re-offered, not consumed).
        val stillOffered = BreakSchedule.currentAvailableBreak(90 * 60_000L, breakRepo.getScheduledBreaks(1L))
        assertEquals(theBreak.id, stillOffered?.id)
    }

    @Test
    fun `take break is the only path to ACTIVE and requires an explicit call`() = runBlocking {
        val breakRepo = FakeBreakRepository()
        val sessionRepo = FakeFocusSessionRepository().apply { session = newSession() }
        val manager = BreakManager(breakRepo, sessionRepo)

        manager.scheduleBreaksForSession(1L, 60 * 60_000L)
        val theBreak = breakRepo.getScheduledBreaks(1L).first()

        manager.takeBreak(1L, theBreak.id)

        assertEquals(FocusSessionStatus.BREAK_ACTIVE, sessionRepo.session?.status)
        val active = breakRepo.getScheduledBreaks(1L).first { it.id == theBreak.id }
        assertEquals(ScheduledBreakStatus.ACTIVE, active.status)
    }

    @Test
    fun `extension never exceeds the session's budget`() = runBlocking {
        val breakRepo = FakeBreakRepository()
        val sessionRepo = FakeFocusSessionRepository().apply { session = newSession() }
        val manager = BreakManager(breakRepo, sessionRepo)

        manager.scheduleBreaksForSession(1L, 60 * 60_000L) // 1 break -> budget = 1 * 2 * 5min = 10min
        val theBreak = breakRepo.getScheduledBreaks(1L).first()
        manager.takeBreak(1L, theBreak.id)

        assertTrue(manager.extendBreak(theBreak.id, 5 * 60_000L)) // used 5/10
        assertTrue(manager.extendBreak(theBreak.id, 5 * 60_000L)) // used 10/10 - at boundary, allowed
        assertFalse(manager.extendBreak(theBreak.id, 5 * 60_000L)) // would exceed - rejected

        val budget = manager.getBudget(1L)!!
        assertEquals(10 * 60_000L, budget.usedMillis)
        assertEquals(0L, budget.remainingMillis)
    }

    @Test
    fun `unscheduled break fail resolution returns to FOCUSING without creating a break`() = runBlocking {
        val breakRepo = FakeBreakRepository()
        val sessionRepo = FakeFocusSessionRepository().apply { session = newSession() }
        val manager = BreakManager(breakRepo, sessionRepo)

        manager.requestUnscheduledBreak(1L)
        assertEquals(FocusSessionStatus.BREAK_VERIFICATION, sessionRepo.session?.status)
        manager.startQuiz(1L)
        assertEquals(FocusSessionStatus.QUIZ_ACTIVE, sessionRepo.session?.status)

        val result = manager.resolveQuizResult(1L, pass = false, breakLengthMillis = 10 * 60_000L)

        assertEquals(null, result)
        assertEquals(FocusSessionStatus.FOCUSING, sessionRepo.session?.status)
        assertTrue(breakRepo.getScheduledBreaks(1L).isEmpty())
    }

    @Test
    fun `unscheduled break pass resolution creates and activates a break`() = runBlocking {
        val breakRepo = FakeBreakRepository()
        val sessionRepo = FakeFocusSessionRepository().apply { session = newSession() }
        val manager = BreakManager(breakRepo, sessionRepo)

        manager.requestUnscheduledBreak(1L)
        manager.startQuiz(1L)
        val breakId = manager.resolveQuizResult(1L, pass = true, breakLengthMillis = 10 * 60_000L)

        assertTrue(breakId != null)
        assertEquals(FocusSessionStatus.BREAK_ACTIVE, sessionRepo.session?.status)
        val created = breakRepo.getScheduledBreaks(1L).first { it.id == breakId }
        assertEquals(ScheduledBreakStatus.ACTIVE, created.status)
        assertTrue(created.isUnscheduled)
    }
}
