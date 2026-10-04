package com.prove2break.ui.apprestriction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.data.repository.BlockedAppRepository
import com.prove2break.data.repository.InstalledAppsRepository
import com.prove2break.domain.model.InstalledAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSelectionUiState(
    val isLoading: Boolean = true,
    val apps: List<InstalledAppInfo> = emptyList(),
    val selectedPackageNames: Set<String> = emptySet(),
)

/** Backs the app-selection screen: lists installed apps, persists checkbox state to Room. */
@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    private val installedAppsRepository: InstalledAppsRepository,
    private val blockedAppRepository: BlockedAppRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSelectionUiState())
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val apps = installedAppsRepository.getInstallableApps()
            val selected = blockedAppRepository.observeSelected().first().map { it.packageName }.toSet()
            _uiState.value = AppSelectionUiState(isLoading = false, apps = apps, selectedPackageNames = selected)
        }
    }

    fun toggle(app: InstalledAppInfo) {
        val newlySelected = app.packageName !in _uiState.value.selectedPackageNames
        viewModelScope.launch {
            blockedAppRepository.setSelected(app.packageName, app.label, newlySelected)
        }
        _uiState.value = _uiState.value.let { state ->
            val updated = state.selectedPackageNames.toMutableSet()
            if (newlySelected) updated.add(app.packageName) else updated.remove(app.packageName)
            state.copy(selectedPackageNames = updated)
        }
    }
}
