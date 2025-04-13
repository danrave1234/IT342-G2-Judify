package com.mobile.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mobile.R
import com.mobile.utils.PreferenceUtils
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Fragment for editing user profile
 */
class EditProfileFragment : Fragment() {

    // UI components
    private lateinit var profileImage: CircleImageView
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var contactDetailsEditText: EditText
    private lateinit var bioEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressOverlay: View
    private lateinit var errorText: TextView

    // ViewModel
    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)

        // Initialize UI components
        initializeViews(view)

        // Set up listeners
        setupListeners()

        // Set up observers
        setupObservers()

        // Load current user data
        loadCurrentUserData()

        return view
    }

    private fun initializeViews(view: View) {
        profileImage = view.findViewById(R.id.profileImageView)
        firstNameEditText = view.findViewById(R.id.firstNameEditText)
        lastNameEditText = view.findViewById(R.id.lastNameEditText)
        usernameEditText = view.findViewById(R.id.usernameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        contactDetailsEditText = view.findViewById(R.id.contactDetailsEditText)
        bioEditText = view.findViewById(R.id.bioEditText)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        progressBar = view.findViewById(R.id.progressBar)
        progressOverlay = view.findViewById(R.id.progressOverlay)
        errorText = view.findViewById(R.id.errorTextView)
    }

    private fun setupListeners() {
        saveButton.setOnClickListener {
            saveProfile()
        }

        cancelButton.setOnClickListener {
            // Navigate back to profile fragment
            parentFragmentManager.popBackStack()
        }

        profileImage.setOnClickListener {
            // TODO: Implement image selection functionality
            Toast.makeText(requireContext(), "Image selection coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        // Track if we're in the process of updating the profile
        var isUpdatingProfile = false

        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            android.util.Log.d("EditProfileFragment", "ProfileState updated: loading=${state.isLoading}, error=${state.error}, user=${state.user != null}")

            when {
                state.isLoading -> {
                    progressOverlay.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    saveButton.isEnabled = false  // Disable button while loading
                    // Mark that we're updating the profile
                    isUpdatingProfile = true
                }
                state.error != null -> {
                    progressOverlay.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.error
                    saveButton.isEnabled = true  // Re-enable button so user can try again
                    // Show error toast
                    Toast.makeText(requireContext(), "Error: ${state.error}", Toast.LENGTH_LONG).show()
                    // Reset the updating flag
                    isUpdatingProfile = false
                }
                state.user != null -> {
                    progressOverlay.visibility = View.GONE
                    errorText.visibility = View.GONE
                    saveButton.isEnabled = true  // Re-enable button

                    // Update profile image
                    if (state.user.profileImageUrl != null && state.user.profileImageUrl.isNotEmpty()) {
                        // TODO: Load image from URL using a library like Glide or Picasso
                        // For now, we'll use the default profile image
                        profileImage.setImageResource(R.drawable.default_profile)
                    } else {
                        // Use placeholder for profile image
                        profileImage.setImageResource(R.drawable.ic_person)
                    }

                    // If we were updating the profile and now we have user data, it means the update was successful
                    if (isUpdatingProfile) {
                        // Show success message
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()

                        // Navigate back to profile fragment
                        parentFragmentManager.popBackStack()

                        // Reset the updating flag
                        isUpdatingProfile = false
                    }
                }
                else -> {
                    // If we reach here, there's no loading, no error, but also no user
                    // This can happen if the ViewModel is in an inconsistent state
                    progressOverlay.visibility = View.GONE
                    errorText.visibility = View.GONE
                    saveButton.isEnabled = true
                    isUpdatingProfile = false
                }
            }
        }
    }

    private fun loadCurrentUserData() {
        val state = viewModel.profileState.value

        // Make sure UI is in a non-loading state initially
        progressOverlay.visibility = View.GONE
        saveButton.isEnabled = true

        if (state?.user != null) {
            // Parse the name into first and last name
            val nameParts = state.user.name.split(" ", limit = 2)
            val firstName = nameParts[0]
            val lastName = if (nameParts.size > 1) nameParts[1] else ""

            // Set the values in the edit texts
            firstNameEditText.setText(firstName)
            lastNameEditText.setText(lastName)
            emailEditText.setText(state.user.email)

            // Set profile image
            if (state.user.profileImageUrl != null && state.user.profileImageUrl.isNotEmpty()) {
                // TODO: Load image from URL using a library like Glide or Picasso
                // For now, we'll use the default profile image
                profileImage.setImageResource(R.drawable.default_profile)
            } else {
                // Use placeholder for profile image
                profileImage.setImageResource(R.drawable.ic_person)
            }
        } else {
            // If user data is not available in the ViewModel, try to get it from preferences
            val firstName = PreferenceUtils.getUserFirstName(requireContext())
            val lastName = PreferenceUtils.getUserLastName(requireContext())
            val email = PreferenceUtils.getUserEmail(requireContext())

            if (firstName != null) {
                firstNameEditText.setText(firstName)
            }
            if (lastName != null) {
                lastNameEditText.setText(lastName)
            }
            if (email != null) {
                emailEditText.setText(email)
            }
        }

        // Ensure contact details field is loaded if available
        val contactDetails = PreferenceUtils.getUserContactDetails(requireContext())
        if (!contactDetails.isNullOrEmpty()) {
            contactDetailsEditText.setText(contactDetails)
        }

        // Load username if available
        val username = PreferenceUtils.getUserUsername(requireContext())
        if (!username.isNullOrEmpty()) {
            usernameEditText.setText(username)
        } else {
            // Default to email as username if not set
            val email = PreferenceUtils.getUserEmail(requireContext())
            if (!email.isNullOrEmpty()) {
                usernameEditText.setText(email)
            }
        }

        // Load bio if available
        val bio = PreferenceUtils.getUserBio(requireContext())
        if (!bio.isNullOrEmpty()) {
            bioEditText.setText(bio)
        }
    }

    private fun saveProfile() {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val contactDetails = contactDetailsEditText.text.toString().trim()
        val bio = bioEditText.text.toString().trim()

        // Validate inputs
        if (firstName.isEmpty()) {
            Toast.makeText(requireContext(), "First name cannot be empty", Toast.LENGTH_SHORT).show()
            firstNameEditText.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Last name cannot be empty", Toast.LENGTH_SHORT).show()
            lastNameEditText.requestFocus()
            return
        }

        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show()
            usernameEditText.requestFocus()
            return
        }

        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email cannot be empty", Toast.LENGTH_SHORT).show()
            emailEditText.requestFocus()
            return
        }

        // Simple email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            emailEditText.requestFocus()
            return
        }

        // Log the data being saved
        android.util.Log.d("EditProfileFragment", "Saving profile: firstName=$firstName, lastName=$lastName, username=$username, email=$email, contactDetails=$contactDetails, bio=$bio")

        // Save additional fields to preferences immediately (as a backup)
        PreferenceUtils.saveUserContactDetails(requireContext(), contactDetails)
        PreferenceUtils.saveUserUsername(requireContext(), username)
        PreferenceUtils.saveUserBio(requireContext(), bio)

        // Show loading state
        progressOverlay.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        saveButton.isEnabled = false

        // Update user profile with contact details
        viewModel.updateUserProfile("$firstName $lastName", email, contactDetails)

        // The rest of the process (saving to preferences, showing success message, navigation)
        // will be handled in the observer when the update is successful
    }

    companion object {
        fun newInstance(): EditProfileFragment {
            return EditProfileFragment()
        }
    }
}
