package com.prove2break.ui.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.breaks.BreakManager
import com.prove2break.data.repository.SubjectRepository
import com.prove2break.domain.model.Subject
import com.prove2break.domain.usecase.StartFocusSessionUseCase
import com.prove2break.focus.FocusService
import com.prove2break.focus.FocusSessionManager
import com.prove2break.focus.FocusWatchdogScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusSetupUiState(
    val isLoadingSubjects: Boolean = true,
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
)

/**
 * Backs the duration-picker Focus Setup screen. Subjects are now real,
 * server-backed resources (Phase 4 Study Management) — this loads the
 * user's subjects via [SubjectRepository] and requires one to be selected
 * before a session can start, instead of the previous hardcoded
 * `subjectId = 1L` placeholder.
 */
@HiltViewModel
class FocusSetupViewModel @Inject constructor(
    private val manager: FocusSessionManager,
    private val breakManager: BreakManager,
    private val subjectRepository: SubjectRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusSetupUiState())
    val uiState: StateFlow<FocusSetupUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    fun loadSubjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSubjects = true)
            runCatching { subjectRepository.listSubjects() }
                .onSuccess { subjects ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingSubjects = false,
                        subjects = subjects,
                        selectedSubjectId = _uiState.value.selectedSubjectId ?: subjects.firstOrNull()?.id,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingSubjects = false, subjects = emptyList())
                }
        }
    }

    fun selectSubject(subjectId: String) {
        _uiState.value = _uiState.value.copy(selectedSubjectId = subjectId)
    }

    fun startSession(plannedDurationMillis: Long, onStarted: (Long) -> Unit, onError: (String) -> Unit) {
        val subjectId = _uiState.value.selectedSubjectId
        if (subjectId == null) {
            onError("Create a subject before starting a focus session")
            return
        }
        viewModelScope.launch {
            runCatching {
                require(plannedDurationMillis in StartFocusSessionUseCase.MIN_DURATION_MILLIS..StartFocusSessionUseCase.MAX_DURATION_MILLIS) {
                    "Duration must be between 15 minutes and 8 hours"
                }
                val sessionId = manager.start(subjectId = subjectId, plannedDurationMillis = plannedDurationMillis)
                breakManager.scheduleBreaksForSession(sessionId, plannedDurationMillis)
                sessionId
            }.onSuccess { sessionId ->
                FocusService.start(context)
                FocusWatchdogScheduler.schedule(context)
                onStarted(sessionId)
            }.onFailure { error ->
                onError(error.message ?: "Could not start focus session")
            }
        }
    }
}
