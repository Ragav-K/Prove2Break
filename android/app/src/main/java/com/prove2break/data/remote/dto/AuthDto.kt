package com.prove2break.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Mirrors backend/app/schemas/auth.py RegisterRequest. */
data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

/** Mirrors backend/app/schemas/auth.py RegisterResponse. */
data class RegisterResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
)

/** Mirrors backend/app/schemas/auth.py LoginRequest. */
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

/** Mirrors backend/app/schemas/auth.py TokenResponse (POST /api/auth/login). */
data class TokenResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
)

/** Mirrors backend/app/schemas/auth.py RefreshRequest. */
data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String,
)

/** Mirrors backend/app/schemas/auth.py AccessTokenResponse (POST /api/auth/refresh). */
data class AccessTokenResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
)
