package com.mobile.model

/**
 * Data class that captures the response from the authentication API
 */
data class AuthResponse(
    val success: Boolean = false,
    val isAuthenticated: Boolean,
    val userId: Long? = null,
    val studentId: Long? = null,
    val tutorId: Long? = null,
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "LEARNER",
    val message: String? = null
) 