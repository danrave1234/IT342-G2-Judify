package com.mobile.ui.booking

import android.app.Application
import android.content.Context
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
    private val tutorId: Long
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
    
    fun loadTutorProfile() {
        viewModelScope.launch {
            try {
                _bookingState.value = _bookingState.value?.copy(isLoading = true)
                val result = NetworkUtils.getTutorProfile(tutorId)
                result.fold(
                    onSuccess = { profile ->
                        _bookingState.value = _bookingState.value?.copy(
                            isLoading = false,
                            tutorProfile = profile
                        )
                    },
                    onFailure = { exception ->
                        _bookingState.value = _bookingState.value?.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load tutor profile"
                        )
                    }
                )
            } catch (e: Exception) {
                _bookingState.value = _bookingState.value?.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
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
    val error: String? = null
)

data class AvailabilityState(
    val availability: List<NetworkUtils.TutorAvailability> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) 