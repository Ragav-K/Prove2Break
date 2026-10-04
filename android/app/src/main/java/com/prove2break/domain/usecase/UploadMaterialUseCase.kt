package com.prove2break.domain.usecase

import com.prove2break.data.repository.MaterialRepository
import com.prove2break.domain.model.StudyMaterial
import com.prove2break.domain.model.StudyMaterialStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed class UploadMaterialState {
    data object Uploading : UploadMaterialState()
    data class Polling(val material: StudyMaterial) : UploadMaterialState()
    data class Ready(val material: StudyMaterial) : UploadMaterialState()
    data class Failed(val material: StudyMaterial?, val message: String) : UploadMaterialState()
}

/**
 * Uploads a PDF for a subject, then polls GET /api/materials/{id}/status until it reaches
 * a terminal state (READY or FAILED), per docs' documented async processing flow.
 */
class UploadMaterialUseCase @Inject constructor(
    private val repository: MaterialRepository,
) {
    operator fun invoke(
        subjectId: String,
        filename: String,
        bytes: ByteArray,
        pollIntervalMillis: Long = 1500L,
        maxPolls: Int = 40,
    ): Flow<UploadMaterialState> = flow {
        emit(UploadMaterialState.Uploading)
        val uploaded = try {
            repository.uploadMaterial(subjectId, filename, bytes)
        } catch (e: Exception) {
            emit(UploadMaterialState.Failed(null, e.message ?: "Upload failed"))
            return@flow
        }

        if (uploaded.isTerminal) {
            emitTerminal(uploaded)
            return@flow
        }

        var current = uploaded
        var polls = 0
        while (!current.isTerminal && polls < maxPolls) {
            emit(UploadMaterialState.Polling(current))
            delay(pollIntervalMillis)
            current = try {
                repository.getMaterialStatus(uploaded.id).copy(
                    subjectId = uploaded.subjectId,
                    filename = uploaded.filename,
                    pageCount = current.pageCount,
                )
            } catch (e: Exception) {
                emit(UploadMaterialState.Failed(current, e.message ?: "Status check failed"))
                return@flow
            }
            polls++
        }

        if (current.isTerminal) {
            emitTerminal(current)
        } else {
            emit(UploadMaterialState.Failed(current, "Processing is taking longer than expected"))
        }
    }

}

private suspend fun FlowCollector<UploadMaterialState>.emitTerminal(material: StudyMaterial) {
    if (material.status == StudyMaterialStatus.READY) {
        emit(UploadMaterialState.Ready(material))
    } else {
        emit(UploadMaterialState.Failed(material, material.failureReason ?: "Processing failed"))
    }
}
