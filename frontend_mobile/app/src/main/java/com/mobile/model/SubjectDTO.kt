package com.mobile.model

import java.util.Date

/**
 * Data Transfer Object for Subject that matches the backend API
 */
data class SubjectDTO(
    val id: Long? = null,
    val name: String,
    val description: String,
    val tutorId: Long? = null,
    val tutorName: String? = null,
    val category: String,
    val hourlyRate: Double,
    val createdAt: Date? = null
) 