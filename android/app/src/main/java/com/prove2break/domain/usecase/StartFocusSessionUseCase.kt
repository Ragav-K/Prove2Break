package com.prove2break.domain.usecase

import com.prove2break.data.repository.FocusSessionRepository
import javax.inject.Inject

/**
 * Single-responsibility business operation, per ANDROID-ARCHITECTURE.md.
 * Enforces the FOCUS-ENGINE.md supported-duration bounds (15 min .. 8 hours)
 * before delegating to the repository (Room-backed source of truth).
 */
class StartFocusSessionUseCase @Inject constructor(
    private val repository: FocusSessionRepository,
) {
    suspend operator fun invoke(subjectId: String, plannedDurationMillis: Long): Long {
        require(plannedDurationMillis in MIN_DURATION_MILLIS..MAX_DURATION_MILLIS) {
            "plannedDurationMillis must be within $MIN_DURATION_MILLIS..$MAX_DURATION_MILLIS"
        }
        return repository.startSession(subjectId, plannedDurationMillis)
    }

    companion object {
        const val MIN_DURATION_MILLIS = 15 * 60 * 1000L
        const val MAX_DURATION_MILLIS = 8 * 60 * 60 * 1000L
    }
}
