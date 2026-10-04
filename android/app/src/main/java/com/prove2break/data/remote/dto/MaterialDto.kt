package com.prove2break.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Mirrors backend/app/models/study_material.py StudyMaterialStatus. */
enum class StudyMaterialStatusDto {
    @SerializedName("PENDING") PENDING,
    @SerializedName("PROCESSING") PROCESSING,
    @SerializedName("READY") READY,
    @SerializedName("FAILED") FAILED,
}

/** Mirrors backend/app/schemas/materials.py StudyMaterialResponse. */
data class StudyMaterialResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("subject_id") val subjectId: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("status") val status: StudyMaterialStatusDto,
    @SerializedName("failure_reason") val failureReason: String?,
    @SerializedName("page_count") val pageCount: Int?,
    @SerializedName("uploaded_at") val uploadedAt: String,
)

/** Mirrors backend/app/schemas/materials.py StudyMaterialStatusResponse. */
data class StudyMaterialStatusResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: StudyMaterialStatusDto,
    @SerializedName("failure_reason") val failureReason: String?,
)
