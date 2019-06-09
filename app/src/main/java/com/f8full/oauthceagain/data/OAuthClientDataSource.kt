package com.f8full.oauthceagain.data

import com.f8full.oauthceagain.data.model.RegisteredOAuthClient
import com.f8full.oauthceagain.data.model.UserCredentialTokens
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import java.io.IOException
import com.nimbusds.oauth2.sdk.client.ClientRegistrationErrorResponse
import com.nimbusds.oauth2.sdk.client.ClientDeleteRequest
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils
import com.nimbusds.openid.connect.sdk.*
import com.nimbusds.openid.connect.sdk.rp.*
import java.net.URI
import java.net.URISyntaxException


/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class OAuthClientDataSource {

    fun register(cozyBaseUrlString: String): Result<RegisteredOAuthClient> {

        val jsonMetadata =
            "{\"redirect_uris\":[\"findmybikes://com.f8full.oauthceagain.oauth2redirect\"],\"client_name\":\"#findmybikes\",\"software_id\":\"github.com/f8full/findmybikesTRUC\",\"software_version\":\"999\",\"client_kind\":\"mobile\",\"client_uri\":\"https://client.example.org/\",\"logo_uri\":\"https://client.example.org/logo.svg\",\"policy_uri\":\"https://client/example.org/policy\"}"
        val metadata = OIDCClientMetadata.parse(JSONObjectUtils.parse(jsonMetadata))

// Make registration request
        val registrationRequest =
            OIDCClientRegistrationRequest(URI("$cozyBaseUrlString/auth/register"), metadata, null)

        val truc = registrationRequest.toHTTPRequest()

        truc.accept = "application/json"

        val regHTTPResponse = truc.send()

// Parse and check response
        val registrationResponse = OIDCClientRegistrationResponseParser.parse(regHTTPResponse)

        if (registrationResponse is ClientRegistrationErrorResponse) {
            val error = registrationResponse
                .errorObject
            return Result.Error(IOException("Error registering client : ${error.toJSONObject()}"))
        }

        val clientInformation = (registrationResponse as OIDCClientInformationResponse).oidcClientInformation

        return Result.Success(
            RegisteredOAuthClient(
                clientId = clientInformation.id.value,
                clientSecret = clientInformation.secret.value,
                registrationAccessToken = clientInformation.registrationAccessToken.value
            )
        )
    }

    fun unregister(cozyBaseUrlString:String,
                   clientId: String,
        masterAccessToken: String): Result<Boolean> {


        val req = ClientDeleteRequest(URI("$cozyBaseUrlString/auth/register/$clientId"),
            BearerAccessToken(masterAccessToken)
        )

        val deleteReponse = req.toHTTPRequest().send()

        if (! deleteReponse.indicatesSuccess()) {
            // We have an error
            val error = ClientRegistrationErrorResponse.parse(deleteReponse)
                .errorObject
            return Result.Error(IOException("Error registering client : ${error.toJSONObject()}"))
        }

        return Result.Success(true)
    }

    fun update(){

    }

    fun info(){

    }

    fun exchangeAuthCodeForTokenCouple(
        cozyBaseUrlString: String,
        redirectIntentData: String,
        clientId: String,
        clientSecret: String,
        authRequestState: State
    ): Result<UserCredentialTokens>{

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

        return Result.Success(
            UserCredentialTokens(
                accessToken = accessTokenResponse?.oidcTokens?.accessToken?.value!!,
                refreshToken = accessTokenResponse.oidcTokens?.refreshToken?.value!!//,
                //idToken = accessTokenResponse.oidcTokens?.idTokenString!!
            )
        )
    }
}

