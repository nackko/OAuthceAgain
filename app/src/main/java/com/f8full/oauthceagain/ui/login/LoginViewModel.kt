package com.f8full.oauthceagain.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import com.f8full.oauthceagain.data.LoginRepository
import com.f8full.oauthceagain.data.Result

import com.f8full.oauthceagain.R
import com.f8full.oauthceagain.data.OAuthClientRepository
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.Nonce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI

class LoginViewModel(private val loginRepository: LoginRepository,
                     private val authClientRepository: OAuthClientRepository) : ViewModel() {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _OAuthClientregistrationResult = MutableLiveData<OAuthClientRegistrationResult>()
    val clientRegistrationResult: LiveData<OAuthClientRegistrationResult> = _OAuthClientregistrationResult

    private val _authenticationUri = MutableLiveData<URI>()
    val authenticaitonUri: LiveData<URI> = _authenticationUri

    fun registerOAuthClient(username: String){

        coroutineScopeIO.launch {
            val result = authClientRepository.register(username)

            if (result is Result.Success) {
                _OAuthClientregistrationResult.postValue(OAuthClientRegistrationResult(success =
                RegisteredOAuthClientView( registrationAccessToken = result.data.registrationAccessToken,
                    clientId = result.data.clientId))
                )
            } else {
                _OAuthClientregistrationResult.postValue(OAuthClientRegistrationResult( error =
                R.string.registration_failed))
            }
        }
    }

    fun unregisterAuthclient() {

        coroutineScopeIO.launch {
            val result = authClientRepository.unregister()

            if (result is Result.Success){
                _OAuthClientregistrationResult.postValue(null)
                Log.i("TAG", "OAuth client deleted")
            }
        }
    }

    fun isRegistered(): Boolean{
        return authClientRepository.isRegistered
    }

    fun authenticate(){
        //we just publish URI for Activity consumption
        // Generate random state string for pairing the response to the request
        val state = State()
// Generate nonce
        val nonce = Nonce()
// Specify scope
        val scope = Scope.parse("openid io.cozy.files io.cozy.oauth.clients")

// Compose the request
        val authenticationRequest = AuthenticationRequest(
            URI("https://f8full.mycozy.cloud/auth/authorize"),
            ResponseType(ResponseType.Value.CODE),
            scope, ClientID(clientRegistrationResult.value?.success?.clientId), URI("findmybikes://com.f8full.oauthceagain.oauth2redirect"), state, nonce
        )


        _authenticationUri.value = authenticationRequest.toURI()
        //authenticationRequest.

    }

    fun login(username: String, password: String) {
        // can be launched in a separate asynchronous job
        val result = loginRepository.login(username, password)

        if (result is Result.Success) {
            _loginResult.value = LoginResult(success = LoggedInUserView(displayName = result.data.displayName))
        } else {
            _loginResult.value = LoginResult(error = R.string.login_failed)
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } /*else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } */else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains('@')) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5;
    }
}
