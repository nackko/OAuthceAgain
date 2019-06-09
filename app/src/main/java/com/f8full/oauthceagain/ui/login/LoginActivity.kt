package com.f8full.oauthceagain.ui.login

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*

import com.f8full.oauthceagain.R
import androidx.browser.customtabs.CustomTabsClient
import android.content.ComponentName
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import android.net.Uri
import android.util.Log


class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val action = intent?.action
        val data = intent?.dataString

        if (action == Intent.ACTION_VIEW && data != null){
            Log.e("LoginActivity", "Intent data stirng : $data")
            loginViewModel.retrieveAccessTokenAndRefreshToken(data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.username)
        val cozyUrl = findViewById<TextView>(R.id.final_url)
        val clientInfo = findViewById<TextView>(R.id.client_info)
        val accessToken = findViewById<TextView>(R.id.access_token)
        val refreshToken = findViewById<TextView>(R.id.refresh_token)
        val registering = findViewById<Button>(R.id.registering)
        val authenticate = findViewById<Button>(R.id.authenticate)
        registering.isEnabled = true
        val loading = findViewById<ProgressBar>(R.id.loading)

        loginViewModel = ViewModelProviders.of(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            registering.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            /*if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }*/
        })

        loginViewModel.clientRegistrationResult.observe(this@LoginActivity, Observer {
            if (it == null) {
                clientInfo.text = getString(R.string.client_info_default)
                accessToken.text = ""
                refreshToken.text = ""
                loading.visibility = View.INVISIBLE
            }

            val registrationResult = it ?: return@Observer

            loading.visibility = View.GONE

            if (registrationResult.error != null){

            }

            if (registrationResult.success != null){
                clientInfo.text = "OAuth client registration token : ${registrationResult.success.registrationAccessToken}"
                accessToken.text = getString(R.string.tap_authenticate)
                refreshToken.text = getString(R.string.tap_authenticate)
            }
        })

        loginViewModel.cozyBaseUrlString.observe(this@LoginActivity, Observer {
            val url = it?: return@Observer

            cozyUrl.text = url
        })

        loginViewModel.authLoginResult.observe(this@LoginActivity, Observer {
            val authLoginResult = it ?: return@Observer

            accessToken.text = "access token : ${authLoginResult.success?.accesstoken}"
            refreshToken.text = "refresh token : ${authLoginResult.success?.refreshToken}"
        })

        loginViewModel.authenticationUri.observe(this@LoginActivity, Observer {
            it?.let { authURI ->
                val connection = object : CustomTabsServiceConnection() {
                    override fun onCustomTabsServiceConnected(componentName: ComponentName, client: CustomTabsClient) {
                        val builder = CustomTabsIntent.Builder()
                        val intent = builder.build()
                        client.warmup(0L) // This prevents backgrounding after redirection
                        intent.launchUrl(this@LoginActivity, Uri.parse(authURI.toURL().toString()))//pass the url you need to open
                    }

                    override fun onServiceDisconnected(name: ComponentName) {

                    }
                }
                CustomTabsClient.bindCustomTabsService(
                    this@LoginActivity,
                    "com.android.chrome",
                    //"com.brave.browser",
                    connection
                )//mention package name which can handle the CCT their many browser present.


            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        /*username.afterTextChanged {
            loginViewModel.loginDataChanged(
                username.text.toString(),
                "12345"//password.text.toString()
            )
        }*/

        username.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    username.text.toString(),
                    "12345"//password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.registerOAuthClient(
                            username.text.toString())
                }
                false
            }

            registering.setOnClickListener {
                loading.visibility = View.VISIBLE
                if (!loginViewModel.isRegistered()) {
                        loginViewModel.registerOAuthClient(username.text.toString())
                }
                else
                    loginViewModel.unregisterAuthclient()
            }

            authenticate.setOnClickListener{
                if (loginViewModel.isRegistered())
                    loginViewModel.authenticate()
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
