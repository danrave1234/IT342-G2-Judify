package com.mobile.ui.register

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.mobile.R
import com.mobile.model.User
import com.mobile.databinding.ActivityRegisterBinding
import com.mobile.ui.login.LoginActivity
import com.mobile.repository.AuthRepository
import com.mobile.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AuthRepository
        authRepository = AuthRepository()

        // Initialize UI elements
        val emailEditText = binding.emailEditText
        val usernameEditText = binding.usernameEditText
        val contactDetailsEditText = binding.contactDetailsEditText
        val backButton = binding.backButton
        val facebookButton = binding.facebookButton
        val googleButton = binding.googleButton
        val signInTextView = binding.signInTextView
        val tutorRegisterTextView = binding.tutorRegisterTextView

        // Set up text watchers for validation
        setupTextWatcher(emailEditText)
        setupTextWatcher(usernameEditText)
        setupTextWatcher(contactDetailsEditText)

        // Set up back button
        backButton.setOnClickListener {
            finish()
        }

        // Set up next button
        binding.nextButton.setOnClickListener {
            // Get user input
            val email = emailEditText.text.toString()
            val username = usernameEditText.text.toString()
            val contactDetails = contactDetailsEditText.text.toString()

            // Validate input
            if (email.isEmpty() || username.isEmpty()) {
                UiUtils.showErrorSnackbar(
                    binding.root,
                    getString(R.string.please_fill_all_fields)
                )
                return@setOnClickListener
            }

            // Navigate to the second page of registration
            val intent = Intent(this, RegisterPage2Activity::class.java)
            intent.putExtra("email", email)
            intent.putExtra("username", username)
            intent.putExtra("contactDetails", contactDetails)
            startActivity(intent)
        }

        // Set up social login buttons
        facebookButton.setOnClickListener {
            UiUtils.showInfoSnackbar(
                binding.root,
                "Facebook login coming soon!"
            )
        }

        googleButton.setOnClickListener {
            UiUtils.showInfoSnackbar(
                binding.root,
                "Google login coming soon!"
            )
        }

        // Set up sign in text view
        signInTextView.setOnClickListener {
            // Navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set up tutor register text view
        tutorRegisterTextView.setOnClickListener {
            // Navigate to TutorRegisterActivity
            val intent = Intent(this, TutorRegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser(user: User) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Use AuthRepository instead of directly using NetworkUtils
                val authResponse = authRepository.register(
                    email = user.email,
                    username = user.username ?: user.email, // Use email as username if not provided
                    password = user.passwordHash,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    contactDetails = user.contactDetails
                )

                binding.loading?.visibility = View.GONE

                if (authResponse.success) {
                    // Registration successful
                    UiUtils.showSuccessSnackbar(
                        binding.root,
                        getString(R.string.registration_successful)
                    )

                    // Navigate to login screen
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Registration failed
                    UiUtils.showErrorSnackbar(
                        binding.root,
                        "${getString(R.string.registration_failed)}: ${authResponse.message}"
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loading?.visibility = View.GONE

                    // Handle exception
                    UiUtils.showErrorSnackbar(
                        binding.root,
                        "${getString(R.string.registration_failed)}: ${e.message}"
                    )
                }
            }
        }
    }

    private fun setupTextWatcher(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                validateForm()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Ignore
            }
        })
    }

    private fun validateForm() {
        val email = binding.emailEditText.text.toString()
        val username = binding.usernameEditText.text.toString()
        val contactDetails = binding.contactDetailsEditText.text.toString()

        // Simple validation
        binding.nextButton.isEnabled = email.contains("@") && 
                                      username.isNotEmpty()
    }
} 
