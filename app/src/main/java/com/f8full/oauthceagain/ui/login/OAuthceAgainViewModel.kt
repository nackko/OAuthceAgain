package com.f8full.oauthceagain.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import com.f8full.oauthceagain.data.Result

import com.f8full.oauthceagain.R
import com.f8full.oauthceagain.data.OAuthceAgainRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI

class OAuthceAgainViewModel(private val authceAgainRepository: OAuthceAgainRepository) : ViewModel() {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _authLoginResult = MutableLiveData<AuthLoginResult>()
    val authLoginResult: LiveData<AuthLoginResult> = _authLoginResult

    @Suppress("PrivatePropertyName")
    private val _OAuthClientRegistrationResult = MutableLiveData<OAuthClientRegistrationResult>()
    val clientRegistrationResult: LiveData<OAuthClientRegistrationResult> = _OAuthClientRegistrationResult

    private val _authenticationUri = MutableLiveData<URI>()
    val authenticationUri: LiveData<URI> = _authenticationUri

    //Maybe this should also live in repo after repos fusion ?
    private val _cozyBaseUrlString = MutableLiveData<String>()
    val cozyBaseUrlString: LiveData<String> = _cozyBaseUrlString

    fun registerOAuthClient(cozyUrlUserInput: String){

        val finalUrl = getCozyUrl(cozyUrlUserInput)

        //TODO: display final URL in interface
        _cozyBaseUrlString.value = finalUrl

        coroutineScopeIO.launch {
            val result = authceAgainRepository.register(finalUrl)

            if (result is Result.Success) {
                _OAuthClientRegistrationResult.postValue(
                    OAuthClientRegistrationResult(
                        success =
                        LoggedOAuthClientView(
                            registrationAccessToken = result.data.registrationAccessToken,
                            clientId = result.data.clientId,
                            clientSecret = result.data.clientSecret
                        )
                    )
                )
            } else {
                _OAuthClientRegistrationResult.postValue(
                    OAuthClientRegistrationResult(
                        error =
                        R.string.registration_failed
                    )
                )
            }
        }
    }

    private fun getCozyUrl(userInput: String): String {
        return if(!userInput.contains(".")) {
            "https://$userInput.mycozy.cloud"
        } else if(!userInput.contains("https://") && !userInput.contains("http://")){
            "https://$userInput"
        } else{
            userInput
        }
    }

    fun unregisterAuthclient() {

        cozyBaseUrlString.value?.let {
            coroutineScopeIO.launch {
                val result = authceAgainRepository.unregister(it)

                if (result is Result.Success){
                    _OAuthClientRegistrationResult.postValue(null)
                    Log.i("TAG", "OAuth client deleted")
                }
            }
        }
    }

    fun isRegistered(): Boolean{
        return authceAgainRepository.isRegistered
    }

    fun authenticate(){

        _cozyBaseUrlString.value?.let {
            coroutineScopeIO.launch {

                val result = authceAgainRepository.buildAuthenticationUri(it, authceAgainRepository.client)

                if(result is Result.Success){
                    _authenticationUri.postValue(result.data)
                }

            }
        }
    }

    fun retrieveAccessTokenAndRefreshToken(redirectIntentData: String) {

        cozyBaseUrlString.value?.let {
            coroutineScopeIO.launch {

                //TODO: merge everything to have a single repo and a single data source (which is cozy data source)
                val result = authceAgainRepository.exchangeAuthCodeForTokenCouple(
                    it,
                    redirectIntentData,
                    authceAgainRepository.client?.clientId!!,
                    authceAgainRepository.client?.clientSecret!!
                )

                if (result is Result.Success){
                    _authLoginResult.postValue(AuthLoginResult(AuthLoggedInUserView(result.data.accessToken, result.data.refreshToken)))
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //From login sample wizard

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
