package com.prove2break.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the access/refresh token pair and exposes whether a session is currently active. */
interface TokenStore {
    /** Emits `true` whenever an access token is present, `false` right after [clear]. */
    val isAuthenticatedFlow: Flow<Boolean>

    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?

    /** Persists both tokens returned by POST /api/auth/login (or register-then-login). */
    suspend fun saveTokens(accessToken: String, refreshToken: String)

    /** Persists only the new access token returned by POST /api/auth/refresh. */
    suspend fun updateAccessToken(accessToken: String)

    /** Clears both tokens — used on logout and on unrecoverable (refresh-token-expired) sessions. */
    suspend fun clear()
}

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

/**
 * DataStore-backed [TokenStore].
 *
 * Deviation from docs/AUTHENTICATION.md ("Secure Storage on Android" calls for
 * EncryptedSharedPreferences or an encrypted DataStore): this project does not yet depend on
 * `androidx.security:security-crypto`, and adding it is out of scope for this change. Tokens are
 * stored in a dedicated (non-encrypted) Preferences DataStore, isolated from the app's other
 * settings, and are never logged. Migrating this store to an encrypted implementation later only
 * requires changing this class — everything else depends on the [TokenStore] interface.
 */
@Singleton
class AuthTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : TokenStore {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    private val accessTokenFlow: Flow<String?> =
        context.authDataStore.data.map { it[accessTokenKey] }

    override val isAuthenticatedFlow: Flow<Boolean> =
        accessTokenFlow.map { !it.isNullOrBlank() }

    override suspend fun getAccessToken(): String? = accessTokenFlow.first()

    override suspend fun getRefreshToken(): String? =
        context.authDataStore.data.map { it[refreshTokenKey] }.first()

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.authDataStore.edit {
            it[accessTokenKey] = accessToken
            it[refreshTokenKey] = refreshToken
        }
    }

    override suspend fun updateAccessToken(accessToken: String) {
        context.authDataStore.edit { it[accessTokenKey] = accessToken }
    }

    override suspend fun clear() {
        context.authDataStore.edit {
            it.remove(accessTokenKey)
            it.remove(refreshTokenKey)
        }
    }
}
