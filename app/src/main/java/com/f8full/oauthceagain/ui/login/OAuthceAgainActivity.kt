package com.f8full.oauthceagain.ui.login

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

    private lateinit var ActivityViewModel: OAuthceAgainViewModel

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val action = intent?.action
        val data = intent?.dataString

        if (action == Intent.ACTION_VIEW && data != null){
            Log.e("LoginActivity", "Intent data stirng : $data")
            ActivityViewModel.retrieveAccessTokenAndRefreshToken(data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (ActivityViewModel.isRegistered()) {
            ActivityViewModel.unregisterAuthclient()
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

        ActivityViewModel = ViewModelProviders.of(this, LoginViewModelFactory())
            .get(OAuthceAgainViewModel::class.java)

        ActivityViewModel.clientRegistrationResult.observe(this@LoginActivity, Observer {
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

        ActivityViewModel.cozyBaseUrlString.observe(this@LoginActivity, Observer {
            val url = it?: return@Observer

            cozyUrl.text = url
        })

        ActivityViewModel.authLoginResult.observe(this@LoginActivity, Observer {
            val authLoginResult = it ?: return@Observer

            accessToken.text = "access token : ${authLoginResult.success?.accesstoken}"
            refreshToken.text = "refresh token : ${authLoginResult.success?.refreshToken}"
        })

        ActivityViewModel.authenticationUri.observe(this@LoginActivity, Observer {
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

        /*username.afterTextChanged {
            ActivityViewModel.loginDataChanged(
                username.text.toString(),
                "12345"//password.text.toString()
            )
        }*/

        username.apply {
            afterTextChanged {
                ActivityViewModel.loginDataChanged(
                    username.text.toString(),
                    "12345"//password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        ActivityViewModel.registerOAuthClient(
                            username.text.toString())
                }
                false
            }

            registering.setOnClickListener {
                loading.visibility = View.VISIBLE
                if (!ActivityViewModel.isRegistered()) {
                        ActivityViewModel.registerOAuthClient(username.text.toString())
                }
                else
                    ActivityViewModel.unregisterAuthclient()
            }

            authenticate.setOnClickListener{
                if (ActivityViewModel.isRegistered())
                    ActivityViewModel.authenticate()
            }
        }

        ActivityViewModel.loginFormState.observe(this@LoginActivity, Observer {
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
    }
}
////////////////////////////////////////////////////////////////////////////////////////////////
//From login sample wizard
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
