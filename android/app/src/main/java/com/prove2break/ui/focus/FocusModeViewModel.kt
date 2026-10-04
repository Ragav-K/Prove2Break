package com.prove2break.ui.focus

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.breaks.BreakManager
import com.prove2break.domain.model.ScheduledBreak
import com.prove2break.focus.FocusService
import com.prove2break.focus.FocusSessionManager
import com.prove2break.focus.FocusSessionSnapshot
import com.prove2break.focus.FocusWatchdogScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the live Focus Mode screen: collects [FocusSessionManager]'s
 * timestamp-derived snapshot flow (never a locally-decrementing counter —
 * FOCUS-ENGINE.md) and exposes pause/resume/end-session actions, plus the
 * scheduled-break-available banner state and its "Take Break" /
 * "Continue Studying" actions (BREAK-SYSTEM.md).
 *
 * [sessionId] comes from the "focus_mode/{sessionId}" nav argument via
 * [SavedStateHandle] (Hilt's assisted-injection support for nav args), since
 * the break schedule is looked up per session id, independent of whatever
 * session Room currently reports as "active".
 */
@HiltViewModel
class FocusModeViewModel @Inject constructor(
    private val manager: FocusSessionManager,
    private val breakManager: BreakManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    val sessionState: StateFlow<FocusSessionSnapshot?> = manager.observeCurrentSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Non-null when a scheduled break is due and hasn't been taken/deferred away yet. */
    val availableBreak: StateFlow<ScheduledBreak?> = breakManager
        .observeAvailableBreak(sessionId, manager.observeCurrentSession())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun pause(sessionId: Long) {
        viewModelScope.launch { manager.pause(sessionId) }
    }

    fun resume(sessionId: Long) {
        viewModelScope.launch { manager.resume(sessionId) }
    }

    fun endSession(sessionId: Long) {
        viewModelScope.launch {
            manager.complete(sessionId)
            FocusService.stop(context)
            FocusWatchdogScheduler.cancel(context)
        }
    }

    fun takeBreak(sessionId: Long, breakId: Long) {
        viewModelScope.launch { breakManager.takeBreak(sessionId, breakId) }
    }

    fun continueStudying(sessionId: Long) {
        viewModelScope.launch { breakManager.continueStudying(sessionId) }
    }

    fun requestUnscheduledBreak(sessionId: Long) {
        viewModelScope.launch { breakManager.requestUnscheduledBreak(sessionId) }
    }
}
