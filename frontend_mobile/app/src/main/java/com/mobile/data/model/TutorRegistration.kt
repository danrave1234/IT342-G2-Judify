package com.mobile.data.model

/**
 * Data class for tutor registration that combines user and tutor profile information
 */
data class TutorRegistration(
    // User information
    val username: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val contactDetails: String? = null,
    
    // Tutor profile information
    val bio: String,
    val expertise: String,
    val hourlyRate: Double,
    val subjects: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null
)