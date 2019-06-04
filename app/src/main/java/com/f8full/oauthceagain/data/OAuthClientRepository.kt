package com.f8full.oauthceagain.data

import com.f8full.oauthceagain.data.model.LoggedInUser
import com.f8full.oauthceagain.data.model.RegisteredOAuthClient

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class OAuthClientRepository(val dataSource: OAuthClientDataSource) {

    // in-memory cache of the registeredOAuthClient object
    var client: RegisteredOAuthClient? = null
        private set

    val isRegistered: Boolean
        get() = client != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        client = null
    }

    fun unregister() {
        client = null
        dataSource.unregister()
    }

    fun register(username: String): Result<RegisteredOAuthClient> {
        // handle registration
        val result = dataSource.register(username)

        if (result is Result.Success) {
            setRegisteredOAuthClient(result.data)
        }

        return result
    }

    private fun setRegisteredOAuthClient(registeredClient: RegisteredOAuthClient) {
        this.client = registeredClient
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }
}
