package com.mobile.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log // <-- Added Log import
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
import androidx.lifecycle.Observer // <-- Added Observer import
import androidx.lifecycle.lifecycleScope // <-- Added lifecycleScope import
import com.mobile.R
import com.mobile.ui.base.BaseFragment
import com.mobile.ui.login.LoginActivity
import com.mobile.ui.register.RegisterActivity // <-- Fixed package for RegisterActivity
import com.mobile.utils.PreferenceUtils
import de.hdodenhof.circleimageview.CircleImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.mobile.ui.dashboard.StudentDashboardActivity
import com.mobile.ui.dashboard.TutorDashboardActivity
import com.mobile.utils.NetworkUtils
import com.mobile.utils.UiUtils
import kotlinx.coroutines.launch

/**
 * Fragment for displaying user profile
 */
class ProfileFragment : BaseFragment() {
    private val TAG = "ProfileFragment" // Add TAG

    // Constants
    private val REQUEST_EDIT_PROFILE = 100

    // UI components
    private lateinit var profileImage: CircleImageView
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var userTypeText: TextView
    private lateinit var editProfileButton: Button
    private lateinit var sessionsCountText: TextView
    private lateinit var reviewsCountText: TextView
    private lateinit var messagesCountText: TextView
    private lateinit var themeModeText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorCard: View
    private lateinit var errorText: TextView
    private lateinit var retryButton: Button
    private lateinit var accountSettingsCard: MaterialCardView
    private lateinit var paymentMethodsButton: LinearLayout
    private lateinit var notificationsButton: LinearLayout
    private lateinit var securityButton: LinearLayout
    private lateinit var privacyButton: LinearLayout
    private lateinit var logoutButton: LinearLayout
    private lateinit var loginContainer: LinearLayout
    private lateinit var loginButton: MaterialButton
    private lateinit var registerButton: MaterialButton

