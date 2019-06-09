package com.f8full.oauthceagain.ui.login

/**
 * Authentication result : success (user details) or error message.
 */
data class AuthLoginResult(
    val success: AuthLoggedInUserView? = null,
    val error: Int? = null
)
