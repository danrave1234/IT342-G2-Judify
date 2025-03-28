package com.mobile.ui.courses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mobile.R
import com.mobile.ui.courses.models.Course
import com.mobile.ui.courses.models.CoursesState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for the courses screen
 */
class CoursesViewModel(application: Application) : AndroidViewModel(application) {

    private val _coursesState = MutableLiveData(CoursesState(isLoading = true))
    val coursesState: LiveData<CoursesState> = _coursesState

    /**
     * Load courses data
     */
    fun loadCourses() {
        _coursesState.value = _coursesState.value?.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Fetch courses from API
                val allCourses = com.mobile.utils.NetworkUtils.getAllCourses()
                val popularCourses = com.mobile.utils.NetworkUtils.getPopularCourses()

                _coursesState.value = _coursesState.value?.copy(
                    isLoading = false,
                    popularCourses = popularCourses,
                    allCourses = allCourses,
                    error = null
                )
            } catch (e: Exception) {
                _coursesState.value = _coursesState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load courses"
                )
            }
        }
    }

    /**
     * Load courses for a specific tutor
     * @param tutorId ID of the tutor
     */
    fun loadTutorCourses(tutorId: Long) {
        _coursesState.value = _coursesState.value?.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Fetch courses from API for the specific tutor
                val tutorCourses = com.mobile.utils.NetworkUtils.getCoursesByTutor(tutorId)

                _coursesState.value = _coursesState.value?.copy(
                    isLoading = false,
                    popularCourses = emptyList(), // Tutors don't need to see popular courses
                    allCourses = tutorCourses,
                    error = if (tutorCourses.isEmpty()) "You haven't created any courses yet" else null
                )
            } catch (e: Exception) {
                _coursesState.value = _coursesState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load your courses"
                )
            }
        }
    }

    /**
     * Search courses by query
     */
    fun searchCourses(query: String) {
        _coursesState.value = _coursesState.value?.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Search courses via API
                val filteredCourses = com.mobile.utils.NetworkUtils.searchCourses(query)

                _coursesState.value = _coursesState.value?.copy(
                    isLoading = false,
                    allCourses = filteredCourses,
                    error = if (filteredCourses.isEmpty()) "No courses found for '$query'" else null
                )
            } catch (e: Exception) {
                _coursesState.value = _coursesState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to search courses"
                )
            }
        }
    }

    /**
     * Filter courses by category
     */
    fun filterCoursesByCategory(category: String?) {
        _coursesState.value = _coursesState.value?.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Filter courses by category via API
                val filteredCourses = com.mobile.utils.NetworkUtils.getCoursesByCategory(category)

                _coursesState.value = _coursesState.value?.copy(
                    isLoading = false,
                    allCourses = filteredCourses,
                    error = if (filteredCourses.isEmpty()) "No courses found in '$category'" else null
                )
            } catch (e: Exception) {
                _coursesState.value = _coursesState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to filter courses"
                )
            }
        }
    }
}
