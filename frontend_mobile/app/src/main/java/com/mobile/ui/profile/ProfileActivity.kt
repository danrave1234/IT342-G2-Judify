package com.mobile.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mobile.R
import com.mobile.databinding.ActivityProfileBinding
import com.mobile.ui.login.LoginActivity
import com.mobile.ui.profile.ProfileViewModel
import de.hdodenhof.circleimageview.CircleImageView

class ProfileActivity : AppCompatActivity() {

    // UI components
    private lateinit var toolbar: Toolbar
    private lateinit var profileImage: CircleImageView
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var editProfileButton: Button
    private lateinit var sessionsCountText: TextView
    private lateinit var reviewsCountText: TextView
    private lateinit var messagesCountText: TextView
    private lateinit var paymentMethodsOption: LinearLayout
    private lateinit var notificationsOption: LinearLayout
    private lateinit var themeModeOption: LinearLayout
    private lateinit var themeModeText: TextView
    private lateinit var privacySettingsOption: LinearLayout
    private lateinit var logoutOption: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var binding: ActivityProfileBinding
    private val sharedPreferences by lazy {
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    }

    // ViewModel
    private lateinit var viewModel: ProfileViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        
        // Initialize UI components
        initializeViews()
        
        // Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Set up listeners
        setupListeners()
        
        // Set up observers
        setupObservers()
        
        // Load profile data
        loadProfileData()
        
        // Update theme mode text
        updateThemeModeText()
    }
    
    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        profileImage = findViewById(R.id.profileImage)
        userNameText = findViewById(R.id.userNameText)
        userEmailText = findViewById(R.id.userEmailText)
        editProfileButton = findViewById(R.id.editProfileButton)
        sessionsCountText = findViewById(R.id.sessionsCountText)
        reviewsCountText = findViewById(R.id.reviewsCountText)
        messagesCountText = findViewById(R.id.messagesCountText)
        paymentMethodsOption = findViewById(R.id.paymentMethodsOption)
        notificationsOption = findViewById(R.id.notificationsOption)
        themeModeOption = findViewById(R.id.themeModeOption)
        themeModeText = findViewById(R.id.themeModeText)
        privacySettingsOption = findViewById(R.id.privacySettingsOption)
        logoutOption = findViewById(R.id.logoutOption)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
    }
    
    private fun setupListeners() {
        editProfileButton.setOnClickListener {
            // Navigate to edit profile screen
            Toast.makeText(this, "Edit Profile coming soon", Toast.LENGTH_SHORT).show()
        }
        
        paymentMethodsOption.setOnClickListener {
            Toast.makeText(this, "Payment Methods coming soon", Toast.LENGTH_SHORT).show()
        }
        
        notificationsOption.setOnClickListener {
            Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }
        
        themeModeOption.setOnClickListener {
            showThemeSelectionDialog()
        }
        
        privacySettingsOption.setOnClickListener {
            Toast.makeText(this, "Privacy Settings coming soon", Toast.LENGTH_SHORT).show()
        }
        
        logoutOption.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }
    
    private fun updateThemeModeText() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val modeText = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "System"
            AppCompatDelegate.MODE_NIGHT_YES -> "Dark"
            AppCompatDelegate.MODE_NIGHT_NO -> "Light"
            else -> "System"
        }
        themeModeText.text = modeText
    }
    
    private fun showThemeSelectionDialog() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val themes = arrayOf("System Default", "Light", "Dark")
        var selectedIndex = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 0
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, selectedIndex) { _, index ->
                selectedIndex = index
            }
            .setPositiveButton("OK") { _, _ ->
                val mode = when (selectedIndex) {
                    0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                updateTheme(mode)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        // Save theme preference
        sharedPreferences.edit().putInt("theme_mode", mode).apply()
    }
    
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }
    
    private fun logout() {
        // Clear user data
        sharedPreferences.edit().apply {
            clear()
            apply()
        }

        // Navigate to login screen
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun setupObservers() {
        viewModel.profileState.observe(this) { state ->
            when {
                state.isLoading -> {
                    progressBar.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                }
                state.error != null -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.error
                }
                state.user != null -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    
                    // Update UI with user data
                    userNameText.text = state.user.name
                    userEmailText.text = state.user.email
                    
                    // Update stats
                    sessionsCountText.text = state.sessions.toString()
                    reviewsCountText.text = state.reviews.toString()
                    messagesCountText.text = state.messages.toString()
                }
            }
        }
    }
    
    private fun loadProfileData() {
        // Get user ID from UserPreferences
        val userId = sharedPreferences.getLong("user_id", 0)
        if (userId > 0) {
            viewModel.loadUserProfile(userId)
        } else {
            // No user is logged in, navigate to login screen
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 