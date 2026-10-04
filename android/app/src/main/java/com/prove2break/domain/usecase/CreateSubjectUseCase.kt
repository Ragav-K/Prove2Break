package com.prove2break.domain.usecase

import com.prove2break.data.repository.SubjectRepository
import com.prove2break.domain.model.Subject
import java.time.LocalDate
import javax.inject.Inject

class CreateSubjectUseCase @Inject constructor(
    private val repository: SubjectRepository,
) {
    suspend operator fun invoke(
        name: String,
        examName: String?,
        examDate: LocalDate?,
        description: String?,
    ): Subject {
        require(name.isNotBlank()) { "Subject name cannot be blank" }
        return repository.createSubject(name.trim(), examName?.trim()?.ifBlank { null }, examDate, description?.trim()?.ifBlank { null })
    }
}
