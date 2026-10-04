package com.prove2break.ui.apprestriction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.data.repository.EmergencyUnlockRepository
import com.prove2break.domain.model.EmergencyUnlockReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the emergency-unlock reason picker. Every grant is written to Room
 * before access is allowed — never a silent bypass (APP-RESTRICTION.md).
 */
@HiltViewModel
class EmergencyUnlockViewModel @Inject constructor(
    private val emergencyUnlockRepository: EmergencyUnlockRepository,
) : ViewModel() {

    fun requestUnlock(
        packageName: String,
        sessionId: Long,
        reason: EmergencyUnlockReason,
        durationMillis: Long,
        onGranted: () -> Unit,
    ) {
        viewModelScope.launch {
            emergencyUnlockRepository.grant(
                packageName = packageName,
                reason = reason,
                durationMillis = durationMillis,
                focusSessionId = sessionId,
            )
            onGranted()
        }
    }

    companion object {
        val DEFAULT_DURATION_OPTIONS = listOf(
            5L * 60_000L to "5 minutes",
            15L * 60_000L to "15 minutes",
            30L * 60_000L to "30 minutes",
        )
    }
}
