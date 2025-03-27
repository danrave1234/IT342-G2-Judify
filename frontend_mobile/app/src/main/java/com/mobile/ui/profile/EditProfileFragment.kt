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
    private lateinit var emailEditText: EditText
    private lateinit var contactDetailsEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar
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
        profileImage = view.findViewById(R.id.profileImage)
        firstNameEditText = view.findViewById(R.id.firstNameEditText)
        lastNameEditText = view.findViewById(R.id.lastNameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        contactDetailsEditText = view.findViewById(R.id.contactDetailsEditText)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)
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
                    
                    // Update profile image
                    if (state.user.profileImageUrl != null && state.user.profileImageUrl.isNotEmpty()) {
                        // TODO: Load image from URL using a library like Glide or Picasso
                        // For now, we'll use the default profile image
                        profileImage.setImageResource(R.drawable.default_profile)
                    } else {
                        // Use placeholder for profile image
                        profileImage.setImageResource(R.drawable.ic_person)
                    }
                }
            }
        }
    }
    
    private fun loadCurrentUserData() {
        val state = viewModel.profileState.value
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
    }
    
    private fun saveProfile() {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val contactDetails = contactDetailsEditText.text.toString().trim()
        
        // Validate inputs
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Update user profile
        viewModel.updateUserProfile("$firstName $lastName", email)
        
        // Save to preferences
        PreferenceUtils.saveUserDetails(requireContext(), firstName, lastName, "LEARNER")
        
        // Show success message
        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate back to profile fragment
        parentFragmentManager.popBackStack()
    }
    
    companion object {
        fun newInstance(): EditProfileFragment {
            return EditProfileFragment()
        }
    }
}