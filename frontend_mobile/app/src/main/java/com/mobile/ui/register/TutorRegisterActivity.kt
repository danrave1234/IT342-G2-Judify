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
import com.mobile.databinding.ActivityTutorRegisterBinding
import com.mobile.ui.login.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TutorRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTutorRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI elements
        val emailEditText = binding.emailEditText
        val usernameEditText = binding.usernameEditText
        val contactDetailsEditText = binding.contactDetailsEditText
        val backButton = binding.backButton
        val signInTextView = binding.signInTextView
        val learnerRegisterTextView = binding.learnerRegisterTextView

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
                Toast.makeText(
                    applicationContext,
                    getString(R.string.please_fill_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Navigate to the second page of registration
            val intent = Intent(this, TutorRegisterPage2Activity::class.java)
            intent.putExtra("email", email)
            intent.putExtra("username", username)
            intent.putExtra("contactDetails", contactDetails)
            startActivity(intent)
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

        // Simple validation
        binding.nextButton.isEnabled = email.contains("@") && 
                                      username.isNotEmpty()
    }
}