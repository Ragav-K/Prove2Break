package com.prove2break.ui.analytics

import com.prove2break.domain.model.TopicPerformanceSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Pins down the weak-topic threshold copy (docs/PRODUCT.md "encouraging,
 * non-judgmental copy" / docs/ANALYTICS.md dashboard composition) and the
 * percent/duration formatting helpers.
 */
class AnalyticsScreenKtTest {

    @Test
    fun `weak topic message is gentle and names the topic and next step`() {
        val topic = TopicPerformanceSummary(
            topicId = "t1",
            topicName = "Parsing",
            attemptCount = 2,
            averageScore = 0.61,
            lastAttemptAt = null,
        )

        val message = weakTopicMessage(topic)

        assertEquals("Parsing — 61%. Revise Parsing before your next focus session.", message)
        // Never shame the student — no judgmental language.
        listOf("fail", "bad", "weak", "wrong", "poor").forEach { forbidden ->
            assertFalse(message.lowercase().contains(forbidden))
        }
    }

    @Test
    fun `formatPercent rounds to nearest whole percent`() {
        assertEquals("61%", formatPercent(0.611))
        assertEquals("70%", formatPercent(0.70))
        assertEquals("0%", formatPercent(0.0))
    }

    @Test
    fun `formatDuration renders hours and minutes`() {
        assertEquals("1h 5m", formatDuration(65 * 60_000L))
        assertEquals("45m", formatDuration(45 * 60_000L))
    }
}
