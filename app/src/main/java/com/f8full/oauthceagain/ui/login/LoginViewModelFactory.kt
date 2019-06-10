package com.f8full.oauthceagain.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.f8full.oauthceagain.data.CozyDataConnector
import com.f8full.oauthceagain.data.OAuthceAgainRepository

/**
 * ViewModel provider factory to instantiate OAuthceAgainViewModel.
 * Required given OAuthceAgainViewModel has a non-empty constructor
 */
class LoginViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OAuthceAgainViewModel::class.java)) {
            return OAuthceAgainViewModel(
                authceAgainRepository = OAuthceAgainRepository(
                    dataConnector = CozyDataConnector()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
