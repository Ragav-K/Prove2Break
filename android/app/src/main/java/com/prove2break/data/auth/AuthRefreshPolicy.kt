package com.prove2break.data.auth

/**
 * Pure decision logic for whether [AuthAuthenticator] should attempt a token refresh for a given
 * 401 response. Kept free of OkHttp/Retrofit types so it can be unit tested directly (see
 * AuthRefreshPolicyTest) instead of only through integration tests against real HTTP internals.
 */
object AuthRefreshPolicy {
    /** OkHttp calls `authenticate` again if we keep returning a request that still 401s; cap it. */
    const val MAX_ATTEMPTS = 2

    /**
     * @param requestPath the path of the request that received the 401 (e.g. "api/subjects").
     * @param refreshToken the currently stored refresh token, if any.
     * @param priorAttemptCount how many prior responses already chained onto this call
     *   (i.e. how many times we've already tried to authenticate this same logical request).
     */
    fun shouldAttemptRefresh(
        requestPath: String,
        refreshToken: String?,
        priorAttemptCount: Int,
    ): Boolean {
        if (refreshToken.isNullOrBlank()) return false
        if (priorAttemptCount >= MAX_ATTEMPTS) return false
        // Never try to "refresh" a 401 coming from the auth endpoints themselves (login/register
        // returning 401 means bad credentials, not an expired session; refresh returning 401
        // means the refresh token itself is invalid and is handled as an unrecoverable session).
        if (requestPath.contains("/api/auth/login") ||
            requestPath.contains("/api/auth/register") ||
            requestPath.contains("/api/auth/refresh")
        ) {
            return false
        }
        return true
    }
}
