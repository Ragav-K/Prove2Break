package com.prove2break.data.repository

import com.prove2break.domain.model.BlockedApp
import kotlinx.coroutines.flow.Flow

/** Single source of truth for which installed apps the student has chosen to restrict. */
interface BlockedAppRepository {
    fun observeAll(): Flow<List<BlockedApp>>
    fun observeSelected(): Flow<List<BlockedApp>>
    suspend fun getSelectedPackageNames(): Set<String>
    suspend fun setSelected(packageName: String, appLabel: String, selected: Boolean)
}
