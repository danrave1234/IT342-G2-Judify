package com.mobile.data.model

import java.util.Date

/**
 * Data class representing a user in the system
 */
data class User(
    val userId: Long? = null,
    val username: String? = null,
    val email: String,
    val passwordHash: String, // Note: Despite the name, this field stores the plain password to be sent to the server
    val firstName: String,
    val lastName: String,
    val profilePicture: String? = null,
    val contactDetails: String? = null,
    val roles: String = "LEARNER", // Default role
    val createdAt: Date? = null,
    val updatedAt: Date? = null
) 
