package com.mobile.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mobile.data.model.User
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import kotlinx.coroutines.launch

class UserSelectionViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "UserSelectionViewModel"
    
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Keep track of the current user to exclude from the list
    private val currentUserId = PreferenceUtils.getUserId(application) ?: -1L
    
    fun loadUsers() {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // First try to load tutors (more relevant for most users to chat with)
                val tutorResult = NetworkUtils.getRandomTutors(10)
                
                tutorResult.fold(
                    onSuccess = { tutors ->
                        // Transform tutor profiles to users
                        val tutorUsers = tutors.map { tutor ->
                            User(
                                userId = tutor.id,
                                email = tutor.email,
                                passwordHash = "",
                                firstName = tutor.name.split(" ").firstOrNull() ?: "",
                                lastName = tutor.name.split(" ").drop(1).joinToString(" "),
                                roles = "TUTOR"
                            )
                        }.filter { it.userId != currentUserId }
                        
                        if (tutorUsers.isNotEmpty()) {
                            _users.value = tutorUsers
                            _isLoading.value = false
                        } else {
                            // If no tutors found, try to find any users
                            findAllUsers()
                        }
                    },
                    onFailure = { 
                        // If tutors can't be loaded, try to find any users
                        findAllUsers()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users: ${e.message}", e)
                _error.value = "Failed to load users: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun findAllUsers() {
        try {
            // This would require a backend endpoint to list all users
            // For now, we'll return an empty list and show an error
            // In a production app, you would call an API to get all users
            Log.w(TAG, "findAllUsers is not implemented in the backend")
            _users.value = emptyList()
            _error.value = "No users found. The feature to list all users is not available."
            _isLoading.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error finding all users: ${e.message}", e)
            _error.value = "Failed to load users: ${e.message}"
            _isLoading.value = false
        }
    }
} 