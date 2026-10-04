package com.prove2break.domain.model

/**
 * Plain domain model — no Room/Retrofit annotations, per ANDROID-ARCHITECTURE.md.
 * Subjects are server-owned resources (Phase 4 Study Management backend), so
 * [id] is the backend UUID string, not a local Room primary key.
 */
data class Subject(
    val id: String,
    val name: String,
    val examName: String?,
    val examDate: String?,
    val description: String?,
    val topicsTotal: Int,
    val topicsCompleted: Int,
)
