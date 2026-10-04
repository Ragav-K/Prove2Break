package com.prove2break.data.auth

import com.prove2break.data.remote.Prove2BreakApiService
import com.prove2break.data.remote.dto.RefreshRequest
import com.prove2break.di.PlainClient
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * OkHttp [Authenticator]: on a 401, attempts one token refresh and retries the original request
 * with the new access token, per docs/AUTHENTICATION.md's Session Expiration Handling. If the
 * refresh token is also invalid/expired, tokens are cleared and [AuthSessionManager] notifies the
 * rest of the app so it can route back to Login.
 *
 * [refreshApi] is built from a plain OkHttpClient (see [com.prove2break.di.PlainClient] in
 * NetworkModule) with no [AuthInterceptor]/[AuthAuthenticator] attached, so calling it here can
 * never recurse back into this authenticator.
 */
class AuthAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val sessionManager: AuthSessionManager,
    @PlainClient private val refreshApi: Prove2BreakApiService,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val requestPath = response.request.url.encodedPath
        val refreshToken = runBlocking { tokenStore.getRefreshToken() }
        val priorAttemptCount = countResponses(response)

        if (!AuthRefreshPolicy.shouldAttemptRefresh(requestPath, refreshToken, priorAttemptCount)) {
            return null
        }

        return runBlocking {
            try {
                val refreshResponse = refreshApi.refresh(RefreshRequest(refreshToken!!))
                val newAccessToken = refreshResponse.body()?.accessToken
                if (refreshResponse.isSuccessful && !newAccessToken.isNullOrBlank()) {
                    tokenStore.updateAccessToken(newAccessToken)
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                } else if (refreshResponse.code() == 401) {
                    // Refresh token itself is invalid/expired: session is unrecoverable.
                    sessionManager.forceLogout()
                    null
                } else {
                    // Transient failure (5xx/etc.) — don't destroy the session, just give up on
                    // this particular request.
                    null
                }
            } catch (e: Exception) {
                // Network failure while refreshing — leave tokens intact, just fail this request.
                null
            }
        }
    }

    private fun countResponses(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
