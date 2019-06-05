package com.f8full.oauthceagain.ui.login

/**
 * User details post authentication that is exposed to the UI
 */
data class RegisteredOAuthClientView(
    val registrationAccessToken: String,
    val clientId: String
    //... other data fields that may be accessible to the UI
)
