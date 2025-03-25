package com.mobile.data.model

import java.util.Date

/**
 * Data class representing a user in the system
 */
data class User(
    val userId: Long? = null,
    val email: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val profilePicture: String? = null,
    val contactDetails: String? = null,
    val roles: String = "LEARNER", // Default role
    val createdAt: Date? = null,
    val updatedAt: Date? = null
) 