package com.mobile.model

import java.util.Date

/**
 * Data Transfer Object for Course that matches the backend API
 */
data class CourseDTO(
    val id: Long? = null,
    val title: String,
    val subtitle: String,
    val description: String,
    val tutorId: Long? = null,
    val tutorName: String? = null,
    val category: String,
    val price: Double,
    val createdAt: Date? = null
)