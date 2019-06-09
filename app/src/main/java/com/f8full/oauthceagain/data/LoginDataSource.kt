package com.f8full.oauthceagain.data

import com.f8full.oauthceagain.data.model.LoggedInUser
import com.f8full.oauthceagain.data.model.UserCredentialTokens
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.*
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(username: String, password: String): Result<LoggedInUser> {
        try {
            // TODO: handle loggedInUser authentication
            val fakeUser = LoggedInUser(java.util.UUID.randomUUID().toString(), "Jane Doe")
            return Result.Success(fakeUser)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }

    fun exchangeAuthCodeForTokenCouple(
        cozyBaseUrlString: String,
        redirectIntentData: String,
        clientId: String,
        clientSecret: String,
        authRequestState: State): Result<UserCredentialTokens>{

        var authResp: AuthenticationResponse? = null

        try {
            authResp = AuthenticationResponseParser.parse(URI(redirectIntentData.removeSuffix("#")))
        } catch (e: ParseException){
            return Result.Error(e)

        }catch (e: URISyntaxException){
            return Result.Error(e)
        }

        if (authResp is AuthenticationErrorResponse){
            val error = authResp.errorObject
            return Result.Error(IOException("Error while authenticating : ${error.toJSONObject()}"))
        }

        val successResponse = authResp as AuthenticationSuccessResponse

        /* Don't forget to check the state!
 * The state in the received authentication response must match the state
 * specified in the previous outgoing authentication request.
 *
 *
*/
        if (successResponse.state != authRequestState){
            return Result.Error(IOException("Auth request state validation failed"))
        }

        val authCode = successResponse.authorizationCode

        val tokenReq = TokenRequest(
            URI("$cozyBaseUrlString/auth/access_token"),
            ClientSecretBasic(
                ClientID(clientId),
                Secret(clientSecret)
            ),
            AuthorizationCodeGrant(authCode, URI("findmybikes://com.f8full.oauthceagain.oauth2redirect"))
        )

        var tokenHTTPResp: HTTPResponse? = null
        try {
            //TODO: Find how to properly construct the TokenRequest object so that the request doesn't have to be altered here
            val truc = tokenReq.toHTTPRequest()

            truc.query = "${truc.query}&client_id=$clientId&client_secret=$clientSecret"
            //tokenHTTPResp = tokenReq.toHTTPRequest().send()
            tokenHTTPResp = truc.send()
        } catch (e: SerializeException) {
            return Result.Error(e)
        } catch (e: IOException) {
            return Result.Error(e)
        }


// Parse and check response
        var tokenResponse: TokenResponse? = null
        try {
            tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp!!)
        } catch (e: ParseException) {
            return Result.Error(e)
        }


        if (tokenResponse is TokenErrorResponse) {
            val error = tokenResponse.errorObject
            return Result.Error(IOException("Error while authenticating : ${error.toJSONObject()}"))
        }

        val accessTokenResponse = tokenResponse as OIDCTokenResponse?

        return Result.Success(UserCredentialTokens(
            accessToken = accessTokenResponse?.oidcTokens?.accessToken?.value!!,
            refreshToken = accessTokenResponse.oidcTokens?.refreshToken?.value!!//,
            //idToken = accessTokenResponse.oidcTokens?.idTokenString!!
            ))
    }
}

