package com.prove2break.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prove2break.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Backs the Splash screen: checks [AuthRepository.observeIsAuthenticated] (backed by whether an
 * access token exists in [com.prove2break.data.auth.AuthTokenStore]) so the NavHost can route to
 * Dashboard or Login on startup, per ANDROID-ARCHITECTURE.md.
 *
 * `null` means "still checking"; the Splash screen waits for a non-null value before navigating.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {
    val isAuthenticated: StateFlow<Boolean?> = authRepository.observeIsAuthenticated()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
