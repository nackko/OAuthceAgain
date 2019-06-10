package com.f8full.oauthceagain.ui.login

/**
 * User details post authentication that is exposed to the UI
 * // If user credentials will be cached in local storage, it is recommended it be encrypted
// @see https://developer.android.com/training/articles/keystore
 */
data class LoggedOAuthClientView(
    val registrationAccessToken: String,
    val clientId: String,
    val clientSecret: String
    //... other data fields that may be accessible to the UI
)
