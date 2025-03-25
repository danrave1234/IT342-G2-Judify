package com.mobile.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
class ProfileViewModel : ViewModel() {
    
    private val _profileState = MutableLiveData(ProfileState(isLoading = true))
    val profileState: LiveData<ProfileState> = _profileState
    
    /**
     * Load user profile data
     */
    fun loadUserProfile(userId: Long) {
        _profileState.value = _profileState.value?.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                // In a real app, fetch user data from API
                // For now, use mock data
                val user = getMockUser(userId)
                
                // Update profile state with user data and stats
                _profileState.value = _profileState.value?.copy(
                    isLoading = false,
                    user = user,
                    sessions = 12,   // Mock session count
                    reviews = 5,     // Mock review count
                    messages = 8,     // Mock message count
                    error = null
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