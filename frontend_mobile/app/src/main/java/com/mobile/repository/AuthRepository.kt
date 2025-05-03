package com.mobile.repository

import com.mobile.model.AuthResponse
import com.mobile.model.User
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
                    role = networkResponse.role,
                    message = if (!networkResponse.isAuthenticated) "Invalid email or password" else null
                )
            },
            onFailure = { exception ->
                // Handle network errors gracefully
                AuthResponse(
                    success = false,
                    isAuthenticated = false,
                    message = exception.message ?: "Login failed due to a network error"
                )
            }
        )
    }

    /**
     * Register a new user
     * @param email User's email address
     * @param username User's username
     * @param password User's password
     * @param firstName User's first name
     * @param lastName User's last name
     * @param contactDetails User's contact details (optional)
     * @return AuthResponse that contains registration result
     */
    suspend fun register(email: String, username: String, password: String, firstName: String, lastName: String, contactDetails: String? = null): AuthResponse {
        // Create a User object with the provided parameters
        val user = User(
            email = email,
            username = username,
            passwordHash = password, // Despite the name, this sends the plain password to the server
            firstName = firstName,
            lastName = lastName,
            contactDetails = contactDetails,
            roles = "LEARNER" // Default role
        )

        // Call NetworkUtils.registerUser and convert the result to AuthResponse
        val result = NetworkUtils.registerUser(user)
        return result.fold(
            onSuccess = { registeredUser ->
                AuthResponse(
                    success = true,
                    isAuthenticated = true, // Assuming successful registration means authenticated
                    userId = registeredUser.userId,
                    email = registeredUser.email,
                    firstName = registeredUser.firstName,
                    lastName = registeredUser.lastName,
                    role = registeredUser.roles
                )
            },
            onFailure = { exception ->
                AuthResponse(
                    success = false,
                    isAuthenticated = false,
                    message = exception.message ?: "Registration failed"
                )
            }
        )
    }
} 
