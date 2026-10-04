package com.prove2break.data.repository

import com.prove2break.domain.model.StudyMaterial

interface MaterialRepository {
    suspend fun uploadMaterial(subjectId: String, filename: String, bytes: ByteArray): StudyMaterial
    suspend fun getMaterial(materialId: String): StudyMaterial
    suspend fun getMaterialStatus(materialId: String): StudyMaterial
}
