package com.prove2break

import com.prove2break.data.repository.FocusSessionRepository
import com.prove2break.domain.model.FocusSession
import com.prove2break.domain.model.FocusSessionStatus
import com.prove2break.domain.usecase.StartFocusSessionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Proves the unit test infra works and pins down basic use-case behavior
 * (see ANDROID-ARCHITECTURE.md domain/usecase layer).
 */
class StartFocusSessionUseCaseTest {

    private class FakeFocusSessionRepository : FocusSessionRepository {
        var lastSubjectId: String? = null
        var lastDuration: Long? = null

        override suspend fun startSession(subjectId: String, plannedDurationMillis: Long): Long {
            lastSubjectId = subjectId
            lastDuration = plannedDurationMillis
            return 42L
        }

        override suspend fun getActiveSession(): FocusSession? = null

        override fun observeActiveSession(): Flow<FocusSession?> = flowOf(null)

        override fun observeAllSessions(): Flow<List<FocusSession>> = flowOf(emptyList())

        override suspend fun pauseSession(sessionId: Long, nowEpochMs: Long) = Unit

        override suspend fun resumeSession(sessionId: Long, nowEpochMs: Long) = Unit

        override suspend fun completeSession(sessionId: Long, nowEpochMs: Long) = Unit

        override suspend fun setStatus(sessionId: Long, status: FocusSessionStatus) = Unit
    }

    @Test
    fun `invoke returns the new session id and forwards params`() = runBlocking {
        val repository = FakeFocusSessionRepository()
        val useCase = StartFocusSessionUseCase(repository)

        val id = useCase(subjectId = "subj-7", plannedDurationMillis = 25 * 60 * 1000L)

        assertEquals(42L, id)
        assertEquals("subj-7", repository.lastSubjectId)
        assertEquals(25 * 60 * 1000L, repository.lastDuration)
    }

    @Test
    fun `invoke rejects duration below the minimum bound`() {
        val repository = FakeFocusSessionRepository()
        val useCase = StartFocusSessionUseCase(repository)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(subjectId = "subj-1", plannedDurationMillis = 1000L) }
        }
    }

    @Test
    fun `invoke rejects duration above the maximum bound`() {
        val repository = FakeFocusSessionRepository()
        val useCase = StartFocusSessionUseCase(repository)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCase(subjectId = "subj-1", plannedDurationMillis = 9 * 60 * 60 * 1000L)
            }
        }
    }
}
