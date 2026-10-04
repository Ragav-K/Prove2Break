package com.prove2break.data.repository

import kotlinx.coroutines.flow.Flow

/** Single source of truth for the user's session (register/login/logout + auth state). */
interface AuthRepository {
    /** Emits `true` whenever a stored access token exists. Drives session-aware navigation. */
    fun observeIsAuthenticated(): Flow<Boolean>

    /**
     * Registers a new account, then logs in with the same credentials so the user lands
     * authenticated (docs/AUTHENTICATION.md: "client proceeds to login flow (or auto-logs-in
     * ... decided at implementation)" — this implementation auto-logs-in).
     */
    suspend fun register(email: String, password: String)

    /** Logs in and persists the returned access/refresh tokens. */
    suspend fun login(email: String, password: String)

    /** Clears local tokens. There is no backend `/api/auth/logout` endpoint yet (see AUTHENTICATION.md deviation note), so this is local-only. */
    suspend fun logout()
}
