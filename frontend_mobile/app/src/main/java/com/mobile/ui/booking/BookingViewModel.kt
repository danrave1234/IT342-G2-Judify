package com.mobile.ui.booking

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mobile.model.Schedule
import com.mobile.repository.BookingRepository
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.launch

class BookingViewModel(
    application: Application,
    private val tutorId: Long,
    private val subjectId: Long = -1,
    private val subjectName: String? = null
) : AndroidViewModel(application) {
    private val TAG = "BookingViewModel"
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

    private val _unavailableTimeSlots = MutableLiveData<Set<String>>(emptySet())
    val unavailableTimeSlots: LiveData<Set<String>> = _unavailableTimeSlots

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
                        // Format in 24-hour time (HH:MM - HH:MM)
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
     * Filters out time slots that overlap with approved sessions
     * @param timeSlots List of time slots in format "HH:MM - HH:MM"
     * @param date The date for which to check overlaps in format "yyyy-MM-dd"
     * @return Pair of (available time slots, unavailable time slots)
     */
    private suspend fun filterOverlappingTimeSlots(timeSlots: List<String>, date: String): Pair<List<String>, Set<String>> {
        val availableTimeSlots = mutableListOf<String>()
        val unavailableTimeSlots = mutableSetOf<String>()

        for (timeSlot in timeSlots) {
            try {
                // Parse the time slot
                val parts = timeSlot.split(" - ")
                if (parts.size != 2) continue

                // Create ISO formatted date-time strings for the API
                val startDateTime = "${date}T${parts[0]}:00"
                val endDateTime = "${date}T${parts[1]}:00"

                // Check if this time slot overlaps with any approved sessions
                val result = NetworkUtils.checkSessionOverlap(tutorId, startDateTime, endDateTime)

                result.fold(
                    onSuccess = { hasOverlap ->
                        if (!hasOverlap) {
                            // If there's no overlap, add the time slot to available slots
                            availableTimeSlots.add(timeSlot)
                            Log.d("BookingViewModel", "Time slot $timeSlot is available")
                        } else {
                            // If there's an overlap, add the time slot to unavailable slots
                            unavailableTimeSlots.add(timeSlot)
                            Log.d("BookingViewModel", "Time slot $timeSlot overlaps with an approved session")
                        }
                    },
                    onFailure = { exception ->
                        // If there's an error, assume the slot is available
                        Log.e("BookingViewModel", "Error checking overlap for time slot $timeSlot: ${exception.message}")
                        availableTimeSlots.add(timeSlot)
                    }
                )
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error filtering time slot $timeSlot: ${e.message}")
                // If there's an error, assume the slot is available
                availableTimeSlots.add(timeSlot)
            }
        }

        Log.d("BookingViewModel", "After filtering: ${availableTimeSlots.size} available time slots, ${unavailableTimeSlots.size} unavailable time slots")
        return Pair(availableTimeSlots, unavailableTimeSlots)
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

                // Convert 12-hour format to 24-hour format
                if (isPm && hour < 12) {
                    hour += 12
                } else if (isAm && hour == 12) {
                    hour = 0
                }

                return Pair(hour, minute)
            } else {
                // Assume 24-hour format
                val parts = trimmedTime.split(":")
                val hour = parts[0].toInt()
                val minute = if (parts.size > 1) parts[1].toInt() else 0
                return Pair(hour, minute)
            }
        } catch (e: Exception) {
            Log.e("BookingViewModel", "Error parsing time string: $timeString", e)
            throw e
        }
    }

    /**
     * Format time from 24-hour (HH:MM) to 12-hour format (h:mm AM/PM)
     * @param hour Hour in 24-hour format (0-23)
     * @param minute Minute (0-59)
     * @return Time string in 12-hour format with AM/PM
     */
    private fun formatTimeTo12Hour(hour: Int, minute: Int): String {
        val isPm = hour >= 12
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        return String.format("%d:%02d %s", displayHour, minute, if (isPm) "PM" else "AM")
    }

    /**
     * Process time slots for display in the UI
     * Converts from 24-hour to 12-hour format for presentation
     */
    fun formatTimeSlotsForDisplay(timeSlots: List<String>): List<String> {
        return timeSlots.map { timeSlot ->
            // Split the time slot "HH:MM - HH:MM"
            val parts = timeSlot.split(" - ")
            if (parts.size != 2) return@map timeSlot

            // Parse start time
            val startParts = parts[0].split(":")
            val startHour = startParts[0].toInt()
            val startMinute = startParts[1].toInt()

            // Parse end time
            val endParts = parts[1].split(":")
            val endHour = endParts[0].toInt()
            val endMinute = endParts[1].toInt()

            // Format to 12-hour
            "${formatTimeTo12Hour(startHour, startMinute)} - ${formatTimeTo12Hour(endHour, endMinute)}"
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

                        // Filter out time slots that overlap with approved sessions
                        if (!specificDate.isNullOrEmpty()) {
                            // Only filter if we have a specific date
                            val (availableTimeSlots, unavailableTimeSlots) = filterOverlappingTimeSlots(timeSlots, specificDate)

                            // Update the state with available time slots
                            _availabilityState.postValue(
                                _availabilityState.value?.copy(
                                    availability = availability,
                                    timeSlots = availableTimeSlots,
                                    isLoading = false,
                                    error = null
                                )
                            )

                            // Store unavailable time slots for UI to display as disabled
                            _unavailableTimeSlots.postValue(unavailableTimeSlots)
                        } else {
                            // If no specific date, just use all time slots
                            _availabilityState.postValue(
                                _availabilityState.value?.copy(
                                    availability = availability,
                                    timeSlots = timeSlots,
                                    isLoading = false,
                                    error = null
                                )
                            )

                            // Clear unavailable time slots
                            _unavailableTimeSlots.postValue(emptySet())
                        }
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
                _bookingState.value = _bookingState.value?.copy(
                    isLoading = true,
                    bookingComplete = false,  // Reset booking completion state
                    error = null  // Clear any previous errors
                )

                // Get student ID from PreferenceUtils
                val userId = com.mobile.utils.PreferenceUtils.getUserId(getApplication())
                val studentId = userId?.toString() ?: "-1"

                if (studentId == "-1") {
                    _bookingState.value = _bookingState.value?.copy(
                        isLoading = false,
                        error = "User not found. Please login again."
                    )
                    callback(false)
                    return@launch
                }

                // Create a tutoring session through the NetworkUtils API
                NetworkUtils.createSession(
                    studentId,
                    tutorId,
                    startTime,
                    endTime,
                    subject,
                    sessionType,
                    "",  // Empty string for location since it's not being passed to this method
                    notes
                ).collect { result ->
                    try {
                        if (result.isSuccess) {
                            Log.d(TAG, "Session created successfully")

                            _bookingState.value = _bookingState.value?.copy(
                                isLoading = false,
                                bookingComplete = true,  // Set booking completion to true
                                error = null
                            )

                            // Call the callback with success
                            callback(true)

                        } else {
                            val error = result.exceptionOrNull()
                            Log.e(TAG, "Error creating session: ${error?.message}")

                            _bookingState.value = _bookingState.value?.copy(
                                isLoading = false,
                                bookingComplete = false,
                                error = error?.message ?: "Unknown error creating session"
                            )

                            // Call the callback with failure
                            callback(false)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception in bookSession: ${e.message}", e)

                        _bookingState.value = _bookingState.value?.copy(
                            isLoading = false,
                            bookingComplete = false,
                            error = e.message ?: "Exception creating session"
                        )

                        // Call the callback with failure
                        callback(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Exception during booking: ${e.message}", e)
                _bookingState.postValue(_bookingState.value?.copy(
                    isLoading = false,
                    bookingComplete = false,
                    error = e.message ?: "An error occurred"
                ))
                callback(false)
            }
        }
    }

    fun bookSchedule(schedule: Schedule) {
        _loading.value = true

        // Get user ID from PreferenceUtils
        val userId = com.mobile.utils.PreferenceUtils.getUserId(getApplication())
        val studentId = userId?.toString() ?: "-1"

        if (studentId == "-1") {
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
