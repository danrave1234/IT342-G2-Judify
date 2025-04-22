package com.mobile.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import com.mobile.R
import com.mobile.ui.base.BaseFragment
import com.mobile.ui.login.LoginActivity
import com.mobile.utils.PreferenceUtils
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Fragment for displaying user profile
 */
class ProfileFragment : BaseFragment() {

    // UI components
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

    // ViewModel
    private lateinit var viewModel: ProfileViewModel

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_profile
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        // Initialize UI components
        initializeViews(view)

        // Set up listeners
        setupListeners()

        // Set up observers
        setupObservers()

        // Load profile data
        loadProfileData()

        // Update theme mode text
        updateThemeModeText()
    }

    private fun initializeViews(view: View) {
        profileImage = view.findViewById(R.id.profileImage)
        userNameText = view.findViewById(R.id.userNameText)
        userEmailText = view.findViewById(R.id.userEmailText)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        sessionsCountText = view.findViewById(R.id.sessionsCountText)
        reviewsCountText = view.findViewById(R.id.reviewsCountText)
        messagesCountText = view.findViewById(R.id.messagesCountText)
        paymentMethodsOption = view.findViewById(R.id.paymentMethodsOption)
        notificationsOption = view.findViewById(R.id.notificationsOption)
        themeModeOption = view.findViewById(R.id.themeModeOption)
        themeModeText = view.findViewById(R.id.themeModeText)
        privacySettingsOption = view.findViewById(R.id.privacySettingsOption)
        logoutOption = view.findViewById(R.id.logoutOption)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
    }

    private fun setupListeners() {
        editProfileButton.setOnClickListener {
            // Navigate to edit profile screen
            val editProfileFragment = EditProfileFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editProfileFragment)
                .addToBackStack(null)
                .commit()
        }

        paymentMethodsOption.setOnClickListener {
            Toast.makeText(requireContext(), "Payment Methods coming soon", Toast.LENGTH_SHORT).show()
        }

        notificationsOption.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        themeModeOption.setOnClickListener {
            showThemeSelectionDialog()
        }

        privacySettingsOption.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy Settings coming soon", Toast.LENGTH_SHORT).show()
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

        AlertDialog.Builder(requireContext())
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
        requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit().putInt("theme_mode", mode).apply()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
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
        PreferenceUtils.clearUserSession(requireContext())

        // Navigate to login screen
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
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

                    // Update profile image
                    if (state.user.profileImageUrl != null && state.user.profileImageUrl.isNotEmpty()) {
                        // TODO: Load image from URL using a library like Glide or Picasso
                        // For now, we'll use the default profile image
                        profileImage.setImageResource(R.drawable.default_profile)
                    } else {
                        // Use placeholder for profile image
                        profileImage.setImageResource(R.drawable.ic_person)
                    }

                    // Update stats
                    sessionsCountText.text = state.sessions.toString()
                    reviewsCountText.text = state.reviews.toString()
                    messagesCountText.text = state.messages.toString()
                }
            }
        }
    }

    private fun loadProfileData() {
        // Get user email from UserPreferences
        val email = PreferenceUtils.getUserEmail(requireContext())
        if (email != null) {
            viewModel.loadUserProfile(email)
        } else {
            // No user is logged in, navigate to login screen
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
