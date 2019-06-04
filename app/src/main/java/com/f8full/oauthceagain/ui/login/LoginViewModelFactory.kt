package com.f8full.oauthceagain.ui.login

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.f8full.oauthceagain.data.LoginDataSource
import com.f8full.oauthceagain.data.LoginRepository
import com.f8full.oauthceagain.data.OAuthClientDataSource
import com.f8full.oauthceagain.data.OAuthClientRepository

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
class LoginViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                loginRepository = LoginRepository(
                    dataSource = LoginDataSource()
                ),
                authClientRepository = OAuthClientRepository(
                    dataSource = OAuthClientDataSource()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
