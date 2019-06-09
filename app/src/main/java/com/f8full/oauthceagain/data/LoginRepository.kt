package com.f8full.oauthceagain.data

import com.f8full.oauthceagain.data.model.LoggedInUser
import com.f8full.oauthceagain.data.model.RegisteredOAuthClient
import com.f8full.oauthceagain.data.model.UserCredentialTokens
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.Nonce
import java.net.URI

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository(val dataSource: LoginDataSource) {

    // in-memory cache of the loggedInUser object
    var user: LoggedInUser? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = null
    }

    fun logout() {
        user = null
        dataSource.logout()
    }

    // in-memory cache of the authRequest State object
    var authRequestState: State? = null
        private set
    // in-memory cache of the authRequest Nonce object
    var authRequestNonce: Nonce? = null
        private set

    var userCred: UserCredentialTokens? = null
        private set

    fun login(username: String, password: String): Result<LoggedInUser> {
        // handle login
        val result = dataSource.login(username, password)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    private fun setLoggedInUser(loggedInUser: LoggedInUser) {
        this.user = loggedInUser
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }

    private fun setUserCredentials(userCred: UserCredentialTokens){
        this.userCred = userCred
    }

    fun exchangeAuthCodeForTokenCouple(
        cozyBaseUrlString: String,
        redirectIntentData: String,
        clientID: String,
        clientSecret: String):
            Result<UserCredentialTokens>{

        val result = dataSource.exchangeAuthCodeForTokenCouple(
            cozyBaseUrlString,
            redirectIntentData,
            clientID,
            clientSecret,
            authRequestState!!)

        if (result is Result.Success){
            setUserCredentials(result.data)
        }

        return result
    }

    fun buildAuthenticationUri(cozyBaseUrlString: String, authClientId: RegisteredOAuthClient?): Result<URI> {

        //we just publish URI for Activity consumption
        // Generate random state string for pairing the response to the request
        authRequestState = State()
// Generate nonce
        authRequestNonce = Nonce()
// Specify scope
        //TODO: custom scope from UI
        val scope = Scope.parse("openid io.cozy.files io.cozy.oauth.clients")

// Compose the request
        val authenticationRequest = AuthenticationRequest(
            URI("$cozyBaseUrlString/auth/authorize"),
            ResponseType(ResponseType.Value.CODE),
            scope, ClientID(authClientId?.clientId), URI("findmybikes://com.f8full.oauthceagain.oauth2redirect"),
            authRequestState,
            authRequestNonce
        )

        return Result.Success(authenticationRequest.toURI())

    }
}
