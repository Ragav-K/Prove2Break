package com.prove2break.data.repository

import com.prove2break.data.remote.dto.SubjectListItemDto
import com.prove2break.data.remote.dto.SubjectResponseDto
import org.junit.Assert.assertEquals
import org.junit.Test

class SubjectMappingTest {

    @Test
    fun `SubjectListItemDto maps every field including progress counts`() {
        val dto = SubjectListItemDto(
            id = "subj-1",
            name = "Biology",
            examName = "Finals",
            examDate = "2026-05-01",
            description = "Cell biology unit",
            createdAt = "2026-01-01T00:00:00Z",
            topicsTotal = 10,
            topicsCompleted = 4,
        )

        val subject = dto.toDomain()

        assertEquals("subj-1", subject.id)
        assertEquals("Biology", subject.name)
        assertEquals("Finals", subject.examName)
        assertEquals("2026-05-01", subject.examDate)
        assertEquals("Cell biology unit", subject.description)
        assertEquals(10, subject.topicsTotal)
        assertEquals(4, subject.topicsCompleted)
    }

    @Test
    fun `SubjectListItemDto maps null optional fields through unchanged`() {
        val dto = SubjectListItemDto(
            id = "subj-2",
            name = "Physics",
            examName = null,
            examDate = null,
            description = null,
            createdAt = "2026-01-01T00:00:00Z",
            topicsTotal = 0,
            topicsCompleted = 0,
        )

        val subject = dto.toDomain()

        assertEquals(null, subject.examName)
        assertEquals(null, subject.examDate)
        assertEquals(null, subject.description)
    }

    @Test
    fun `SubjectResponseDto maps with caller-supplied progress counts`() {
        val dto = SubjectResponseDto(
            id = "subj-3",
            name = "Chemistry",
            examName = "Midterm",
            examDate = "2026-03-15",
            description = null,
            createdAt = "2026-01-01T00:00:00Z",
        )

        val subject = dto.toDomain(topicsTotal = 5, topicsCompleted = 2)

        assertEquals("subj-3", subject.id)
        assertEquals("Chemistry", subject.name)
        assertEquals(5, subject.topicsTotal)
        assertEquals(2, subject.topicsCompleted)
    }
}
