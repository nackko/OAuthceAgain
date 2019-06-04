package com.f8full.oauthceagain.ui.login

import com.f8full.oauthceagain.data.model.RegisteredOAuthClient

/**
 * Authentication result : success (user details) or error message.
 */
data class OAuthClientRegistrationResult(
    val success: RegisteredOAuthClientView? = null,
    val error: Int? = null
)
