package com.mobile.ui.profile

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.lifecycle.ViewModelProvider
import com.mobile.R
import com.mobile.ui.base.BaseFragment
import com.mobile.utils.PreferenceUtils
import com.mobile.utils.UiUtils
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Fragment for editing user profile
 */
class EditProfileFragment : BaseFragment() {
    private val TAG = "EditProfileFragment"

    // UI components
    private lateinit var profileImageView: CircleImageView
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var bioEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var rootView: View

    // ViewModel
    private lateinit var viewModel: ProfileViewModel

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_edit_profile
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Store root view for Snackbars
        rootView = view

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)

        // Initialize views
        initializeViews(view)

        // Set up observers
        setupObservers()

        // Load current profile data
        loadProfileData()
    }

    private fun initializeViews(view: View) {
        profileImageView = view.findViewById(R.id.profileImageView)
        firstNameEditText = view.findViewById(R.id.firstNameEditText)
        lastNameEditText = view.findViewById(R.id.lastNameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText)
        bioEditText = view.findViewById(R.id.bioEditText)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        progressBar = view.findViewById(R.id.progressBar)

        // Set up click listeners
        profileImageView.setOnClickListener {
            UiUtils.showInfoSnackbar(rootView, "Image selection coming soon")
        }

        saveButton.setOnClickListener {
            saveProfile()
        }

        cancelButton.setOnClickListener {
            // Go back without saving
            requireActivity().onBackPressed()
        }
    }

    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            // Update UI based on state
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            if (state.error != null) {
                UiUtils.showErrorSnackbar(rootView, "Error loading profile: ${state.error}")
            }

            // Fill form with user data if available
            state.user?.let { user ->
                // Split name into first and last name if needed
                val names = user.name.split(" ", limit = 2)
                firstNameEditText.setText(names.getOrNull(0) ?: "")
                lastNameEditText.setText(names.getOrNull(1) ?: "")

                // Set other fields
                emailEditText.setText(user.email)
                phoneNumberEditText.setText(user.phoneNumber ?: "")
                bioEditText.setText(user.bio ?: "")
            }
        }
    }

    private fun loadProfileData() {
        // Get user email from preferences
        val email = PreferenceUtils.getUserEmail(requireContext())
        if (email == null) {
            UiUtils.showErrorSnackbar(rootView, "Error: User email not found.")
            requireActivity().onBackPressed()
            return
        }

        // Load profile using the ViewModel
        viewModel.loadUserProfile(email)
    }

    private fun saveProfile() {
        // Validate inputs
        if (!validateInputs()) {
            return
        }

        // Show loading
        progressBar.visibility = View.VISIBLE

        // Get values from form
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val fullName = "$firstName $lastName".trim()
        val email = emailEditText.text.toString().trim()
        val phoneNumber = phoneNumberEditText.text.toString().trim()
        val bio = bioEditText.text.toString().trim()

        // Update profile using ViewModel
        viewModel.updateUserProfile(
            name = fullName,
            email = email,
            phoneNumber = phoneNumber,
            bio = bio,
            onComplete = { success ->
                // Hide loading
                progressBar.visibility = View.GONE

                if (success) {
                    // Set result and finish
                    requireActivity().setResult(Activity.RESULT_OK)
                    requireActivity().onBackPressed()
                } else {
                    UiUtils.showErrorSnackbar(rootView, "Failed to update profile")
                }
            }
        )
    }

    private fun validateInputs(): Boolean {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        // Check if required fields are filled
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            UiUtils.showErrorSnackbar(rootView, "Please fill in all required fields (First Name, Last Name, Email)")
            return false
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            UiUtils.showErrorSnackbar(rootView, "Please enter a valid email address")
            return false
        }

        return true
    }

    companion object {
        fun newInstance(): EditProfileFragment {
            return EditProfileFragment()
        }
    }
}