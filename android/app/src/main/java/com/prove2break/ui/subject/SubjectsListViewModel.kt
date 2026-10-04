package com.prove2break.ui.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.data.repository.SubjectRepository
import com.prove2break.domain.model.Subject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectsListUiState(
    val isLoading: Boolean = true,
    val subjects: List<Subject> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class SubjectsListViewModel @Inject constructor(
    private val repository: SubjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectsListUiState())
    val uiState: StateFlow<SubjectsListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.listSubjects() }
                .onSuccess { subjects ->
                    _uiState.value = SubjectsListUiState(isLoading = false, subjects = subjects)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load subjects",
                    )
                }
        }
    }
}
