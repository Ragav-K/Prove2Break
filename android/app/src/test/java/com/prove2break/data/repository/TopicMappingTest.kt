package com.prove2break.data.repository

import com.prove2break.data.remote.dto.TopicCompletionStatusDto
import com.prove2break.data.remote.dto.TopicDifficultyDto
import com.prove2break.data.remote.dto.TopicResponseDto
import com.prove2break.domain.model.TopicCompletionStatus
import com.prove2break.domain.model.TopicDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicMappingTest {

    @Test
    fun `TopicResponseDto maps all fields including nullable unit and performance`() {
        val dto = TopicResponseDto(
            id = "topic-1",
            subjectId = "subj-1",
            unitId = "unit-1",
            name = "Photosynthesis",
            description = "Light and dark reactions",
            sourceReference = "p.42",
            completionStatus = TopicCompletionStatusDto.COMPLETED,
            difficulty = TopicDifficultyDto.MEDIUM,
            performance = 0.85,
        )

        val topic = dto.toDomain()

        assertEquals("topic-1", topic.id)
        assertEquals("unit-1", topic.unitId)
        assertEquals("Photosynthesis", topic.name)
        assertEquals(TopicCompletionStatus.COMPLETED, topic.completionStatus)
        assertEquals(TopicDifficulty.MEDIUM, topic.difficulty)
        assertEquals(0.85, topic.performance)
        assertTrue(topic.isCompleted)
    }

    @Test
    fun `topic with null unit and performance maps to null, not defaults`() {
        val dto = TopicResponseDto(
            id = "topic-2",
            subjectId = "subj-1",
            unitId = null,
            name = "Osmosis",
            description = null,
            sourceReference = null,
            completionStatus = TopicCompletionStatusDto.NOT_STARTED,
            difficulty = TopicDifficultyDto.EASY,
            performance = null,
        )

        val topic = dto.toDomain()

        assertEquals(null, topic.unitId)
        assertEquals(null, topic.performance)
        assertTrue(!topic.isCompleted)
    }

    @Test
    fun `IN_PROGRESS is not completed`() {
        val dto = TopicResponseDto(
            id = "topic-3",
            subjectId = "subj-1",
            unitId = null,
            name = "Mitosis",
            description = null,
            sourceReference = null,
            completionStatus = TopicCompletionStatusDto.IN_PROGRESS,
            difficulty = TopicDifficultyDto.HARD,
            performance = null,
        )

        assertTrue(!dto.toDomain().isCompleted)
    }
}
