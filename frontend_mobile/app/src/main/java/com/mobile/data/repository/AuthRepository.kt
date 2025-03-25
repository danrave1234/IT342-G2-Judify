package com.mobile.data.repository

import com.mobile.data.model.AuthResponse
import com.mobile.utils.NetworkUtils

/**
 * Repository class for handling authentication operations
 */
class AuthRepository {

    /**
     * Login a user with the given credentials
     * @param email User's email address
     * @param password User's password
     * @return AuthResponse that contains authentication result
     */
    suspend fun login(email: String, password: String): AuthResponse {
        val result = NetworkUtils.authenticateUser(email, password)
        return result.fold(
            onSuccess = { networkResponse ->
                // Convert NetworkUtils.AuthResponse to our AuthResponse model
                AuthResponse(
                    success = networkResponse.success,
                    isAuthenticated = networkResponse.isAuthenticated,
                    userId = networkResponse.userId,
                    email = networkResponse.email,
                    firstName = networkResponse.firstName,
                    lastName = networkResponse.lastName,
                    role = networkResponse.role
                )
            },
            onFailure = { 
                throw it 
            }
        )
    }
    
    /**
     * Register a new user
     * @param email User's email address
     * @param password User's password
     * @param firstName User's first name
     * @param lastName User's last name
     * @return AuthResponse that contains registration result
     */
    suspend fun register(email: String, password: String, firstName: String, lastName: String): AuthResponse {
        // Here we would call the NetworkUtils registerUser method and convert the result to AuthResponse
        // For now, we'll just return a failed registration
        return AuthResponse(
            success = false,
            isAuthenticated = false,
            message = "Registration not implemented yet"
        )
    }
} 