package com.mobile.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Data class for user information
 */
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val profileImageUrl: String? = null
)

/**
 * Data class for profile state
 */
data class ProfileState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val sessions: Int = 0,
    val reviews: Int = 0,
    val messages: Int = 0,
    val error: String? = null
)

/**
 * ViewModel for the profile screen
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _profileState = MutableLiveData(ProfileState(isLoading = true))
    val profileState: LiveData<ProfileState> = _profileState

    /**
     * Load user profile data from preferences first, then try backend as fallback
     * @param email User's email address
     */
    fun loadUserProfile(email: String) {
        _profileState.value = _profileState.value?.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Get user details from PreferenceUtils
                val context = getApplication<Application>()
                val firstName = com.mobile.utils.PreferenceUtils.getUserFirstName(context) ?: ""
                val lastName = com.mobile.utils.PreferenceUtils.getUserLastName(context) ?: ""

                if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                    // Create User object from saved preferences
                    val user = User(
                        id = 0, // We don't need the ID for display purposes
                        name = "$firstName $lastName",
                        email = email,
                        profileImageUrl = null // No profile image for now
                    )

                    // Update profile state with user data and stats
                    _profileState.value = _profileState.value?.copy(
                        isLoading = false,
                        user = user,
                        sessions = 0,   // Placeholder
                        reviews = 0,    // Placeholder
                        messages = 0,   // Placeholder
                        error = null
                    )
                } else {
                    // If user data is missing from preferences, try to fetch from backend
                    fetchUserFromBackend(email)
                }
            } catch (e: Exception) {
                _profileState.value = _profileState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    /**
     * Fetch user profile data from the backend as a fallback
     * @param email User's email address
     */
    private fun fetchUserFromBackend(email: String) {
        viewModelScope.launch {
            try {
                // Fetch user data from API using email
                val result = com.mobile.utils.NetworkUtils.findUserByEmail(email)

                result.fold(
                    onSuccess = { userEntity ->
                        // Create User object from backend data
                        val user = User(
                            id = userEntity.userId ?: 0,
                            name = "${userEntity.firstName} ${userEntity.lastName}",
                            email = userEntity.email,
                            profileImageUrl = userEntity.profilePicture
                        )

                        // Update profile state with user data
                        _profileState.value = _profileState.value?.copy(
                            isLoading = false,
                            user = user,
                            sessions = 0,   // Placeholder
                            reviews = 0,    // Placeholder
                            messages = 0,   // Placeholder
                            error = null
                        )

                        // Save user details to preferences for future use
                        val context = getApplication<Application>()
                        com.mobile.utils.PreferenceUtils.saveUserDetails(
                            context,
                            userEntity.firstName,
                            userEntity.lastName,
                            userEntity.roles
                        )
                    },
                    onFailure = { exception ->
                        _profileState.value = _profileState.value?.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load profile"
                        )
                    }
                )
            } catch (e: Exception) {
                _profileState.value = _profileState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    /**
     * Get mock user data for demo purposes
     */
    private fun getMockUser(userId: Long): User {
        return User(
            id = userId,
            name = "John Doe",
            email = "john.doe@example.com",
            profileImageUrl = null // No image for mock data
        )
    }

    /**
     * Update user profile
     */
    fun updateUserProfile(name: String, email: String) {
        _profileState.value = _profileState.value?.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // In a real app, send update to API
                // For now, just update local state
                val currentUser = _profileState.value?.user
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        name = name,
                        email = email
                    )

                    _profileState.value = _profileState.value?.copy(
                        isLoading = false,
                        user = updatedUser,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _profileState.value = _profileState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update profile"
                )
            }
        }
    }
} 
