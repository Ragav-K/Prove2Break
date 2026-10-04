package com.prove2break.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Mirrors backend/app/schemas/analytics.py AnalyticsOverviewResponse. */
data class AnalyticsOverviewResponseDto(
    @SerializedName("total_attempts") val totalAttempts: Int,
    @SerializedName("passed_attempts") val passedAttempts: Int,
    @SerializedName("pass_rate") val passRate: Double,
    @SerializedName("average_score") val averageScore: Double,
    @SerializedName("total_quizzes") val totalQuizzes: Int,
)

/** Mirrors backend/app/schemas/analytics.py TopicPerformanceResponse. */
data class TopicPerformanceResponseDto(
    @SerializedName("topic_id") val topicId: String,
    @SerializedName("topic_name") val topicName: String?,
    @SerializedName("attempt_count") val attemptCount: Int,
    @SerializedName("average_score") val averageScore: Double,
    @SerializedName("last_attempt_at") val lastAttemptAt: String?,
)

/** Mirrors backend/app/schemas/analytics.py TopicPerformanceListResponse. */
data class TopicPerformanceListResponseDto(
    @SerializedName("topics") val topics: List<TopicPerformanceResponseDto>,
)

/** Mirrors backend/app/schemas/analytics.py WeakTopicsResponse. */
data class WeakTopicsResponseDto(
    @SerializedName("threshold") val threshold: Double,
    @SerializedName("topics") val topics: List<TopicPerformanceResponseDto>,
)
