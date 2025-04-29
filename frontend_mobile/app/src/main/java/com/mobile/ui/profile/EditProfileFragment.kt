package com.mobile.ui.profile

import android.os.Bundle
import android.util.Log // Added Log import
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
import androidx.lifecycle.Observer // Import Observer explicitly
import com.mobile.R
import com.mobile.utils.PreferenceUtils
import de.hdodenhof.circleimageview.CircleImageView
import androidx.appcompat.widget.Toolbar // Import Toolbar

/**
 * Fragment for editing user profile
 */
class EditProfileFragment : Fragment() {
    private val TAG = "EditProfileFragment" // Added TAG

    // UI components
    private lateinit var profileImage: CircleImageView
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var contactDetailsEditText: EditText
    private lateinit var bioEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button // Keep for potential future use or alternative UI
    private lateinit var progressBar: ProgressBar
    private lateinit var progressOverlay: View
    private lateinit var errorText: TextView
    private lateinit var toolbar: Toolbar // Add Toolbar reference

    // ViewModel
    private lateinit var viewModel: ProfileViewModel
    private var initialDataLoaded = false // Flag to prevent repopulating on state changes after initial load
    private var profileStateObserver: Observer<ProfileState>? = null // Observer instance variable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        // Initialize ViewModel - use requireActivity() to share with ProfileFragment
        viewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)

        // Initialize UI components
        initializeViews(view)

        // Set up listeners
        setupListeners()

        // Set up observers
        setupObservers()

        // Trigger profile load if needed (e.g., if coming directly here)
        // The observer will handle populating the data once loaded.
        // Check if ViewModel already has data; if not, trigger load.
        if (viewModel.profileState.value?.user == null && viewModel.profileState.value?.isLoading == false) {
            val email = PreferenceUtils.getUserEmail(requireContext())
            if (email != null) {
                Log.d(TAG, "No user data in ViewModel, triggering loadUserProfile.")
                viewModel.loadUserProfile(email)
            } else {
                Log.e(TAG, "Cannot load profile, user email not found in preferences.")
                Toast.makeText(requireContext(), "Error: User email not found.", Toast.LENGTH_LONG).show()
                errorText.text = "Error: User email not found."
                errorText.visibility = View.VISIBLE
                // Disable save button if we can't load initial data
                saveButton.isEnabled = false
            }
        } else if (viewModel.profileState.value?.user != null && !initialDataLoaded) {
            // If ViewModel already has data (e.g., navigating back), populate immediately
            Log.d(TAG, "ViewModel has data, populating UI immediately.")
            viewModel.profileState.value?.user?.let { populateUserData(it) } // Safe call
            initialDataLoaded = true
            saveButton.isEnabled = true
        } else if (viewModel.profileState.value?.isLoading == true) {
            // If currently loading, disable save button
            saveButton.isEnabled = false
        }

        return view
    }

    override fun onDestroyView() {
        // Clean up observer to prevent leaks and multiple navigations
        profileStateObserver?.let { viewModel.profileState.removeObserver(it) }
        profileStateObserver = null
        super.onDestroyView()
    }

    private fun initializeViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar) // Initialize Toolbar
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

        // Initially disable save button and hide overlay
        saveButton.isEnabled = false
        progressOverlay.visibility = View.GONE
    }

    private fun setupListeners() {
        // Handle Toolbar navigation click (Back button)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        saveButton.setOnClickListener {
            saveProfile()
        }

        // Keep the cancel button listener for explicit cancellation
        cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        profileImage.setOnClickListener {
            // TODO: Implement image selection functionality
            Toast.makeText(requireContext(), "Image selection coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        // Remove previous observer if it exists
        profileStateObserver?.let { viewModel.profileState.removeObserver(it) }

        // Create and add the new observer
        profileStateObserver = Observer { state ->
            Log.d(TAG, "Observer received state: $state")

            // Handle loading state
            progressOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            // Disable save button while loading OR if initial data hasn't loaded and there's no user data yet
            saveButton.isEnabled = !state.isLoading && initialDataLoaded

            // Handle error display
            if (state.error != null && !state.isUpdateSuccessful) { // Show error only if it's not a success state
                errorText.visibility = View.VISIBLE
                errorText.text = state.error
                if (!initialDataLoaded) { // Show toast only if initial load failed
                    Toast.makeText(requireContext(), "Error loading profile: ${state.error}", Toast.LENGTH_LONG).show()
                }
            } else if (!state.isUpdateSuccessful) { // Hide error if no error and not a success state yet
                errorText.visibility = View.GONE
            }

            // Populate UI with user data only ONCE when not loading and user is available
            if (!state.isLoading && state.user != null && !initialDataLoaded) {
                Log.d(TAG, "Populating UI from state.user")
                populateUserData(state.user)
                loadAdditionalFromPreferences() // Load bio/contact after basic user data
                initialDataLoaded = true
                saveButton.isEnabled = true // Enable save button after initial load
            } else if (!state.isLoading && state.user == null && !initialDataLoaded) {
                // If loading finished but user is null, try loading from prefs as fallback
                Log.w(TAG, "ViewModel state has null user after loading, attempting to load from Preferences.")
                loadAllFromPreferences() // Load everything from preferences
                initialDataLoaded = true
                saveButton.isEnabled = true // Enable save button even if prefs are empty
            }

            // Handle successful update navigation
            // Check isUpdateSuccessful specifically to trigger navigation only once
            if (state.isUpdateSuccessful) {
                Log.d(TAG, "Update successful, navigating back.")
                // Toast moved to ProfileFragment
                parentFragmentManager.popBackStack()
                // Let ProfileFragment handle clearing the flag
            }
        }
        viewModel.profileState.observe(viewLifecycleOwner, profileStateObserver!!)
    }

    private fun populateUserData(user: User) {
        Log.d(TAG, "Populating UI with user data: Name='${user.name}', Email='${user.email}', Username='${user.username}'")
        val nameParts = user.name.split(" ", limit = 2)
        val firstName = nameParts.getOrElse(0) { "" }
        val lastName = nameParts.getOrElse(1) { "" }

        firstNameEditText.setText(firstName)
        lastNameEditText.setText(lastName)
        emailEditText.setText(user.email)
        usernameEditText.setText(user.username ?: "") // Populate username here

        if (user.profileImageUrl != null && user.profileImageUrl.isNotEmpty()) {
            // TODO: Load image from URL using Glide or Picasso
            profileImage.setImageResource(R.drawable.default_profile)
        } else {
            profileImage.setImageResource(R.drawable.ic_person)
        }
    }

    // Loads only fields not part of the User model (bio, contact)
    private fun loadAdditionalFromPreferences() {
        Log.d(TAG, "Loading additional fields (bio, contact) from preferences.")
        val context = requireContext()
        val contactDetails = PreferenceUtils.getUserContactDetails(context)
        contactDetailsEditText.setText(contactDetails ?: "") // Set to empty string if null
        val bio = PreferenceUtils.getUserBio(context)
        bioEditText.setText(bio ?: "") // Set to empty string if null
    }

    // Loads all fields from preferences (fallback)
    private fun loadAllFromPreferences() {
        Log.d(TAG, "Loading ALL fields from preferences as fallback.")
        val context = requireContext()
        firstNameEditText.setText(PreferenceUtils.getUserFirstName(context) ?: "")
        lastNameEditText.setText(PreferenceUtils.getUserLastName(context) ?: "")
        emailEditText.setText(PreferenceUtils.getUserEmail(context) ?: "")
        usernameEditText.setText(PreferenceUtils.getUserUsername(context) ?: "")
        contactDetailsEditText.setText(PreferenceUtils.getUserContactDetails(context) ?: "")
        bioEditText.setText(PreferenceUtils.getUserBio(context) ?: "")
    }


    private fun saveProfile() {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        // Handle potentially empty strings correctly, don't save "null"
        val contactDetails = contactDetailsEditText.text.toString().trim().let { if (it.equals("null", ignoreCase = true)) "" else it }
        val bio = bioEditText.text.toString().trim().let { if (it.equals("null", ignoreCase = true)) "" else it }


        // Basic validation
        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields (First Name, Last Name, Username, Email)", Toast.LENGTH_LONG).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            emailEditText.requestFocus()
            return
        }

        Log.d(TAG, "Attempting to save profile: firstName=$firstName, lastName=$lastName, username=$username, email=$email, contact='$contactDetails', bio='$bio'")

        // Save username and bio directly to preferences immediately for faster UI update
        // Pass empty string "" if the field is empty, not null or "null"
        PreferenceUtils.saveUserUsername(requireContext(), username)
        PreferenceUtils.saveUserBio(requireContext(), bio)
        PreferenceUtils.saveUserContactDetails(requireContext(), contactDetails) // Also save contact details to prefs
        Log.d(TAG, "Username, Bio, and ContactDetails saved directly to preferences.")

        // Call ViewModel to update the profile (this will handle loading state and backend update)
        viewModel.updateUserProfile(firstName, lastName, email, username, contactDetails)

        // Observer will handle navigation and success/error messages
    }

    companion object {
        fun newInstance(): EditProfileFragment {
            return EditProfileFragment()
        }
    }
}