package com.prove2break.ui.material

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.data.repository.TopicRepository
import com.prove2break.domain.model.Topic
import com.prove2break.domain.usecase.RenameTopicUseCase
import com.prove2break.domain.usecase.ToggleTopicCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicListUiState(
    val isLoading: Boolean = true,
    val topics: List<Topic> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class TopicListViewModel @Inject constructor(
    private val repository: TopicRepository,
    private val toggleTopicCompletionUseCase: ToggleTopicCompletionUseCase,
    private val renameTopicUseCase: RenameTopicUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val subjectId: String = requireNotNull(savedStateHandle.get<String>("subjectId")) {
        "TopicListViewModel requires a subjectId argument"
    }

    private val _uiState = MutableStateFlow(TopicListUiState())
    val uiState: StateFlow<TopicListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.listTopics(subjectId) }
                .onSuccess { topics -> _uiState.value = TopicListUiState(isLoading = false, topics = topics) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load topics",
                    )
                }
        }
    }

    /** Optimistically flips completion locally, then confirms/reconciles against the server. */
    fun toggleCompletion(topic: Topic) {
        val previous = _uiState.value.topics
        _uiState.value = _uiState.value.copy(
            topics = previous.map { if (it.id == topic.id) it.copy(completionStatus = flippedStatus(it)) else it },
        )
        viewModelScope.launch {
            runCatching { toggleTopicCompletionUseCase(topic) }
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        topics = _uiState.value.topics.map { if (it.id == updated.id) updated else it },
                    )
                }
                .onFailure { error ->
                    // Roll back the optimistic update on failure.
                    _uiState.value = _uiState.value.copy(
                        topics = previous,
                        errorMessage = error.message ?: "Failed to update topic",
                    )
                }
        }
    }

    fun renameTopic(topicId: String, newName: String) {
        viewModelScope.launch {
            runCatching { renameTopicUseCase(topicId, newName) }
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        topics = _uiState.value.topics.map { if (it.id == updated.id) updated else it },
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(errorMessage = error.message ?: "Failed to rename topic")
                }
        }
    }

    private fun flippedStatus(topic: Topic) =
        if (topic.isCompleted) com.prove2break.domain.model.TopicCompletionStatus.NOT_STARTED
        else com.prove2break.domain.model.TopicCompletionStatus.COMPLETED
}
