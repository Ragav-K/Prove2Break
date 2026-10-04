package com.prove2break.ui.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.data.repository.SubjectRepository
import com.prove2break.domain.usecase.CreateSubjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SubjectEditUiState(
    val subjectId: String? = null,
    val name: String = "",
    val examName: String = "",
    val examDate: LocalDate? = null,
    val description: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
) {
    val isEditMode: Boolean get() = subjectId != null
}

@HiltViewModel
class SubjectEditViewModel @Inject constructor(
    private val repository: SubjectRepository,
    private val createSubjectUseCase: CreateSubjectUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editingSubjectId: String? = savedStateHandle.get<String>("subjectId")

    private val _uiState = MutableStateFlow(SubjectEditUiState(subjectId = editingSubjectId))
    val uiState: StateFlow<SubjectEditUiState> = _uiState.asStateFlow()

    init {
        editingSubjectId?.let { id ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                runCatching { repository.getSubject(id) }
                    .onSuccess { subject ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            name = subject.name,
                            examName = subject.examName.orEmpty(),
                            examDate = subject.examDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                            description = subject.description.orEmpty(),
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load subject",
                        )
                    }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onExamNameChange(value: String) {
        _uiState.value = _uiState.value.copy(examName = value)
    }

    fun onExamDateChange(value: LocalDate?) {
        _uiState.value = _uiState.value.copy(examDate = value)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Name is required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = runCatching {
                val examName = state.examName.ifBlank { null }
                val description = state.description.ifBlank { null }
                if (state.isEditMode) {
                    repository.updateSubject(state.subjectId!!, state.name, examName, state.examDate, description)
                } else {
                    createSubjectUseCase(state.name, examName, state.examDate, description)
                }
            }
            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Failed to save subject",
                    )
                }
        }
    }
}
