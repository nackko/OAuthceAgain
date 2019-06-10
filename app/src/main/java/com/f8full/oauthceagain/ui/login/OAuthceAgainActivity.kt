package com.f8full.oauthceagain.ui.login

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.f8full.oauthceagain.R
import com.google.android.material.snackbar.Snackbar
import de.psdev.licensesdialog.LicensesDialog

/**
 * Created by F8Full on 2019-06-10. This file is part of OAuthceAgain
 *
 */
class OAuthceAgainActivity : AppCompatActivity() {

    private lateinit var ActivityViewModel: OAuthceAgainViewModel

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val action = intent?.action
        val data = intent?.dataString

        if (action == Intent.ACTION_VIEW && data != null){
            Log.e("OAuthceAgainActivity", "Intent data stirng : $data")
            ActivityViewModel.retrieveAccessTokenAndRefreshToken(data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_oauthceagain, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about_menu_item -> {
                LicensesDialog.Builder(this@OAuthceAgainActivity)
                    .setNotices(R.raw.notices)
                    .build()
                    .show()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
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

        ActivityViewModel.clientRegistrationResult.observe(this@OAuthceAgainActivity, Observer {
            if (it == null) {
                registering.text = getString(R.string.action_registering)
                username.isEnabled = true
                authenticate.isEnabled = false
                clientInfo.text = getString(R.string.client_info_default)
                accessToken.text = ""
                refreshToken.text = ""
                loading.visibility = View.INVISIBLE
            }

            val registrationResult = it ?: return@Observer

            loading.visibility = View.GONE

            if (registrationResult.error != null){
                registering.text = getString(R.string.action_registering)
                authenticate.isEnabled = false
            }

            if (registrationResult.success != null){
                clientInfo.text = "OAuth client registration token : ${registrationResult.success.registrationAccessToken}"
                accessToken.text = getString(R.string.tap_authenticate)
                refreshToken.text = getString(R.string.tap_authenticate)
                registering.text = getString(R.string.action_unregistering)
                authenticate.isEnabled = true
                username.isEnabled = false
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(username.windowToken, 0)
            }
        })

        ActivityViewModel.cozyBaseUrlString.observe(this@OAuthceAgainActivity, Observer {
            val url = it?: return@Observer

            cozyUrl.text = url
        })

        ActivityViewModel.authLoginResult.observe(this@OAuthceAgainActivity, Observer {
            val authLoginResult = it ?: return@Observer

            accessToken.text = "access token : ${authLoginResult.success?.accesstoken}"
            refreshToken.text = "refresh token : ${authLoginResult.success?.refreshToken}"
            authenticate.isEnabled = false
        })

        ActivityViewModel.authenticationUri.observe(this@OAuthceAgainActivity, Observer {
            it?.let { authURI ->
                val connection = object : CustomTabsServiceConnection() {
                    override fun onCustomTabsServiceConnected(componentName: ComponentName, client: CustomTabsClient) {
                        val builder = CustomTabsIntent.Builder()
                        val intent = builder.build()
                        client.warmup(0L) // This prevents backgrounding after redirection
                        intent.launchUrl(
                            this@OAuthceAgainActivity,
                            Uri.parse(authURI.toURL().toString())
                        )//pass the url you need to open
                    }

                    override fun onServiceDisconnected(name: ComponentName) {

                    }
                }

                if (!CustomTabsClient.bindCustomTabsService(
                        this@OAuthceAgainActivity,
                        "com.brave.browser",
                        //"com.android.chrome",
                        connection
                    )
                ) {
                    Snackbar.make(findViewById(R.id.container), "Brave browser recommended", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Download") {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse("market://details?id=com.brave.browser")
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                            }

                        }
                        .show()
                }
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

        ActivityViewModel.loginFormState.observe(this@OAuthceAgainActivity, Observer {
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
