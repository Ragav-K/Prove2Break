package com.prove2break.data.repository

import com.prove2break.data.remote.Prove2BreakApiService
import com.prove2break.data.remote.dto.StudyMaterialResponseDto
import com.prove2break.data.remote.dto.StudyMaterialStatusDto
import com.prove2break.data.remote.dto.StudyMaterialStatusResponseDto
import com.prove2break.domain.model.StudyMaterial
import com.prove2break.domain.model.StudyMaterialStatus
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class MaterialRepositoryImpl @Inject constructor(
    private val api: Prove2BreakApiService,
) : MaterialRepository {

    override suspend fun uploadMaterial(subjectId: String, filename: String, bytes: ByteArray): StudyMaterial {
        val subjectIdBody = subjectId.toRequestBody("text/plain".toMediaType())
        val filePart = MultipartBody.Part.createFormData(
            "file",
            filename,
            bytes.toRequestBody("application/pdf".toMediaType()),
        )
        val response = api.uploadMaterial(subjectIdBody, filePart)
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to upload material")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain()
    }

    override suspend fun getMaterial(materialId: String): StudyMaterial {
        val response = api.getMaterial(materialId)
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to load material")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain()
    }

    override suspend fun getMaterialStatus(materialId: String): StudyMaterial {
        val response = api.getMaterialStatus(materialId)
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to load material status")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain()
    }
}

internal fun StudyMaterialResponseDto.toDomain(): StudyMaterial = StudyMaterial(
    id = id,
    subjectId = subjectId,
    filename = filename,
    status = status.toDomain(),
    failureReason = failureReason,
    pageCount = pageCount,
)

/**
 * [StudyMaterialStatusResponseDto] carries only id/status/failure_reason per the backend
 * schema. Callers polling status (see UploadMaterialUseCase) already hold the filename and
 * subjectId from the initial upload response, so those fields are left blank here rather
 * than faked.
 */
internal fun StudyMaterialStatusResponseDto.toDomain(): StudyMaterial = StudyMaterial(
    id = id,
    subjectId = "",
    filename = "",
    status = status.toDomain(),
    failureReason = failureReason,
    pageCount = null,
)

internal fun StudyMaterialStatusDto.toDomain(): StudyMaterialStatus = when (this) {
    StudyMaterialStatusDto.PENDING -> StudyMaterialStatus.PENDING
    StudyMaterialStatusDto.PROCESSING -> StudyMaterialStatus.PROCESSING
    StudyMaterialStatusDto.READY -> StudyMaterialStatus.READY
    StudyMaterialStatusDto.FAILED -> StudyMaterialStatus.FAILED
}