    // ViewModel
    private lateinit var viewModel: ProfileViewModel
    private var profileStateObserver: Observer<ProfileState>? = null // Observer instance variable
    private var userType: String = ""

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_profile
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Initialize ViewModel - use requireActivity() for shared ViewModel
        viewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)

        // Initialize UI components
        initializeViews(view)

        // Set up listeners
        setupListeners()

        // Set up observers
        setupObservers()

        // Load profile data if not already loaded or if coming back from edit
        // Use the existing loadUserProfile logic which checks for existing data
        loadProfileData()

        // Update theme mode text
        updateThemeModeText()
    }

    override fun onDestroyView() {
        // Clean up observer to prevent memory leaks
        profileStateObserver?.let { viewModel.profileState.removeObserver(it) }
        profileStateObserver = null
        super.onDestroyView()
    }

    private fun initializeViews(view: View) {
        profileImage = view.findViewById(R.id.profileImage)
        userNameText = view.findViewById(R.id.userNameText)
        userEmailText = view.findViewById(R.id.userEmailText)
        userTypeText = view.findViewById(R.id.userTypeText)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        sessionsCountText = view.findViewById(R.id.sessionsCountText)
        reviewsCountText = view.findViewById(R.id.reviewsCountText)
        messagesCountText = view.findViewById(R.id.messagesCountText)
        themeModeText = view.findViewById(R.id.themeModeText)
        progressBar = view.findViewById(R.id.progressBar)
        errorCard = view.findViewById(R.id.errorCard)
        errorText = view.findViewById(R.id.errorText)
        retryButton = view.findViewById(R.id.retryButton)
        accountSettingsCard = view.findViewById(R.id.accountSettingsCard)
        paymentMethodsButton = view.findViewById(R.id.paymentMethodsButton)
        notificationsButton = view.findViewById(R.id.notificationsButton)
        securityButton = view.findViewById(R.id.securityButton)
        privacyButton = view.findViewById(R.id.privacyButton)
        logoutButton = view.findViewById(R.id.logoutButton)
        loginContainer = view.findViewById(R.id.loginContainer)
        loginButton = view.findViewById(R.id.loginButton)
        registerButton = view.findViewById(R.id.registerButton)

        // Set up retry button click listener
        retryButton.setOnClickListener {
            loadProfileData()
        }
    }

    private fun setupListeners() {
        editProfileButton.setOnClickListener {
            Log.d(TAG, "Edit Profile button clicked.") // Log click
            // Navigate to edit profile screen
            val editProfileFragment = EditProfileFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editProfileFragment)
                .addToBackStack(null) // Add to back stack so user can return
                .commit()
        }

        paymentMethodsButton.setOnClickListener {
            UiUtils.showInfoSnackbar(requireView(), "Payment Methods coming soon")
        }

        notificationsButton.setOnClickListener {
            UiUtils.showInfoSnackbar(requireView(), "Notifications coming soon")
        }

        securityButton.setOnClickListener {
            UiUtils.showInfoSnackbar(requireView(), "Security settings coming soon")
        }

        privacyButton.setOnClickListener {
            UiUtils.showInfoSnackbar(requireView(), "Privacy Settings coming soon")
        }

        logoutButton.setOnClickListener {
            logout()
        }

        loginButton.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        registerButton.setOnClickListener {
            val intent = Intent(requireContext(), RegisterActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
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
        // Remove previous observer if exists
        profileStateObserver?.let { viewModel.profileState.removeObserver(it) }

        profileStateObserver = Observer { state ->
            Log.d(TAG, "Observer received state: $state") // Log state changes

            when {
                state.isLoading -> {
                    progressBar.visibility = View.VISIBLE
                    errorCard.visibility = View.GONE
                    Log.d(TAG, "State is Loading")
                }
                state.error != null -> {
                    progressBar.visibility = View.GONE
                    errorCard.visibility = View.VISIBLE
                    errorText.text = state.error
                    Log.e(TAG, "State has Error: ${state.error}")
                }
                state.user != null -> {
                    progressBar.visibility = View.GONE
                    errorCard.visibility = View.GONE
                    Log.d(TAG, "State has User: ${state.user.name}")

                    // Update UI with user data
                    userNameText.text = state.user.name
                    userEmailText.text = state.user.email
                    userTypeText.text = if (state.user.role == "tutor") "Tutor" else "Student"

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

                    // Check if the update was successful
                    if (state.isUpdateSuccessful) {
                        Log.d(TAG, "Update was successful, showing toast and clearing status.")
                        UiUtils.showSuccessSnackbar(requireView(), "Profile updated successfully")
                        // Reset the flag in the ViewModel
                        viewModel.clearUpdateStatus()
                    }
                }
                else -> {
                    // Handle case where state is not loading, no error, but user is null
                    progressBar.visibility = View.GONE
                    errorCard.visibility = View.VISIBLE
                    errorText.text = "Could not load profile data."
                    Log.w(TAG, "State is not loading, no error, but user is null.")
                }
            }
        }
        viewModel.profileState.observe(viewLifecycleOwner, profileStateObserver!!)
    }

    private fun loadProfileData() {
        val userId = PreferenceUtils.getUserId(requireContext())
        val userRole = PreferenceUtils.getUserRole(requireContext())
        val email = PreferenceUtils.getUserEmail(requireContext())

        if (userId != null) {
            // User is logged in
            loginContainer.visibility = View.GONE
            accountSettingsCard.visibility = View.VISIBLE
            logoutButton.visibility = View.VISIBLE

            // Set user type
            userType = userRole ?: "student"

            // Use the ViewModel to load profile data
            if (email != null) {
                Log.d(TAG, "Loading profile data for email: $email")
                viewModel.loadUserProfile(email)
            } else {
                Log.e(TAG, "User email is null, cannot load profile")
                errorCard.visibility = View.VISIBLE
                errorText.text = "User email not found. Please log in again."
            }
        } else {
            // User is not logged in
            loginContainer.visibility = View.VISIBLE
            accountSettingsCard.visibility = View.GONE
            logoutButton.visibility = View.GONE
            userNameText.text = "Guest User"
            userEmailText.text = "Please log in to view your profile"
            userTypeText.text = ""
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_EDIT_PROFILE && resultCode == Activity.RESULT_OK) {
            // Reload profile using the ViewModel
            val email = PreferenceUtils.getUserEmail(requireContext())
            if (email != null) {
                viewModel.loadUserProfile(email)
            }
            
            UiUtils.showSuccessSnackbar(requireView(), "Profile updated successfully")
            
            // Notify the parent activity to refresh navigation drawer
            val activity = requireActivity()
            if (activity is StudentDashboardActivity || activity is TutorDashboardActivity) {
                activity.recreate()
            }
        }
    }

    companion object {
        private const val REQUEST_EDIT_PROFILE = 100
    }
}
