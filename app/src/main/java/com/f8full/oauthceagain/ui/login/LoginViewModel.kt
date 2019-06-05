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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(private val loginRepository: LoginRepository,
                     private val authClientRepository: OAuthClientRepository) : ViewModel() {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _OAuthClientregistrationResult = MutableLiveData<OAuthClientRegistrationResult>()
    val clientRegistrationResult: LiveData<OAuthClientRegistrationResult> = _OAuthClientregistrationResult

    fun registerOAuthClient(username: String){

        coroutineScopeIO.launch {
            val result = authClientRepository.register(username)

            if (result is Result.Success) {
                _OAuthClientregistrationResult.postValue(OAuthClientRegistrationResult(success =
                RegisteredOAuthClientView( registrationAccessToken = result.data.registrationAccessToken))
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
