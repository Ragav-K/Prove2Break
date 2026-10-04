package com.prove2break.domain.model

enum class StudyMaterialStatus { PENDING, PROCESSING, READY, FAILED }

data class StudyMaterial(
    val id: String,
    val subjectId: String,
    val filename: String,
    val status: StudyMaterialStatus,
    val failureReason: String?,
    val pageCount: Int?,
) {
    val isTerminal: Boolean get() = status == StudyMaterialStatus.READY || status == StudyMaterialStatus.FAILED
}
