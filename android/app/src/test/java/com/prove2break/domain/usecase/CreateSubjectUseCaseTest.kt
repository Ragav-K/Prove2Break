package com.prove2break.domain.usecase

import com.prove2break.data.repository.SubjectRepository
import com.prove2break.domain.model.Subject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

private class FakeSubjectRepository : SubjectRepository {
    var lastName: String? = null
    var lastExamName: String? = null

    override suspend fun listSubjects(): List<Subject> = emptyList()

    override suspend fun getSubject(subjectId: String): Subject {
        throw UnsupportedOperationException()
    }

    override suspend fun createSubject(name: String, examName: String?, examDate: LocalDate?, description: String?): Subject {
        lastName = name
        lastExamName = examName
        return Subject(id = "subj-1", name = name, examName = examName, examDate = examDate?.toString(), description = description, topicsTotal = 0, topicsCompleted = 0)
    }

    override suspend fun updateSubject(subjectId: String, name: String?, examName: String?, examDate: LocalDate?, description: String?): Subject {
        throw UnsupportedOperationException()
    }

    override suspend fun deleteSubject(subjectId: String) = Unit
}

class CreateSubjectUseCaseTest {

    @Test
    fun `blank name is rejected`() {
        val useCase = CreateSubjectUseCase(FakeSubjectRepository())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase("   ", null, null, null) }
        }
    }

    @Test
    fun `blank optional fields are normalized to null`() = runBlocking {
        val repository = FakeSubjectRepository()
        val useCase = CreateSubjectUseCase(repository)

        useCase("Biology", "  ", null, null)

        assertEquals("Biology", repository.lastName)
        assertEquals(null, repository.lastExamName)
    }
}
