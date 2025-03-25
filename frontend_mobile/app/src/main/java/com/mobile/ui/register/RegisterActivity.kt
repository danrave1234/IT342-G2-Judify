package com.mobile.ui.register

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobile.R
import com.mobile.data.model.User
import com.mobile.databinding.ActivityRegisterBinding
import com.mobile.ui.login.LoginActivity
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI elements
        val emailEditText = binding.emailEditText
        val firstNameEditText = binding.firstNameEditText
        val lastNameEditText = binding.lastNameEditText
        val passwordEditText = binding.passwordEditText
        val signUpButton = binding.signUpButton
        val backButton = binding.backButton
        val rememberMeCheckbox = binding.rememberMeCheckbox
        val facebookButton = binding.facebookButton
        val googleButton = binding.googleButton
        val appleButton = binding.appleButton
        val signInTextView = binding.signInTextView

        // Set up text watchers for validation
        setupTextWatcher(emailEditText)
        setupTextWatcher(passwordEditText)
        setupTextWatcher(firstNameEditText)
        setupTextWatcher(lastNameEditText)

        // Set up back button
        backButton.setOnClickListener {
            finish()
        }

        // Set up sign up button
        signUpButton.setOnClickListener {
            // Show loading indicator
            binding.loading?.visibility = View.VISIBLE
            
            // Get user input
            val email = emailEditText.text.toString()
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val password = passwordEditText.text.toString()
            
            // Validate input
            if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.please_fill_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
                binding.loading?.visibility = View.GONE
                return@setOnClickListener
            }
            
            // Create user object - send plain password to be hashed by the server
            val user = User(
                email = email,
                passwordHash = password, // Send plain password, server will hash it
                firstName = firstName,
                lastName = lastName,
                roles = "LEARNER" // Default role
            )
            
            // Register user
            registerUser(user)
        }

        // Set up social login buttons
        facebookButton.setOnClickListener {
            Toast.makeText(
                applicationContext,
                "Facebook login coming soon!",
                Toast.LENGTH_SHORT
            ).show()
        }

        googleButton.setOnClickListener {
            Toast.makeText(
                applicationContext,
                "Google login coming soon!",
                Toast.LENGTH_SHORT
            ).show()
        }

        appleButton.setOnClickListener {
            Toast.makeText(
                applicationContext,
                "Apple login coming soon!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Set up sign in text view
        signInTextView.setOnClickListener {
            // Navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun registerUser(user: User) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = NetworkUtils.registerUser(user)
                
                result.fold(
                    onSuccess = { registeredUser ->
                        binding.loading?.visibility = View.GONE
                        
                        // Registration successful
                        Toast.makeText(
                            applicationContext,
                            getString(R.string.registration_successful),
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Navigate to login screen
                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onFailure = { exception ->
                        binding.loading?.visibility = View.GONE
                        
                        // Registration failed
                        Toast.makeText(
                            applicationContext,
                            "${getString(R.string.registration_failed)}: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loading?.visibility = View.GONE
                    
                    // Handle exception
                    Toast.makeText(
                        applicationContext,
                        "${getString(R.string.registration_failed)}: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
        val firstName = binding.firstNameEditText.text.toString()
        val lastName = binding.lastNameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        
        // Simple validation
        binding.signUpButton.isEnabled = email.contains("@") && 
                                         password.length >= 5 &&
                                         firstName.isNotEmpty() &&
                                         lastName.isNotEmpty()
    }
} 