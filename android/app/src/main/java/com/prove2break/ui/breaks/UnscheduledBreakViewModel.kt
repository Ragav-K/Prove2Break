package com.prove2break.ui.breaks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.breaks.BreakManager
import com.prove2break.data.repository.TopicRepository
import com.prove2break.domain.model.Topic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnscheduledBreakUiState(
    val isLoadingTopics: Boolean = true,
    val topics: List<Topic> = emptyList(),
    val selectedTopicIds: Set<String> = emptySet(),
)

/**
 * Backs the unscheduled "Break Now" flow: BREAK_VERIFICATION (select
 * claimed-complete topics via the real, backend-backed [TopicRepository] —
 * Phase 4) then QUIZ_ACTIVE, handed off to [QuizViewModel] /
 * [RapidFireQuizScreen] which run the real rapid-fire quiz and resolve the
 * result via [BreakManager.resolveQuizResult].
 */
@HiltViewModel
class UnscheduledBreakViewModel @Inject constructor(
    private val breakManager: BreakManager,
    private val topicRepository: TopicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L
    val subjectId: String = savedStateHandle.get<String>("subjectId").orEmpty()

    private val _uiState = MutableStateFlow(UnscheduledBreakUiState())
    val uiState: StateFlow<UnscheduledBreakUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { breakManager.requestUnscheduledBreak(sessionId) }
        loadTopics()
    }

    private fun loadTopics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTopics = true)
            runCatching { topicRepository.listTopics(subjectId) }
                .onSuccess { topics -> _uiState.value = _uiState.value.copy(isLoadingTopics = false, topics = topics) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoadingTopics = false, topics = emptyList()) }
        }
    }

    fun toggleTopic(topicId: String) {
        val current = _uiState.value.selectedTopicIds
        _uiState.value = _uiState.value.copy(
            selectedTopicIds = if (topicId in current) current - topicId else current + topicId,
        )
    }

    /** "Verify My Learning" — moves BREAK_VERIFICATION -> QUIZ_ACTIVE, then hands the
     * claimed topic ids to the caller so it can navigate to the real quiz screen. */
    fun startVerification(onStarted: (List<String>) -> Unit) {
        viewModelScope.launch {
            breakManager.startQuiz(sessionId)
            onStarted(_uiState.value.selectedTopicIds.toList())
        }
    }
}
