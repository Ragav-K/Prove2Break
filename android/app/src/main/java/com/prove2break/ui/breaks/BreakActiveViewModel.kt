package com.prove2break.ui.breaks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.breaks.BreakManager
import com.prove2break.breaks.BreakTimerSnapshot
import com.prove2break.domain.model.BreakBudget
import com.prove2break.domain.model.ScheduledBreak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [BreakActiveScreen]: the live break countdown (derived from
 * timestamps via [com.prove2break.breaks.BreakTimer], never a decrementing
 * counter — same discipline as the focus timer), plus "End Break Early" and
 * bounded "Extend Break".
 */
@HiltViewModel
class BreakActiveViewModel @Inject constructor(
    private val breakManager: BreakManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L

    val activeBreak: StateFlow<Pair<ScheduledBreak, BreakTimerSnapshot>?> = breakManager
        .observeActiveBreakTimer(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _budget = MutableStateFlow<BreakBudget?>(null)
    val budget: StateFlow<BreakBudget?> = _budget.asStateFlow()

    private val _extensionDenied = MutableStateFlow(false)
    /** True right after an extension request was rejected for exceeding budget — non-judgmental, dismissable. */
    val extensionDenied: StateFlow<Boolean> = _extensionDenied.asStateFlow()

    init {
        refreshBudget()
        // Auto-reconcile: when the break's own timer completes, resolve it to
        // FOCUSING just like FocusService reconciles the focus timer — this
        // is timer completion, not an auto-start of anything new, so it does
        // not violate BREAK-SYSTEM.md's "continue studying never auto-starts
        // a break" rule (that rule governs entering BREAK_ACTIVE, not leaving it).
        viewModelScope.launch {
            activeBreak.collect { current ->
                if (current != null && current.second.isComplete) {
                    breakManager.endBreak(sessionId, current.first.id)
                }
            }
        }
    }

    private fun refreshBudget() {
        viewModelScope.launch {
            _budget.value = breakManager.getBudget(sessionId)
        }
    }

    fun endBreakEarly() {
        val breakId = activeBreak.value?.first?.id ?: return
        viewModelScope.launch {
            breakManager.endBreak(sessionId, breakId)
        }
    }

    fun extendBreak() {
        val breakId = activeBreak.value?.first?.id ?: return
        viewModelScope.launch {
            val allowed = breakManager.extendBreak(breakId)
            _extensionDenied.value = !allowed
            refreshBudget()
        }
    }

    fun dismissExtensionDenied() {
        _extensionDenied.value = false
    }
}
