package com.mobile.ui.courses.models

/**
 * Data class representing a course/subject
 */
data class Course(
    val id: Long,
    val title: String,
    val subtitle: String,
    val description: String,
    val tutorCount: Int,
    val averageRating: Float,
    val averagePrice: Float,
    val category: String,
    val imageResId: Int? = null,
    val tutorId: Long? = null,
    val tutorName: String? = null
)

/**
 * Data class representing the courses state in the UI
 */
data class CoursesState(
    val isLoading: Boolean = false,
    val allCourses: List<Course> = emptyList(),
    val error: String? = null
)
