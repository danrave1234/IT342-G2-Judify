package com.mobile.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mobile.R
import com.mobile.data.model.AuthResponse
import com.mobile.ui.dashboard.LearnerDashboardActivity
import com.mobile.ui.register.RegisterActivity
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var registerTextView: TextView
    private lateinit var rememberMeCheckbox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize ViewModel using the factory
        viewModel = ViewModelProvider(this, LoginViewModelFactory(application))
            .get(LoginViewModel::class.java)

        // Initialize UI components
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.login)
        loadingProgressBar = findViewById(R.id.loading)
        registerTextView = findViewById(R.id.registerTextView)
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox)

        // Setup login button click listener
        loginButton.setOnClickListener {
            attemptLogin()
        }

        // Setup register text view click listener
        registerTextView.setOnClickListener {
            navigateToRegister()
        }
    }

    private fun attemptLogin() {
        // Get input values
        val email = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        // Validate inputs
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.please_fill_all_fields),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Show loading indicator
        loadingProgressBar.visibility = View.VISIBLE
        loginButton.isEnabled = false

        // Attempt login
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = NetworkUtils.authenticateUser(email, password)
                
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    loginButton.isEnabled = true

                    result.fold(
                        onSuccess = { response ->
                            if (response.isAuthenticated) {
                                // Convert NetworkUtils.AuthResponse to our model AuthResponse
                                val authResponse = com.mobile.data.model.AuthResponse(
                                    success = true,
                                    isAuthenticated = response.isAuthenticated,
                                    userId = response.userId,
                                    email = response.email,
                                    firstName = response.firstName,
                                    lastName = response.lastName,
                                    role = response.role
                                )
                                
                                // Save user session with the converted response
                                viewModel.saveUserSession(
                                    authResponse,
                                    rememberMeCheckbox.isChecked
                                )
                                
                                // Navigate to dashboard
                                onLoginSuccess()
                            } else {
                                // Show error message
                                Toast.makeText(
                                    this@LoginActivity,
                                    getString(R.string.login_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onFailure = { exception ->
                            // Show error message
                            Toast.makeText(
                                this@LoginActivity,
                                exception.message ?: getString(R.string.login_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    loginButton.isEnabled = true
                    
                    // Show error message
                    Toast.makeText(
                        this@LoginActivity,
                        e.message ?: getString(R.string.login_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun onLoginSuccess() {
        // Navigate to dashboard
        val intent = Intent(this, LearnerDashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        // Navigate to register screen
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}