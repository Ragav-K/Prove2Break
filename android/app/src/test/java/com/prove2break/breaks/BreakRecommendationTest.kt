package com.prove2break.breaks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers all four documented duration tiers from BREAK-SYSTEM.md's
 * recommendation table exactly, plus the `ceil(duration / 55min)` custom
 * formula for durations outside those tiers.
 */
class BreakRecommendationTest {

    private val minute = 60_000L

    @Test
    fun `1 hour recommends 1 break at 50 minutes, 5 minutes long`() {
        val breaks = BreakRecommendation.recommend(60 * minute)

        assertEquals(1, breaks.size)
        assertEquals(50 * minute, breaks[0].offsetMillis)
        assertEquals(5 * minute, breaks[0].lengthMillis)
    }

    @Test
    fun `2 hours recommends 2 breaks at 50 and 110 minutes`() {
        val breaks = BreakRecommendation.recommend(120 * minute)

        assertEquals(2, breaks.size)
        assertEquals(50 * minute, breaks[0].offsetMillis)
        assertEquals(110 * minute, breaks[1].offsetMillis)
        // 5-10 min range
        assertTrue(breaks[0].lengthMillis in 5 * minute..10 * minute)
        assertTrue(breaks[1].lengthMillis in 5 * minute..10 * minute)
    }

    @Test
    fun `3 hours recommends 3 breaks roughly every 50 to 60 minutes at 10 minutes long`() {
        val breaks = BreakRecommendation.recommend(180 * minute)

        assertEquals(3, breaks.size)
        breaks.forEach { assertEquals(10 * minute, it.lengthMillis) }
        // Roughly every 50-60 minutes.
        val gaps = breaks.zipWithNext { a, b -> b.offsetMillis - a.offsetMillis }
        gaps.forEach { assertTrue(it in 45 * minute..65 * minute) }
    }

    @Test
    fun `4 hours recommends 4 breaks roughly every 50 to 60 minutes at 10 to 15 minutes long`() {
        val breaks = BreakRecommendation.recommend(240 * minute)

        assertEquals(4, breaks.size)
        breaks.forEach { assertTrue(it.lengthMillis in 10 * minute..15 * minute) }
        val gaps = breaks.zipWithNext { a, b -> b.offsetMillis - a.offsetMillis }
        gaps.forEach { assertTrue(it in 45 * minute..65 * minute) }
    }

    @Test
    fun `custom 90 minute duration recommends ceil(90 over 55) equals 2 breaks`() {
        val breaks = BreakRecommendation.recommend(90 * minute)

        assertEquals(2, breaks.size)
        breaks.forEach {
            assertTrue(it.offsetMillis in 0..90 * minute)
            assertTrue(it.lengthMillis in 5 * minute..10 * minute)
        }
    }

    @Test
    fun `custom 5 hour 30 minute duration recommends ceil(330 over 55) equals 6 breaks`() {
        val durationMillis = 330 * minute
        val breaks = BreakRecommendation.recommend(durationMillis)

        assertEquals(6, breaks.size)
        // Evenly spaced, strictly increasing, all within the session.
        breaks.zipWithNext { a, b -> assertTrue(b.offsetMillis > a.offsetMillis) }
        breaks.forEach { assertTrue(it.offsetMillis < durationMillis) }
    }

    @Test
    fun `custom short 40 minute duration recommends 1 break`() {
        val breaks = BreakRecommendation.recommend(40 * minute)

        assertEquals(1, breaks.size)
        assertTrue(breaks[0].offsetMillis < 40 * minute)
    }

    @Test
    fun `withUpdatedBreak and withRemovedBreak support user adjustment before session start`() {
        val original = BreakRecommendation.recommend(120 * minute)

        val edited = BreakRecommendation.withUpdatedBreak(original, index = 0, offsetMillis = 45 * minute)
        assertEquals(45 * minute, edited[0].offsetMillis)
        assertEquals(original[1], edited[1])

        val removed = BreakRecommendation.withRemovedBreak(original, index = 1)
        assertEquals(1, removed.size)
        assertEquals(original[0], removed[0])
    }
}
