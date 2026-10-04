package com.prove2break.domain.usecase

import com.prove2break.data.repository.MaterialRepository
import com.prove2break.domain.model.StudyMaterial
import com.prove2break.domain.model.StudyMaterialStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fake repository whose [getMaterialStatus] returns a scripted sequence of statuses
 * (e.g. PROCESSING, PROCESSING, READY), standing in for the backend's async PDF
 * processing pipeline (backend/app/api/materials.py upload + status endpoints).
 */
private class FakeMaterialRepository(
    private val uploadResult: Result<StudyMaterial> = Result.success(
        StudyMaterial("mat-1", "subj-1", "notes.pdf", StudyMaterialStatus.PENDING, null, null),
    ),
    private val statusSequence: List<StudyMaterialStatus> = emptyList(),
) : MaterialRepository {
    private var statusIndex = 0

    override suspend fun uploadMaterial(subjectId: String, filename: String, bytes: ByteArray): StudyMaterial {
        return uploadResult.getOrThrow()
    }

    override suspend fun getMaterial(materialId: String): StudyMaterial {
        throw UnsupportedOperationException("not used by UploadMaterialUseCase")
    }

    override suspend fun getMaterialStatus(materialId: String): StudyMaterial {
        val status = statusSequence[statusIndex]
        statusIndex++
        return StudyMaterial(materialId, "subj-1", "", status, if (status == StudyMaterialStatus.FAILED) "bad pdf" else null, null)
    }
}

class UploadMaterialUseCaseTest {

    @Test
    fun `upload that is immediately READY skips polling and emits Uploading then Ready`() = runBlocking {
        val repository = FakeMaterialRepository(
            uploadResult = Result.success(
                StudyMaterial("mat-1", "subj-1", "notes.pdf", StudyMaterialStatus.READY, null, 5),
            ),
        )
        val useCase = UploadMaterialUseCase(repository)

        val states = useCase("subj-1", "notes.pdf", byteArrayOf(1, 2, 3)).toList()

        assertEquals(2, states.size)
        assertTrue(states[0] is UploadMaterialState.Uploading)
        assertTrue(states[1] is UploadMaterialState.Ready)
        assertEquals("mat-1", (states[1] as UploadMaterialState.Ready).material.id)
    }

    @Test
    fun `polls PROCESSING until READY then stops`() = runBlocking {
        val repository = FakeMaterialRepository(
            statusSequence = listOf(StudyMaterialStatus.PROCESSING, StudyMaterialStatus.PROCESSING, StudyMaterialStatus.READY),
        )
        val useCase = UploadMaterialUseCase(repository)

        val states = useCase("subj-1", "notes.pdf", byteArrayOf(1), pollIntervalMillis = 0L).toList()

        // Uploading, Polling(PENDING), Polling(PROCESSING), Polling(PROCESSING), Ready
        assertTrue(states.first() is UploadMaterialState.Uploading)
        assertTrue(states.last() is UploadMaterialState.Ready)
        val pollingCount = states.count { it is UploadMaterialState.Polling }
        assertEquals(3, pollingCount)
    }

    @Test
    fun `polling that resolves to FAILED emits Failed with the backend failure reason`() = runBlocking {
        val repository = FakeMaterialRepository(
            statusSequence = listOf(StudyMaterialStatus.PROCESSING, StudyMaterialStatus.FAILED),
        )
        val useCase = UploadMaterialUseCase(repository)

        val states = useCase("subj-1", "notes.pdf", byteArrayOf(1), pollIntervalMillis = 0L).toList()

        val last = states.last()
        assertTrue(last is UploadMaterialState.Failed)
        assertEquals("bad pdf", (last as UploadMaterialState.Failed).message)
    }

    @Test
    fun `upload failure short-circuits without polling`() = runBlocking {
        val repository = FakeMaterialRepository(uploadResult = Result.failure(RuntimeException("network down")))
        val useCase = UploadMaterialUseCase(repository)

        val states = useCase("subj-1", "notes.pdf", byteArrayOf(1)).toList()

        assertEquals(2, states.size)
        assertTrue(states[1] is UploadMaterialState.Failed)
        assertEquals("network down", (states[1] as UploadMaterialState.Failed).message)
    }

    @Test
    fun `exceeding maxPolls while still non-terminal emits a timeout Failed state`() = runBlocking {
        val repository = FakeMaterialRepository(
            statusSequence = listOf(StudyMaterialStatus.PROCESSING, StudyMaterialStatus.PROCESSING, StudyMaterialStatus.PROCESSING),
        )
        val useCase = UploadMaterialUseCase(repository)

        val states = useCase("subj-1", "notes.pdf", byteArrayOf(1), pollIntervalMillis = 0L, maxPolls = 3).toList()

        val last = states.last()
        assertTrue(last is UploadMaterialState.Failed)
    }
}
