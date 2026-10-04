package com.prove2break.data.repository

import com.prove2break.domain.model.Subject
import java.time.LocalDate

/** Single source of truth for subjects — a thin pass-through to the backend (no local cache yet). */
interface SubjectRepository {
    suspend fun listSubjects(): List<Subject>
    suspend fun getSubject(subjectId: String): Subject
    suspend fun createSubject(
        name: String,
        examName: String?,
        examDate: LocalDate?,
        description: String?,
    ): Subject
    suspend fun updateSubject(
        subjectId: String,
        name: String?,
        examName: String?,
        examDate: LocalDate?,
        description: String?,
    ): Subject
    suspend fun deleteSubject(subjectId: String)
}
