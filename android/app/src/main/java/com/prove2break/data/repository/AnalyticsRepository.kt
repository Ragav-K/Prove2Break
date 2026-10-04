package com.prove2break.data.repository

import com.prove2break.domain.model.AnalyticsOverview
import com.prove2break.domain.model.TopicPerformanceSummary

/** Backend-backed source for quiz/topic-performance analytics (docs/ANALYTICS.md). */
interface AnalyticsRepository {
    suspend fun getOverview(): AnalyticsOverview
    suspend fun getTopicPerformance(): List<TopicPerformanceSummary>
    suspend fun getWeakTopics(): List<TopicPerformanceSummary>
}
