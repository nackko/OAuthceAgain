package com.f8full.oauthceagain.data

import com.f8full.oauthceagain.data.model.RegisteredOAuthClient
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class OAuthClientDataSource {

    fun register(username: String): Result<RegisteredOAuthClient> {
        try {
            // TODO: handle loggedInUser authentication
            val fakeOAuthClient = RegisteredOAuthClient("clientId", "clientSecret", "regToken")
            return Result.Success(fakeOAuthClient)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun unregister() {
        // TODO: revoke authentication
    }

    fun update(){

    }

    fun info(){

    }
}

