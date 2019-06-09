package com.f8full.oauthceagain.data.model

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 * // If user credentials will be cached in local storage, it is recommended it be encrypted
// @see https://developer.android.com/training/articles/keystore
 */
data class UserCredentialTokens(
    val accessToken: String,
    val refreshToken: String//,
    //val idToken: String
)
