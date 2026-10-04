package com.prove2break.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Mirrors backend/app/models/topic.py TopicCompletionStatus. */
enum class TopicCompletionStatusDto {
    @SerializedName("NOT_STARTED") NOT_STARTED,
    @SerializedName("IN_PROGRESS") IN_PROGRESS,
    @SerializedName("COMPLETED") COMPLETED,
}

/** Mirrors backend/app/models/topic.py TopicDifficulty. */
enum class TopicDifficultyDto {
    @SerializedName("EASY") EASY,
    @SerializedName("MEDIUM") MEDIUM,
    @SerializedName("HARD") HARD,
}

/** Mirrors backend/app/schemas/topics.py TopicResponse. */
data class TopicResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("subject_id") val subjectId: String,
    @SerializedName("unit_id") val unitId: String?,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("source_reference") val sourceReference: String?,
    @SerializedName("completion_status") val completionStatus: TopicCompletionStatusDto,
    @SerializedName("difficulty") val difficulty: TopicDifficultyDto,
    @SerializedName("performance") val performance: Double?,
)

/** Mirrors backend/app/schemas/topics.py TopicUpdate. */
data class TopicUpdateRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("is_completed") val isCompleted: Boolean? = null,
)
