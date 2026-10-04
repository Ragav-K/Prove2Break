package com.prove2break.data.repository

import com.prove2break.data.remote.Prove2BreakApiService
import com.prove2break.data.remote.dto.AnalyticsOverviewResponseDto
import com.prove2break.data.remote.dto.TopicPerformanceResponseDto
import com.prove2break.domain.model.AnalyticsOverview
import com.prove2break.domain.model.TopicPerformanceSummary
import javax.inject.Inject

class AnalyticsRepositoryImpl @Inject constructor(
    private val api: Prove2BreakApiService,
) : AnalyticsRepository {

    override suspend fun getOverview(): AnalyticsOverview {
        val response = api.getAnalyticsOverview()
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to load analytics overview")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain()
    }

    override suspend fun getTopicPerformance(): List<TopicPerformanceSummary> {
        val response = api.getTopicPerformance()
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to load topic performance")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.topics.map { it.toDomain() }
    }

    override suspend fun getWeakTopics(): List<TopicPerformanceSummary> {
        val response = api.getWeakTopics()
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to load weak topics")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.topics.map { it.toDomain() }
    }
}

internal fun AnalyticsOverviewResponseDto.toDomain(): AnalyticsOverview = AnalyticsOverview(
    totalAttempts = totalAttempts,
    passedAttempts = passedAttempts,
    passRate = passRate,
    averageScore = averageScore,
    totalQuizzes = totalQuizzes,
)

internal fun TopicPerformanceResponseDto.toDomain(): TopicPerformanceSummary = TopicPerformanceSummary(
    topicId = topicId,
    topicName = topicName,
    attemptCount = attemptCount,
    averageScore = averageScore,
    lastAttemptAt = lastAttemptAt,
)
