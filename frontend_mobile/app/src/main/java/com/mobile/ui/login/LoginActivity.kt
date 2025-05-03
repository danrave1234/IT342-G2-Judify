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
import com.mobile.ui.dashboard.LearnerDashboardActivity
import com.mobile.ui.dashboard.TutorDashboardActivity
import com.mobile.ui.register.RegisterActivity
import com.mobile.ui.register.TutorRegisterActivity
import com.mobile.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var registerTextView: TextView
    private lateinit var tutorRegisterTextView: TextView
    private lateinit var rememberMeCheckbox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize ViewModel using the factory
        viewModel = ViewModelProvider(this, LoginViewModelFactory(application))
            .get(LoginViewModel::class.java)

        // Initialize AuthRepository
        authRepository = AuthRepository()

        // Initialize UI components
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.login)
        loadingProgressBar = findViewById(R.id.loading)
        registerTextView = findViewById(R.id.registerTextView)
        tutorRegisterTextView = findViewById(R.id.tutorRegisterTextView)
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox)

        // Setup login button click listener
        loginButton.setOnClickListener {
            attemptLogin()
        }

        // Setup register text view click listener
        registerTextView.setOnClickListener {
            navigateToRegister()
        }

        // Setup tutor register text view click listener
        tutorRegisterTextView.setOnClickListener {
            navigateToTutorRegister()
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
            // Use AuthRepository instead of directly using NetworkUtils
            val authResponse = authRepository.login(email, password)

            withContext(Dispatchers.Main) {
                loadingProgressBar.visibility = View.GONE
                loginButton.isEnabled = true

                if (authResponse.isAuthenticated) {
                    // Save user session
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
                        authResponse.message ?: getString(R.string.login_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun onLoginSuccess() {
        // Get user role from preferences
        val userRole = viewModel.getUserRole()

        // Navigate to appropriate dashboard based on role
        val intent = if (userRole == "TUTOR") {
            Intent(this, TutorDashboardActivity::class.java)
        } else {
            Intent(this, LearnerDashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        // Navigate to register screen
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToTutorRegister() {
        // Navigate to tutor register screen
        val intent = Intent(this, TutorRegisterActivity::class.java)
        startActivity(intent)
    }
}
