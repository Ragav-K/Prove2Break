package com.prove2break.data.repository

import com.prove2break.data.local.BlockedAppDao
import com.prove2break.data.local.BlockedAppEntity
import com.prove2break.domain.model.BlockedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BlockedAppRepositoryImpl @Inject constructor(
    private val dao: BlockedAppDao,
) : BlockedAppRepository {

    override fun observeAll(): Flow<List<BlockedApp>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSelected(): Flow<List<BlockedApp>> =
        dao.observeSelected().map { list -> list.map { it.toDomain() } }

    override suspend fun getSelectedPackageNames(): Set<String> =
        dao.getSelected().map { it.packageName }.toSet()

    override suspend fun setSelected(packageName: String, appLabel: String, selected: Boolean) {
        dao.upsert(BlockedAppEntity(packageName = packageName, appLabel = appLabel, isSelected = selected))
    }

    private fun BlockedAppEntity.toDomain() = BlockedApp(
        packageName = packageName,
        appLabel = appLabel,
        isSelected = isSelected,
    )
}
