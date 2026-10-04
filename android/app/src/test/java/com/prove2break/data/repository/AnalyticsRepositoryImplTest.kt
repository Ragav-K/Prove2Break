package com.prove2break.data.repository

import com.prove2break.data.remote.dto.AnalyticsOverviewResponseDto
import com.prove2break.data.remote.dto.TopicPerformanceResponseDto
import org.junit.Assert.assertEquals
import org.junit.Test

/** DTO <-> domain mapping, matching backend/app/schemas/analytics.py field-for-field. */
class AnalyticsRepositoryImplTest {

    @Test
    fun `AnalyticsOverviewResponseDto maps to domain`() {
        val dto = AnalyticsOverviewResponseDto(
            totalAttempts = 10,
            passedAttempts = 7,
            passRate = 0.7,
            averageScore = 0.82,
            totalQuizzes = 12,
        )

        val domain = dto.toDomain()

        assertEquals(10, domain.totalAttempts)
        assertEquals(7, domain.passedAttempts)
        assertEquals(0.7, domain.passRate, 0.0001)
        assertEquals(0.82, domain.averageScore, 0.0001)
        assertEquals(12, domain.totalQuizzes)
    }

    @Test
    fun `TopicPerformanceResponseDto maps to domain`() {
        val dto = TopicPerformanceResponseDto(
            topicId = "topic-1",
            topicName = "Parsing",
            attemptCount = 3,
            averageScore = 0.61,
            lastAttemptAt = "2026-01-01T00:00:00Z",
        )

        val domain = dto.toDomain()

        assertEquals("topic-1", domain.topicId)
        assertEquals("Parsing", domain.topicName)
        assertEquals(3, domain.attemptCount)
        assertEquals(0.61, domain.averageScore, 0.0001)
        assertEquals("2026-01-01T00:00:00Z", domain.lastAttemptAt)
    }
}
