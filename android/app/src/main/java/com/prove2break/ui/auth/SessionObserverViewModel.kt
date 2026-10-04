package com.prove2break.ui.auth

import androidx.lifecycle.ViewModel
import com.prove2break.data.auth.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * Thin ViewModel wrapper so [Prove2BreakNavHost][com.prove2break.ui.navigation.Prove2BreakNavHost]
 * (a plain composable, not a Hilt entry point) can observe [AuthSessionManager.forceLogoutEvents]
 * and navigate back to Login when a session becomes unrecoverable (refresh token expired/invalid).
 */
@HiltViewModel
class SessionObserverViewModel @Inject constructor(
    sessionManager: AuthSessionManager,
) : ViewModel() {
    val forceLogoutEvents: SharedFlow<Unit> = sessionManager.forceLogoutEvents
}
