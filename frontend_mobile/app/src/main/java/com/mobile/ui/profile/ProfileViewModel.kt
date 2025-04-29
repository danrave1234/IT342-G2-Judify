package com.mobile.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mobile.utils.PreferenceUtils // Import PreferenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mobile.data.model.User as ApiUser // Alias the User model from data layer

// Data class for user information (UI Model) - Added username
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val username: String? = null, // Added username field
    val profileImageUrl: String? = null
)

// Data class for profile state
data class ProfileState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val sessions: Int = 0, // Added default value
    val reviews: Int = 0,  // Added default value
    val messages: Int = 0, // Added default value
    val isUpdateSuccessful: Boolean = false, // Flag for successful update
    val error: String? = null
) {
    // Custom toString for better logging, handles null user
    override fun toString(): String {
        // Using user?.toString() ?: "null" ensures null is logged clearly
        return "ProfileState(isLoading=$isLoading, user=${user?.toString() ?: "null"}, sessions=$sessions, reviews=$reviews, messages=$messages, isUpdateSuccessful=$isUpdateSuccessful, error=$error)"
    }
}

/**
 * ViewModel for the profile screen
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ProfileViewModel" // Added TAG

    private val _profileState = MutableLiveData(ProfileState()) // Initialize with default state
    val profileState: LiveData<ProfileState> = _profileState

    /**
     * Load user profile data. Prioritizes preferences, falls back to backend.
     * Ensures loading state is handled correctly.
     * @param email User's email address
     */
    fun loadUserProfile(email: String) {
        // Reset update success flag when loading
        val currentVal = _profileState.value ?: ProfileState()
        if (currentVal.user == null || currentVal.isLoading || currentVal.isUpdateSuccessful) {
            // Only set loading if data isn't present or already loading, or after an update
            _profileState.value = ProfileState(isLoading = true) // Start with a clean loading state, reset flags
            Log.d(TAG, "loadUserProfile called for $email. State set to loading=true.")
        } else {
            Log.d(TAG, "loadUserProfile called for $email, but user data already exists. Refreshing might happen in background if needed.")
            // Keep existing user data, but ensure loading is false and error is null, reset update flag
            _profileState.value = currentVal.copy(isLoading = false, error = null, isUpdateSuccessful = false)
        }


        viewModelScope.launch {
            try {
                // Try loading from preferences first
                val context = getApplication<Application>()
                val firstName = PreferenceUtils.getUserFirstName(context) ?: ""
                val lastName = PreferenceUtils.getUserLastName(context) ?: ""
                val usernamePref = PreferenceUtils.getUserUsername(context) // Load username from prefs
                val userId = PreferenceUtils.getUserId(context)

                if (firstName.isNotEmpty() && lastName.isNotEmpty() && userId != null) {
                    Log.d(TAG, "User details found in preferences. UserID: $userId")
                    val user = User(
                        id = userId,
                        name = "$firstName $lastName".trim(),
                        email = email,
                        username = usernamePref, // Include username
                        profileImageUrl = null // Profile image URL not stored in prefs currently
                    )
                    // Preserve stats if they exist, reset update flag
                    _profileState.postValue(_profileState.value?.copy(isLoading = false, user = user, isUpdateSuccessful = false))
                    Log.d(TAG, "State updated from preferences: ${_profileState.value}")
                } else {
                    // If preferences are incomplete, fetch from backend
                    Log.w(TAG, "User details incomplete/missing in preferences. Fetching from backend.")
                    // Ensure loading state is true before backend fetch
                    if (_profileState.value?.isLoading == false) {
                        withContext(Dispatchers.Main) { // Ensure state update is on Main thread
                            _profileState.value = _profileState.value?.copy(isLoading = true, isUpdateSuccessful = false)
                        }
                        Log.d(TAG, "Setting loading=true before backend fetch.")
                    }
                    fetchUserFromBackend(email) // Fetch from backend
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}", e)
                _profileState.postValue(ProfileState(isLoading = false, error = "Failed to load profile: ${e.message}", isUpdateSuccessful = false))
                Log.d(TAG, "State updated after error: ${_profileState.value}")
            }
        }
    }

    /**
     * Fetch user profile data from the backend. MUST be called from within a coroutine scope.
     * @param email User's email address
     */
    private suspend fun fetchUserFromBackend(email: String) {
        // Ensure loading state is true while fetching (might be redundant but safe)
        if (_profileState.value?.isLoading == false) {
            withContext(Dispatchers.Main) {
                // Preserve stats if they exist, reset update flag
                _profileState.value = _profileState.value?.copy(isLoading = true, isUpdateSuccessful = false)
                Log.d(TAG, "Ensuring loading=true during backend fetch.")
            }
        }

        try {
            Log.d(TAG, "Fetching user from backend for email: $email")
            val result = com.mobile.utils.NetworkUtils.findUserByEmail(email)

            result.fold(
                onSuccess = { userEntity ->
                    Log.d(TAG, "Successfully fetched user from backend: ${userEntity.userId}")
                    val user = User(
                        id = userEntity.userId ?: 0L, // Provide default if null
                        name = "${userEntity.firstName} ${userEntity.lastName}".trim(),
                        email = userEntity.email,
                        username = userEntity.username, // Get username from backend response
                        profileImageUrl = userEntity.profilePicture
                    )
                    // Preserve stats if they exist, reset update flag
                    _profileState.postValue(_profileState.value?.copy(isLoading = false, user = user, isUpdateSuccessful = false))
                    Log.d(TAG, "State updated from backend: ${_profileState.value}")

                    // Save fetched details to preferences
                    val context = getApplication<Application>()
                    PreferenceUtils.saveUserDetails(
                        context,
                        userEntity.firstName,
                        userEntity.lastName,
                        userEntity.email,
                        userEntity.roles // Use role from response
                    )
                    userEntity.userId?.let { PreferenceUtils.saveUserId(context, it) }
                    userEntity.username?.let { PreferenceUtils.saveUserUsername(context, it) } // Save username if available
                    userEntity.contactDetails?.let { PreferenceUtils.saveUserContactDetails(context, it) } // Save contact if available

                    Log.d(TAG, "Saved fetched user details to preferences.")
                },
                onFailure = { exception ->
                    Log.e(TAG, "Failed to fetch user from backend: ${exception.message}", exception)
                    _profileState.postValue(_profileState.value?.copy(isLoading = false, error = exception.message ?: "Failed to load profile from backend", isUpdateSuccessful = false))
                    Log.d(TAG, "State updated after backend failure: ${_profileState.value}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during backend fetch: ${e.message}", e)
            _profileState.postValue(_profileState.value?.copy(isLoading = false, error = e.message ?: "Failed to load profile from backend", isUpdateSuccessful = false))
            Log.d(TAG, "State updated after backend exception: ${_profileState.value}")
        }
    }

    /**
     * Update user profile. Handles setting loading state and updating preferences/backend.
     * @param firstName First name
     * @param lastName Last name
     * @param email Email address
     * @param username Username
     * @param contactDetails Optional contact details (phone number)
     */
    fun updateUserProfile(firstName: String, lastName: String, email: String, username: String, contactDetails: String?) {
        Log.d(TAG, "updateUserProfile called: name=$firstName $lastName, email=$email, username=$username, contact=$contactDetails")
        val currentState = _profileState.value ?: ProfileState()
        // Set loading state, clear previous error and success flag
        _profileState.value = currentState.copy(isLoading = true, error = null, isUpdateSuccessful = false)
        Log.d(TAG, "State set to loading=true for update.")

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val userId = PreferenceUtils.getUserId(context)

                if (userId != null) {
                    Log.d(TAG, "Updating user ID: $userId")

                    // Prepare ApiUser object for the backend call
                    val apiUser = ApiUser(
                        userId = userId,
                        email = email,
                        passwordHash = "", // Password not updated here
                        firstName = firstName,
                        lastName = lastName,
                        profilePicture = currentState.user?.profileImageUrl, // Keep existing image URL
                        contactDetails = contactDetails ?: "", // Send empty string if null
                        roles = PreferenceUtils.getUserRole(context) ?: "LEARNER", // Get role from prefs
                        username = username // Include username
                    )

                    Log.d(TAG, "Calling NetworkUtils.updateUser for userId: $userId with user: $apiUser")
                    val result = com.mobile.utils.NetworkUtils.updateUser(apiUser)
                    Log.d(TAG, "NetworkUtils.updateUser finished. Result success: ${result.isSuccess}")

                    result.fold(
                        onSuccess = { updatedUser ->
                            Log.d(TAG, "Update success. Updating state and preferences.")
                            val profileUser = User(
                                id = updatedUser.userId ?: userId,
                                name = "$firstName $lastName", // Use the name we sent for update
                                email = updatedUser.email,
                                username = updatedUser.username, // Use updated username
                                profileImageUrl = updatedUser.profilePicture
                            )

                            // Update preferences with latest data from the response
                            PreferenceUtils.saveUserDetails(
                                context,
                                updatedUser.firstName,
                                updatedUser.lastName,
                                updatedUser.email,
                                updatedUser.roles // Use role from response
                            )
                            updatedUser.userId?.let { PreferenceUtils.saveUserId(context, it) }
                            updatedUser.username?.let { PreferenceUtils.saveUserUsername(context, it) } // Save username from response
                            updatedUser.contactDetails?.let { PreferenceUtils.saveUserContactDetails(context, it) } // Save contact from response

                            // Update state: loading finished, user data updated, set success flag
                            // Preserve stats from the previous state
                            _profileState.postValue(currentState.copy(
                                isLoading = false,
                                user = profileUser,
                                error = null,
                                isUpdateSuccessful = true // Signal success
                            ))
                            Log.d(TAG, "State updated after update success: ${_profileState.value}")
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Update failed: ${exception.message}", exception)
                            _profileState.postValue(currentState.copy(
                                isLoading = false,
                                error = "Update failed: ${exception.message}" // Keep current data but show error
                            ))
                            Log.d(TAG, "State updated after update failure: ${_profileState.value}")
                        }
                    )
                } else {
                    Log.e(TAG, "User ID not found in preferences, cannot update.")
                    _profileState.postValue(currentState.copy(
                        isLoading = false,
                        error = "User ID not found. Please log in again."
                    ))
                    Log.d(TAG, "State updated after user ID error: ${_profileState.value}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during profile update: ${e.message}", e)
                _profileState.postValue(currentState.copy(
                    isLoading = false,
                    error = "Update error: ${e.message}"
                ))
                Log.d(TAG, "State updated after update exception: ${_profileState.value}")
            }
        }
    }

    /**
     * Clears the update success flag. Should be called after handling the success state.
     */
    fun clearUpdateStatus() {
        if (_profileState.value?.isUpdateSuccessful == true) {
            _profileState.value = _profileState.value?.copy(isUpdateSuccessful = false)
            Log.d(TAG, "Update status cleared.")
        }
    }
}