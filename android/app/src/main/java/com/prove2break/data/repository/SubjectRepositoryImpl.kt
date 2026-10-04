package com.prove2break.data.repository

import com.prove2break.data.remote.Prove2BreakApiService
import com.prove2break.data.remote.dto.SubjectCreateRequest
import com.prove2break.data.remote.dto.SubjectListItemDto
import com.prove2break.data.remote.dto.SubjectResponseDto
import com.prove2break.data.remote.dto.SubjectUpdateRequest
import com.prove2break.domain.model.Subject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SubjectRepositoryImpl @Inject constructor(
    private val api: Prove2BreakApiService,
) : SubjectRepository {

    override suspend fun listSubjects(): List<Subject> {
        val response = api.listSubjects()
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to load subjects")
        return response.body().orEmpty().map { it.toDomain() }
    }

    override suspend fun getSubject(subjectId: String): Subject {
        val response = api.getSubject(subjectId)
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to load subject")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain(topicsTotal = 0, topicsCompleted = 0)
    }

    override suspend fun createSubject(
        name: String,
        examName: String?,
        examDate: LocalDate?,
        description: String?,
    ): Subject {
        val response = api.createSubject(
            SubjectCreateRequest(
                name = name,
                examName = examName,
                examDate = examDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                description = description,
            ),
        )
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to create subject")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain(topicsTotal = 0, topicsCompleted = 0)
    }

    override suspend fun updateSubject(
        subjectId: String,
        name: String?,
        examName: String?,
        examDate: LocalDate?,
        description: String?,
    ): Subject {
        val response = api.updateSubject(
            subjectId,
            SubjectUpdateRequest(
                name = name,
                examName = examName,
                examDate = examDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                description = description,
            ),
        )
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to update subject")
        val body = response.body() ?: throw ApiException(response.code(), "Empty response")
        return body.toDomain(topicsTotal = 0, topicsCompleted = 0)
    }

    override suspend fun deleteSubject(subjectId: String) {
        val response = api.deleteSubject(subjectId)
        if (!response.isSuccessful) throw ApiException(response.code(), "Failed to delete subject")
    }
}

/** Thrown for any non-2xx backend response; ViewModels turn this into a user-facing message. */
class ApiException(val code: Int, message: String) : Exception(message)

internal fun SubjectListItemDto.toDomain(): Subject = Subject(
    id = id,
    name = name,
    examName = examName,
    examDate = examDate,
    description = description,
    topicsTotal = topicsTotal,
    topicsCompleted = topicsCompleted,
)

internal fun SubjectResponseDto.toDomain(topicsTotal: Int, topicsCompleted: Int): Subject = Subject(
    id = id,
    name = name,
    examName = examName,
    examDate = examDate,
    description = description,
    topicsTotal = topicsTotal,
    topicsCompleted = topicsCompleted,
)
