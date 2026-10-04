package com.prove2break.ui.material

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.data.repository.SubjectRepository
import com.prove2break.domain.model.StudyMaterial
import com.prove2break.domain.model.Subject
import com.prove2break.domain.usecase.UploadMaterialState
import com.prove2break.domain.usecase.UploadMaterialUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectDetailUiState(
    val isLoading: Boolean = true,
    val subject: Subject? = null,
    val materials: List<StudyMaterial> = emptyList(),
    val uploadState: UploadMaterialState? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val uploadMaterialUseCase: UploadMaterialUseCase,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val subjectId: String = requireNotNull(savedStateHandle.get<String>("subjectId")) {
        "SubjectDetailViewModel requires a subjectId argument"
    }

    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    init {
        loadSubject()
    }

    fun loadSubject() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { subjectRepository.getSubject(subjectId) }
                .onSuccess { subject ->
                    _uiState.value = _uiState.value.copy(isLoading = false, subject = subject)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load subject",
                    )
                }
        }
    }

    /** Reads the picked document's bytes via the app's ContentResolver and starts the upload/poll flow. */
    fun uploadMaterial(uri: android.net.Uri) {
        viewModelScope.launch {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes == null) {
                _uiState.value = _uiState.value.copy(
                    uploadState = UploadMaterialState.Failed(null, "Could not read the selected file"),
                )
                return@launch
            }
            val filename = queryFilename(uri) ?: "material.pdf"
            uploadMaterialUseCase(subjectId, filename, bytes).collect { state ->
                _uiState.value = _uiState.value.copy(uploadState = state)
                if (state is UploadMaterialState.Ready) {
                    _uiState.value = _uiState.value.copy(
                        materials = _uiState.value.materials + state.material,
                    )
                }
            }
        }
    }

    fun clearUploadState() {
        _uiState.value = _uiState.value.copy(uploadState = null)
    }

    private fun queryFilename(uri: android.net.Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        }.getOrNull()
    }
}
