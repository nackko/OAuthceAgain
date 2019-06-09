package com.f8full.oauthceagain.data

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

    fun unregister(cozyBaseUrlString: String): Result<Boolean> {
        val result = dataSource.unregister(
            cozyBaseUrlString = cozyBaseUrlString,
            clientId = client?.clientId!!,
            masterAccessToken = client?.registrationAccessToken!!)

        if (result is Result.Success) {
            this.client = null
        }

        return result
    }

    fun register(cozyBaseUrlString: String): Result<RegisteredOAuthClient> {
        // handle registration
        val result = dataSource.register(cozyBaseUrlString)

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

    // in-memory cache of the authRequest State object
    private var authRequestState: State? = null
    // in-memory cache of the authRequest Nonce object
    var authRequestNonce: Nonce? = null
        private set

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    var userCred: UserCredentialTokens? = null
        private set

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
