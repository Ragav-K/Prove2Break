package com.prove2break.data.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRefreshPolicyTest {

    @Test
    fun `refuses to refresh when no refresh token is stored`() {
        assertFalse(
            AuthRefreshPolicy.shouldAttemptRefresh(
                requestPath = "/api/subjects",
                refreshToken = null,
                priorAttemptCount = 1,
            ),
        )
        assertFalse(
            AuthRefreshPolicy.shouldAttemptRefresh(
                requestPath = "/api/subjects",
                refreshToken = "",
                priorAttemptCount = 1,
            ),
        )
    }

    @Test
    fun `attempts refresh for an ordinary authenticated endpoint with a token and under the cap`() {
        assertTrue(
            AuthRefreshPolicy.shouldAttemptRefresh(
                requestPath = "/api/subjects",
                refreshToken = "refresh-token",
                priorAttemptCount = 1,
            ),
        )
    }

    @Test
    fun `stops retrying once the attempt cap is reached to avoid infinite loops`() {
        assertFalse(
            AuthRefreshPolicy.shouldAttemptRefresh(
                requestPath = "/api/subjects",
                refreshToken = "refresh-token",
                priorAttemptCount = AuthRefreshPolicy.MAX_ATTEMPTS,
            ),
        )
    }

    @Test
    fun `never attempts refresh for the auth endpoints themselves`() {
        assertFalse(
            AuthRefreshPolicy.shouldAttemptRefresh("/api/auth/login", "refresh-token", 1),
        )
        assertFalse(
            AuthRefreshPolicy.shouldAttemptRefresh("/api/auth/register", "refresh-token", 1),
        )
        assertFalse(
            AuthRefreshPolicy.shouldAttemptRefresh("/api/auth/refresh", "refresh-token", 1),
        )
    }
}
