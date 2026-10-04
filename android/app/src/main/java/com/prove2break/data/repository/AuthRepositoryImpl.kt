package com.prove2break.data.repository

import com.prove2break.data.auth.TokenStore
import com.prove2break.data.remote.Prove2BreakApiService
import com.prove2break.data.remote.dto.LoginRequest
import com.prove2break.data.remote.dto.RegisterRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: Prove2BreakApiService,
    private val tokenStore: TokenStore,
) : AuthRepository {

    override fun observeIsAuthenticated(): Flow<Boolean> = tokenStore.isAuthenticatedFlow

    override suspend fun register(email: String, password: String) {
        val response = api.register(RegisterRequest(email = email, password = password))
        if (!response.isSuccessful) {
            throw ApiException(response.code(), registerErrorMessage(response.code()))
        }
        login(email, password)
    }

    override suspend fun login(email: String, password: String) {
        val response = api.login(LoginRequest(email = email, password = password))
        if (!response.isSuccessful) {
            throw ApiException(response.code(), loginErrorMessage(response.code()))
        }
        val body = response.body() ?: throw ApiException(response.code(), "Empty response from server")
        tokenStore.saveTokens(body.accessToken, body.refreshToken)
    }

    override suspend fun logout() {
        tokenStore.clear()
    }
}

private fun registerErrorMessage(code: Int): String = when (code) {
    409 -> "An account with this email already exists"
    422 -> "Please enter a valid email and a password of at least 8 characters"
    else -> "Registration failed. Please try again"
}

private fun loginErrorMessage(code: Int): String = when (code) {
    401 -> "Invalid email or password"
    else -> "Login failed. Please try again"
}
