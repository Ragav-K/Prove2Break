package com.prove2break.domain.model

/** Backend-owned quiz aggregate stats (docs/ANALYTICS.md). */
data class AnalyticsOverview(
    val totalAttempts: Int,
    val passedAttempts: Int,
    val passRate: Double,
    val averageScore: Double,
    val totalQuizzes: Int,
) {
    companion object {
        val EMPTY = AnalyticsOverview(0, 0, 0.0, 0.0, 0)
    }
}

/** Backend-owned per-topic performance row (mirrors TopicPerformance). */
data class TopicPerformanceSummary(
    val topicId: String,
    val topicName: String?,
    val attemptCount: Int,
    val averageScore: Double,
    val lastAttemptAt: String?,
)
