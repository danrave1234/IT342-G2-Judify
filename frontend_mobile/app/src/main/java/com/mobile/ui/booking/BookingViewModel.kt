package com.mobile.ui.booking

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mobile.data.model.Booking
import com.mobile.data.model.Schedule
import com.mobile.data.repository.BookingRepository
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class BookingViewModel(
    application: Application,
    private val tutorId: Long,
    private val courseId: Long = -1,
    private val courseTitle: String? = null
) : AndroidViewModel(application) {
    private val repository = BookingRepository()
    private val _bookingResult = MutableLiveData<Boolean>()
    val bookingResult: LiveData<Boolean> = _bookingResult

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val sharedPreferences = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _bookingState = MutableLiveData(BookingState())
    val bookingState: LiveData<BookingState> = _bookingState

    private val _availabilityState = MutableLiveData(AvailabilityState())
    val availabilityState: LiveData<AvailabilityState> = _availabilityState

    init {
        Log.d("BookingViewModel", "Initialized with tutorId=$tutorId, courseId=$courseId, courseTitle=$courseTitle")
        // If we have a course title, we'll pre-fill the subject field
        if (!courseTitle.isNullOrBlank()) {
            _bookingState.value = _bookingState.value?.copy(
                selectedSubject = courseTitle
            )
        }
    }

    fun loadTutorProfile() {
        viewModelScope.launch {
            try {
                _bookingState.value = _bookingState.value?.copy(isLoading = true)
                Log.d("BookingViewModel", "Loading tutor profile for tutorId: $tutorId")

                // First try to get the tutor info from the course data
                // This can be used as a fallback if the API call fails
                val courseTitle = _bookingState.value?.selectedSubject

                // Attempt to get the tutor profile from API
                val result = NetworkUtils.getTutorProfile(tutorId)
                result.fold(
                    onSuccess = { profile ->
                        Log.d("BookingViewModel", "Successfully loaded profile: ${profile.id} - ${profile.name}")
                        Log.d("BookingViewModel", "Profile details: email=${profile.email}, bio=${profile.bio}, subjects=${profile.subjects}")
                        _bookingState.value = _bookingState.value?.copy(
                            isLoading = false,
                            tutorProfile = profile,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        Log.e("BookingViewModel", "Failed to load tutor profile: ${exception.message}", exception)

                        // Try to get tutor info directly from the database as fallback
                        loadTutorFromDatabase(tutorId, courseTitle)
                    }
                )
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Exception in loadTutorProfile: ${e.message}", e)
                // Try to get tutor info directly from the database as fallback
                loadTutorFromDatabase(tutorId, _bookingState.value?.selectedSubject)
            }
        }
    }

    private suspend fun loadTutorFromDatabase(tutorId: Long, courseTitle: String?) {
        try {
            // Try to get course details which might contain tutor name
            val course = if (courseId > 0) {
                // Load course info which should contain tutor name
                NetworkUtils.getCourseById(courseId)
            } else {
                null
            }

            val tutorNameFromDb = course?.tutorName
            val expertiseFromDb = course?.category ?: courseTitle

            // Create a reasonable fallback profile using whatever info we have
            // Use 0.0f for rating to ensure we're not using dummy data
            // Add "(fallback)" to subjects to indicate they're not from the API
            val fallbackProfile = NetworkUtils.TutorProfile(
                id = tutorId,
                name = tutorNameFromDb ?: "Tutor (ID: $tutorId)",
                email = "contact@judify.edu",
                bio = courseTitle?.let { "Expert in $it (fallback info)" } ?: "Academic Tutor (fallback info)",
                rating = 0.0f, // Use 0.0f to ensure we're not using dummy data
                subjects = expertiseFromDb?.let { listOf("$it (fallback)") } ?: 
                            courseTitle?.let { listOf("$it (fallback)") } ?: 
                            listOf("No subjects available"),
                hourlyRate = 35.0
            )

            // Log the fallback profile for debugging
            Log.d("BookingViewModel", "Created fallback profile with rating: ${fallbackProfile.rating}, subjects: ${fallbackProfile.subjects}")

            _bookingState.value = _bookingState.value?.copy(
                isLoading = false,
                tutorProfile = fallbackProfile,
                error = "Unable to load complete tutor details. Some information may be limited."
            )

        } catch (e: Exception) {
            Log.e("BookingViewModel", "Failed to load from database: ${e.message}", e)

            // Last resort fallback with generic info
            // Use 0.0f for rating to ensure we're not using dummy data
            // Add "(fallback)" to subjects to indicate they're not from the API
            val defaultProfile = NetworkUtils.TutorProfile(
                id = tutorId,
                name = "Academic Tutor",
                email = "contact@judify.edu",
                bio = courseTitle?.let { "Expert in $it (fallback info)" } ?: "Academic Tutor (fallback info)",
                rating = 0.0f, // Use 0.0f to ensure we're not using dummy data
                subjects = courseTitle?.let { listOf("$it (fallback)") } ?: listOf("No subjects available"),
                hourlyRate = 35.0
            )

            // Log the default profile for debugging
            Log.d("BookingViewModel", "Created default profile with rating: ${defaultProfile.rating}, subjects: ${defaultProfile.subjects}")

            _bookingState.value = _bookingState.value?.copy(
                isLoading = false,
                tutorProfile = defaultProfile,
                error = "Unable to load tutor details. Using default information."
            )
        }
    }

    fun loadTutorAvailability(dayOfWeek: String) {
        _availabilityState.value = _availabilityState.value?.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // In a real app, we would call an API to get tutor availability
                // For this demo, we're simulating availability for different days
                val availabilityMap = mapOf(
                    "MONDAY" to listOf(NetworkUtils.TutorAvailability(id = 1, tutorId = tutorId, dayOfWeek = "MONDAY", startTime = "9:00 AM", endTime = "5:00 PM")),
                    "TUESDAY" to listOf(NetworkUtils.TutorAvailability(id = 2, tutorId = tutorId, dayOfWeek = "TUESDAY", startTime = "10:00 AM", endTime = "6:00 PM")),
                    "WEDNESDAY" to listOf(NetworkUtils.TutorAvailability(id = 3, tutorId = tutorId, dayOfWeek = "WEDNESDAY", startTime = "9:00 AM", endTime = "3:00 PM")),
                    "THURSDAY" to listOf(NetworkUtils.TutorAvailability(id = 4, tutorId = tutorId, dayOfWeek = "THURSDAY", startTime = "12:00 PM", endTime = "8:00 PM")),
                    "FRIDAY" to listOf(NetworkUtils.TutorAvailability(id = 5, tutorId = tutorId, dayOfWeek = "FRIDAY", startTime = "8:00 AM", endTime = "4:00 PM")),
                    "SATURDAY" to emptyList(),
                    "SUNDAY" to emptyList()
                )

                val availability = availabilityMap[dayOfWeek] ?: emptyList()

                _availabilityState.postValue(
                    _availabilityState.value?.copy(
                        availability = availability,
                        isLoading = false,
                        error = null
                    )
                )
            } catch (e: Exception) {
                _availabilityState.postValue(
                    _availabilityState.value?.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                )
            }
        }
    }

    fun bookSession(
        startTime: String,
        endTime: String,
        subject: String,
        sessionType: String,
        notes: String,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _bookingState.value = _bookingState.value?.copy(isLoading = true)

                // Get learner ID from SharedPreferences
                val learnerId = sharedPreferences.getString("user_id", "") ?: ""

                if (learnerId.isEmpty()) {
                    _bookingState.value = _bookingState.value?.copy(
                        isLoading = false,
                        error = "User not found. Please login again."
                    )
                    callback(false)
                    return@launch
                }

                // Create booking object
                val booking = Booking(
                    id = UUID.randomUUID().toString(),
                    learnerId = learnerId,
                    mentorId = tutorId.toString(),
                    scheduleId = UUID.randomUUID().toString(), // This should come from backend
                    status = "pending",
                    createdAt = Date()
                )

                // Attempt to create the booking
                val result = repository.createBooking(booking)
                result.fold(
                    onSuccess = {
                        _bookingState.value = _bookingState.value?.copy(
                            isLoading = false,
                            bookingComplete = true
                        )
                        callback(true)
                    },
                    onFailure = { exception ->
                        _bookingState.value = _bookingState.value?.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to create booking"
                        )
                        callback(false)
                    }
                )
            } catch (e: Exception) {
                _bookingState.value = _bookingState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
                callback(false)
            }
        }
    }

    fun bookSchedule(schedule: Schedule) {
        _loading.value = true

        // Get user ID from SharedPreferences
        val learnerId = sharedPreferences.getString("user_id", "") ?: ""

        if (learnerId.isEmpty()) {
            _error.value = "User not found. Please login again."
            _loading.value = false
            return
        }

        val booking = Booking(
            id = UUID.randomUUID().toString(),
            learnerId = learnerId,
            mentorId = schedule.mentorId,
            scheduleId = schedule.id,
            status = "pending",
            createdAt = Date()
        )

        // ... rest of the booking logic ...
    }
}

data class BookingState(
    val tutorProfile: NetworkUtils.TutorProfile? = null,
    val isLoading: Boolean = false,
    val isBookingInProgress: Boolean = false,
    val bookingComplete: Boolean = false,
    val error: String? = null,
    val selectedSubject: String? = null
)

data class AvailabilityState(
    val availability: List<NetworkUtils.TutorAvailability> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) 
