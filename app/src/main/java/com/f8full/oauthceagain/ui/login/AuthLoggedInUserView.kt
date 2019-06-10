package com.f8full.oauthceagain.ui.login

/**
 * Created by F8Full on 2019-06-10. This file is part of OAuthceAgain
 *
 */
/**
 * User details post authentication that is exposed to the UI
 */
data class AuthLoggedInUserView(
    val accesstoken: String,
    val refreshToken: String
    //... other data fields that may be accessible to the UI
)
