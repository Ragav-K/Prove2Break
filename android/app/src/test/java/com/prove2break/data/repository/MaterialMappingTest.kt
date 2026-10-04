package com.prove2break.data.repository

import com.prove2break.data.remote.dto.StudyMaterialResponseDto
import com.prove2break.data.remote.dto.StudyMaterialStatusDto
import com.prove2break.data.remote.dto.StudyMaterialStatusResponseDto
import com.prove2break.domain.model.StudyMaterialStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialMappingTest {

    @Test
    fun `StudyMaterialResponseDto maps all fields and status enum`() {
        val dto = StudyMaterialResponseDto(
            id = "mat-1",
            subjectId = "subj-1",
            filename = "notes.pdf",
            status = StudyMaterialStatusDto.READY,
            failureReason = null,
            pageCount = 12,
            uploadedAt = "2026-01-01T00:00:00Z",
        )

        val material = dto.toDomain()

        assertEquals("mat-1", material.id)
        assertEquals("subj-1", material.subjectId)
        assertEquals("notes.pdf", material.filename)
        assertEquals(StudyMaterialStatus.READY, material.status)
        assertEquals(12, material.pageCount)
        assertTrue(material.isTerminal)
    }

    @Test
    fun `PENDING and PROCESSING are not terminal, READY and FAILED are`() {
        assertTrue(!StudyMaterialStatusDto.PENDING.toDomain().let { it == StudyMaterialStatus.READY || it == StudyMaterialStatus.FAILED })
        assertEquals(StudyMaterialStatus.PROCESSING, StudyMaterialStatusDto.PROCESSING.toDomain())
        assertEquals(StudyMaterialStatus.READY, StudyMaterialStatusDto.READY.toDomain())
        assertEquals(StudyMaterialStatus.FAILED, StudyMaterialStatusDto.FAILED.toDomain())
    }

    @Test
    fun `StudyMaterialStatusResponseDto maps failure reason through`() {
        val dto = StudyMaterialStatusResponseDto(
            id = "mat-2",
            status = StudyMaterialStatusDto.FAILED,
            failureReason = "Unsupported PDF encoding",
        )

        val material = dto.toDomain()

        assertEquals("mat-2", material.id)
        assertEquals(StudyMaterialStatus.FAILED, material.status)
        assertEquals("Unsupported PDF encoding", material.failureReason)
        assertTrue(material.isTerminal)
    }
}
