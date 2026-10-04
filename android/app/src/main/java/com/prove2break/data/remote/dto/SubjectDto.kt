package com.prove2break.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Mirrors backend/app/schemas/subjects.py SubjectResponse. */
data class SubjectResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("exam_name") val examName: String?,
    @SerializedName("exam_date") val examDate: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("created_at") val createdAt: String,
)

/** Mirrors backend/app/schemas/subjects.py SubjectListItem (SubjectResponse + progress). */
data class SubjectListItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("exam_name") val examName: String?,
    @SerializedName("exam_date") val examDate: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("topics_total") val topicsTotal: Int,
    @SerializedName("topics_completed") val topicsCompleted: Int,
)

/** Mirrors backend/app/schemas/subjects.py SubjectCreate. All fields optional except name. */
data class SubjectCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("exam_name") val examName: String? = null,
    @SerializedName("exam_date") val examDate: String? = null,
    @SerializedName("description") val description: String? = null,
)

/** Mirrors backend/app/schemas/subjects.py SubjectUpdate. Every field optional. */
data class SubjectUpdateRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("exam_name") val examName: String? = null,
    @SerializedName("exam_date") val examDate: String? = null,
    @SerializedName("description") val description: String? = null,
)
