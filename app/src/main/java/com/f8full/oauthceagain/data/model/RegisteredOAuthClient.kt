package com.f8full.oauthceagain.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
data class RegisteredOAuthClient(
    val clientId: String,
    val clientSecret: String,
    val registrationAccessToken: String
)
