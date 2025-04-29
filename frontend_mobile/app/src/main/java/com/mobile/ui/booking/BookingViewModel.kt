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
import com.mobile.data.model.CourseDTO
import com.mobile.data.repository.BookingRepository
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class BookingViewModel(
    application: Application,
    private val tutorId: Long,
    private val subjectId: Long = -1,
    private val subjectName: String? = null
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
        Log.d("BookingViewModel", "Initialized with tutorId=$tutorId, subjectId=$subjectId, subjectName=$subjectName")
        // If we have a subject name, we'll pre-fill the subject field
        if (!subjectName.isNullOrBlank()) {
            _bookingState.value = _bookingState.value?.copy(
                selectedSubject = subjectName
            )
        }
    }

    fun loadTutorProfile() {
        viewModelScope.launch {
            try {
                _bookingState.value = _bookingState.value?.copy(isLoading = true)
                Log.d("BookingViewModel", "Loading tutor profile for tutorId: $tutorId")

                // First try to get the tutor info from the subject data
                // This can be used as a fallback if the API call fails
                val subjectName = _bookingState.value?.selectedSubject

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
                        loadTutorFromDatabase(tutorId, subjectName)
                    }
                )
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Exception in loadTutorProfile: ${e.message}", e)
                // Try to get tutor info directly from the database as fallback
                loadTutorFromDatabase(tutorId, _bookingState.value?.selectedSubject)
            }
        }
    }

    private suspend fun loadTutorFromDatabase(tutorId: Long, subjectName: String?) {
        try {
            // Try to get subject details which might contain tutor name
            val subject = if (subjectId > 0) {
                // Load subject info which should contain tutor name
                NetworkUtils.getSubjectById(subjectId)
            } else {
                null
            }

            // Use the tutorName if available, otherwise use a default name
            // This handles the case where subject.tutorName might be null
            val tutorNameFromDb = subject?.tutorName ?: "Tutor #$tutorId" 
            val expertiseFromDb = subject?.category ?: subjectName

            // Create a reasonable fallback profile using whatever info we have
            val fallbackProfile = NetworkUtils.TutorProfile(
                id = tutorId,
                name = tutorNameFromDb,
                email = "contact@judify.edu",
                bio = subjectName?.let { "Expert in $it (fallback info)" } ?: "Academic Tutor (fallback info)",
                rating = 0.0f,
                subjects = expertiseFromDb?.let { listOf("$it (fallback)") } ?: 
                            subjectName?.let { listOf("$it (fallback)") } ?: 
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
                bio = subjectName?.let { "Expert in $it (fallback info)" } ?: "Academic Tutor (fallback info)",
                rating = 0.0f, // Use 0.0f to ensure we're not using dummy data
                subjects = subjectName?.let { listOf("$it (fallback)") } ?: listOf("No subjects available"),
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

    /**
     * Generates time slots from tutor availability
     * @param availability List of tutor availability objects
     * @return List of time slot strings in format "HH:MM - HH:MM"
     */
    private fun generateTimeSlotsFromAvailability(availability: List<NetworkUtils.TutorAvailability>): List<String> {
        val timeSlots = mutableListOf<String>()

        // If no availability, return empty list
        if (availability.isEmpty()) {
            Log.d("BookingViewModel", "No availability found for the selected day")
            return timeSlots
        }

        Log.d("BookingViewModel", "Processing ${availability.size} availability slots: ${availability.map { "${it.dayOfWeek} ${it.startTime}-${it.endTime}" }}")

        // For each availability period, generate time slots in 30-minute intervals
        for (avail in availability) {
            try {
                // Log the raw time values for debugging
                Log.d("BookingViewModel", "Processing availability: dayOfWeek=${avail.dayOfWeek}, startTime=${avail.startTime}, endTime=${avail.endTime}")

                // Normalize the time format to handle various input formats
                val (startHour, startMinute) = parseTimeToHourAndMinute(avail.startTime)
                val (endHour, endMinute) = parseTimeToHourAndMinute(avail.endTime)

                Log.d("BookingViewModel", "Parsed times: startHour=$startHour, startMinute=$startMinute, endHour=$endHour, endMinute=$endMinute")

                // Generate time slots in 30-minute intervals
                var currentHour = startHour
                var currentMinute = startMinute

                while (currentHour < endHour || (currentHour == endHour && currentMinute < endMinute)) {
                    // Calculate end time of this slot (30 minutes later)
                    var slotEndHour = currentHour
                    var slotEndMinute = currentMinute + 30

                    // Handle minute overflow
                    if (slotEndMinute >= 60) {
                        slotEndHour += 1
                        slotEndMinute -= 60
                    }

                    // Only add the slot if it ends before or at the availability end time
                    if (slotEndHour < endHour || (slotEndHour == endHour && slotEndMinute <= endMinute)) {
                        val timeSlot = String.format("%02d:%02d - %02d:%02d", currentHour, currentMinute, slotEndHour, slotEndMinute)
                        timeSlots.add(timeSlot)
                        Log.d("BookingViewModel", "Added time slot: $timeSlot")
                    }

                    // Move to next slot
                    currentMinute += 30
                    if (currentMinute >= 60) {
                        currentHour += 1
                        currentMinute -= 60
                    }
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error parsing availability times: ${e.message}", e)
                // Continue to next availability period
            }
        }

        Log.d("BookingViewModel", "Generated ${timeSlots.size} time slots: $timeSlots")
        return timeSlots
    }

    /**
     * Parse time string to hour and minute, handling various formats
     * @param timeString The time string to parse (e.g., "9:00", "9:00 AM", "09:00", "12:00", etc.)
     * @return Pair of hour (0-23) and minute (0-59)
     */
    private fun parseTimeToHourAndMinute(timeString: String): Pair<Int, Int> {
        try {
            // Remove any extra whitespace
            val trimmedTime = timeString.trim()

            // Check if time contains AM/PM
            val hasAmPm = trimmedTime.contains(" AM", ignoreCase = true) || 
                          trimmedTime.contains(" PM", ignoreCase = true) ||
                          trimmedTime.contains("AM", ignoreCase = true) || 
                          trimmedTime.contains("PM", ignoreCase = true)

            if (hasAmPm) {
                // Handle AM/PM format
                val isAm = trimmedTime.contains("AM", ignoreCase = true)
                val isPm = trimmedTime.contains("PM", ignoreCase = true)

                // Remove AM/PM and split by colon
                val timePart = trimmedTime
                    .replace(" AM", "", ignoreCase = true)
                    .replace(" PM", "", ignoreCase = true)
                    .replace("AM", "", ignoreCase = true)
                    .replace("PM", "", ignoreCase = true)
                    .trim()

                val parts = timePart.split(":")
                var hour = parts[0].toInt()
                val minute = if (parts.size > 1) parts[1].toInt() else 0

                // Adjust hour for 12-hour format
                if (isPm && hour < 12) {
                    hour += 12
                } else if (isAm && hour == 12) {
                    hour = 0
                }

                return Pair(hour, minute)
            } else {
                // Handle 24-hour format (HH:MM or H:MM)
                val parts = trimmedTime.split(":")
                val hour = parts[0].toInt()
                val minute = if (parts.size > 1) parts[1].toInt() else 0

                // Log for debugging
                Log.d("BookingViewModel", "Parsed 24-hour time: $hour:$minute from $timeString")

                return Pair(hour, minute)
            }
        } catch (e: Exception) {
            Log.e("BookingViewModel", "Error parsing time string '$timeString': ${e.message}", e)
            // Default to 12:00 PM if parsing fails (more likely to be within tutor hours)
            return Pair(12, 0)
        }
    }

    /**
     * Loads availability slots for a tutor on a specific day and date
     * @param dayOfWeek The day of week (e.g. "MONDAY")
     * @param specificDate The specific date in yyyy-MM-dd format
     */
    fun loadTutorAvailability(dayOfWeek: String, specificDate: String? = null) {
        _availabilityState.value = _availabilityState.value?.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // First try to get tutor availability for the specific date
                val result = if (!specificDate.isNullOrEmpty()) {
                    // Try to get availability for the specific date first
                    NetworkUtils.getTutorAvailabilityByDate(tutorId, specificDate)
                } else if (dayOfWeek.isNotEmpty()) {
                    // Fall back to getting availability by day of week if no specific date
                    NetworkUtils.getTutorAvailabilityByDay(tutorId, dayOfWeek)
                } else {
                    // Last resort: get all availability
                    NetworkUtils.getTutorAvailability(tutorId)
                }

                result.fold(
                    onSuccess = { availability ->
                        // Generate time slots from availability
                        val timeSlots = generateTimeSlotsFromAvailability(availability)

                        _availabilityState.postValue(
                            _availabilityState.value?.copy(
                                availability = availability,
                                timeSlots = timeSlots,
                                isLoading = false,
                                error = null
                            )
                        )
                    },
                    onFailure = { exception ->
                        Log.e("BookingViewModel", "Failed to load tutor availability: ${exception.message}", exception)

                        _availabilityState.postValue(
                            _availabilityState.value?.copy(
                                isLoading = false,
                                error = "Failed to load availability: ${exception.message}"
                            )
                        )
                    }
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
                val learnerId = sharedPreferences.getLong("user_id", -1).toString()

                if (learnerId == "-1") {
                    _bookingState.value = _bookingState.value?.copy(
                        isLoading = false,
                        error = "User not found. Please login again."
                    )
                    callback(false)
                    return@launch
                }

                // Create a tutoring session through the NetworkUtils API
                val result = NetworkUtils.createTutoringSession(
                    tutorId = tutorId,
                    studentId = learnerId.toLong(),
                    startTime = startTime,
                    endTime = endTime,
                    subject = subject,
                    sessionType = sessionType,
                    notes = notes
                )

                result.fold(
                    onSuccess = { _ ->
                        _bookingState.value = _bookingState.value?.copy(
                            isLoading = false,
                            bookingComplete = true
                        )
                        callback(true)
                    },
                    onFailure = { exception ->
                        // Fall back to the old method if the API call fails
                        try {
                            // Create booking object for the legacy API
                            val legacyBooking = Booking(
                                id = UUID.randomUUID().toString(),
                                learnerId = learnerId,
                                tutorId = tutorId.toString(),
                                scheduleId = UUID.randomUUID().toString(), // This should come from backend
                                status = "pending",
                                startTime = startTime,
                                endTime = endTime,
                                subject = subject,
                                sessionType = sessionType,
                                notes = notes,
                                createdAt = Date()
                            )

                            // Attempt to create the booking
                            val fallbackResult = repository.createBooking(legacyBooking)
                            fallbackResult.fold(
                                onSuccess = {
                                    _bookingState.value = _bookingState.value?.copy(
                                        isLoading = false,
                                        bookingComplete = true,
                                        error = "Created using legacy system. Some features may be limited."
                                    )
                                    callback(true)
                                },
                                onFailure = { fallbackException ->
                                    _bookingState.value = _bookingState.value?.copy(
                                        isLoading = false,
                                        error = fallbackException.message ?: "Failed to create booking"
                                    )
                                    callback(false)
                                }
                            )
                        } catch (e: Exception) {
                            _bookingState.value = _bookingState.value?.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to create booking"
                            )
                            callback(false)
                        }
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
        val learnerId = sharedPreferences.getLong("user_id", -1).toString()

        if (learnerId == "-1") {
            _error.value = "User not found. Please login again."
            _loading.value = false
            return
        }

        // TODO: Implement booking logic
        _loading.value = false
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
    val timeSlots: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) 
