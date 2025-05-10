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
import com.mobile.model.TutorRegistration
import com.mobile.databinding.ActivityTutorRegisterPage2Binding
import com.mobile.ui.login.LoginActivity
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TutorRegisterPage2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorRegisterPage2Binding
    
    // Variables to store data from the first page
    private lateinit var email: String
    private lateinit var username: String
    private lateinit var contactDetails: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTutorRegisterPage2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from the first page
        email = intent.getStringExtra("email") ?: ""
        username = intent.getStringExtra("username") ?: ""
        contactDetails = intent.getStringExtra("contactDetails") ?: ""

        // Initialize UI elements
        val firstNameEditText = binding.firstNameEditText
        val lastNameEditText = binding.lastNameEditText
        val passwordEditText = binding.passwordEditText
        val confirmPasswordEditText = binding.confirmPasswordEditText
        val bioEditText = binding.bioEditText
        val expertiseEditText = binding.expertiseEditText
        val hourlyRateEditText = binding.hourlyRateEditText
        val subjectsEditText = binding.subjectsEditText
        val registerButton = binding.registerButton
        val backButton = binding.backButton
        val signInTextView = binding.signInTextView
        val learnerRegisterTextView = binding.learnerRegisterTextView

        // Set up text watchers for validation
        setupTextWatcher(firstNameEditText)
        setupTextWatcher(lastNameEditText)
        setupTextWatcher(passwordEditText)
        setupTextWatcher(confirmPasswordEditText)
        setupTextWatcher(bioEditText)
        setupTextWatcher(expertiseEditText)
        setupTextWatcher(hourlyRateEditText)
        setupTextWatcher(subjectsEditText)

        // Set up back button
        backButton.setOnClickListener {
            finish()
        }

        // Set up register button
        registerButton.setOnClickListener {
            // Show loading indicator
            binding.loading.visibility = View.VISIBLE

            // Get user input
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()
            val bio = bioEditText.text.toString()
            val expertise = expertiseEditText.text.toString()
            val hourlyRateStr = hourlyRateEditText.text.toString()
            val subjectsStr = subjectsEditText.text.toString()

            // Validate input
            if (firstName.isEmpty() || lastName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                bio.isEmpty() || expertise.isEmpty() || hourlyRateStr.isEmpty() || subjectsStr.isEmpty()) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.please_fill_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
                binding.loading.visibility = View.GONE
                return@setOnClickListener
            }

            // Check if passwords match
            if (password != confirmPassword) {
                Toast.makeText(
                    applicationContext,
                    "Passwords do not match",
                    Toast.LENGTH_SHORT
                ).show()
                binding.loading.visibility = View.GONE
                return@setOnClickListener
            }

            // Parse hourly rate
            val hourlyRate = try {
                hourlyRateStr.toDouble()
            } catch (e: NumberFormatException) {
                Toast.makeText(
                    applicationContext,
                    "Please enter a valid hourly rate",
                    Toast.LENGTH_SHORT
                ).show()
                binding.loading.visibility = View.GONE
                return@setOnClickListener
            }

            // Parse subjects (comma separated)
            val subjects = subjectsStr.split(",").map { it.trim() }

            // Create tutor registration object
            val tutorRegistration = TutorRegistration(
                username = username,
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName,
                contactDetails = contactDetails.takeIf { it.isNotEmpty() },
                bio = bio,
                expertise = expertise,
                hourlyRate = hourlyRate,
                subjects = subjects
            )

            // Register tutor
            registerTutor(tutorRegistration)
        }

        // Set up sign in text view
        signInTextView.setOnClickListener {
            // Navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set up learner register text view
        learnerRegisterTextView.setOnClickListener {
            // Navigate to RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerTutor(tutorRegistration: TutorRegistration) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    NetworkUtils.registerTutor(tutorRegistration)
                }

                binding.loading.visibility = View.GONE

                if (result.isSuccess) {
                    // Registration successful
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.registration_successful),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to login screen
                    val intent = Intent(this@TutorRegisterPage2Activity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Registration failed
                    Toast.makeText(
                        applicationContext,
                        "${getString(R.string.registration_failed)}: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loading.visibility = View.GONE

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
        val firstName = binding.firstNameEditText.text.toString()
        val lastName = binding.lastNameEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()
        val bio = binding.bioEditText.text.toString()
        val expertise = binding.expertiseEditText.text.toString()
        val hourlyRate = binding.hourlyRateEditText.text.toString()
        val subjects = binding.subjectsEditText.text.toString()

        // Simple validation
        binding.registerButton.isEnabled = firstName.isNotEmpty() &&
                                         lastName.isNotEmpty() &&
                                         password.length >= 5 &&
                                         confirmPassword == password &&
                                         bio.isNotEmpty() &&
                                         expertise.isNotEmpty() &&
                                         hourlyRate.isNotEmpty() &&
                                         subjects.isNotEmpty()
    }
}