package com.prove2break.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signals an app-wide, unrecoverable end of session (refresh token missing/expired/rejected).
 *
 * [AuthAuthenticator] calls [forceLogout] when a 401 cannot be resolved by refreshing; a
 * top-level observer (see [com.prove2break.ui.navigation.Prove2BreakNavHost]) collects
 * [forceLogoutEvents] and navigates back to Login per docs/AUTHENTICATION.md's "Session
 * Expiration Handling" — no silent retries against a dead session.
 */
@Singleton
class AuthSessionManager @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val _forceLogoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val forceLogoutEvents: SharedFlow<Unit> = _forceLogoutEvents.asSharedFlow()

    suspend fun forceLogout() {
        tokenStore.clear()
        _forceLogoutEvents.tryEmit(Unit)
    }
}
