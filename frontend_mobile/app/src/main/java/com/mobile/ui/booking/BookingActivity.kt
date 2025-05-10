package com.mobile.ui.booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mobile.R
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PaymentUtils
import com.mobile.utils.UiUtils
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingActivity : AppCompatActivity() {

    private lateinit var viewModel: BookingViewModel

    // UI components
    private lateinit var backButton: ImageView
    private lateinit var tutorImage: CircleImageView
    private lateinit var tutorNameText: TextView
    private lateinit var tutorExpertiseText: TextView
    private lateinit var tutorRatingText: RatingBar
    private lateinit var tutorRateText: TextView
    private lateinit var notesEditText: TextInputEditText
    private lateinit var dateEditText: TextInputEditText
    private lateinit var timeEditText: TextInputEditText
    private lateinit var durationDropdown: AutoCompleteTextView
    private lateinit var subjectDropdown: AutoCompleteTextView
    private lateinit var sessionTypeDropdown: AutoCompleteTextView
    private lateinit var summaryDurationText: TextView
    private lateinit var totalPriceText: TextView
    private lateinit var bookButton: Button
    private lateinit var messageButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var timeSlotRecyclerView: RecyclerView
    private lateinit var noSlotsTextView: TextView
    private lateinit var timeSlotAdapter: TimeSlotAdapter
    private lateinit var subjectsRecyclerView: RecyclerView
    private lateinit var subjectAdapter: TutorSubjectAdapter

    // Date and time variables
    private val calendar = Calendar.getInstance()
    private var selectedDuration = 1.0
    private var tutorId: Long = 0
    private var selectedSessionType = ""
    private var selectedTimeSlot: String? = null
    private var selectedMeetingLatitude: Double = 0.0
    private var selectedMeetingLongitude: Double = 0.0
    private var selectedMeetingLocationName: String = ""
    private var selectedSubject: String = ""
    private var tutorSubjects: List<NetworkUtils.TutorSubject> = emptyList()

    // Formatters
    private val dateFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val apiDateTimeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        // Get the tutor ID from the intent
        tutorId = intent.getLongExtra(EXTRA_TUTOR_ID, -1)
        if (tutorId == -1L) {
            UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Invalid tutor ID")
            finish()
            return
        }

        // Get subject information if available
        val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1)
        val subjectName = intent.getStringExtra(EXTRA_SUBJECT_NAME)

        if (!subjectName.isNullOrEmpty()) {
            selectedSubject = subjectName
        }

        Log.d("BookingActivity", "Received: tutorId=$tutorId, subjectId=$subjectId, subjectName=$subjectName")

        // Debug: Check server connectivity
        verifyServerConnectivity()

        // Setup the ViewModel with factory
        viewModel = ViewModelProvider(
            this,
            BookingViewModelFactory(application, tutorId, subjectId, subjectName)
        ).get(BookingViewModel::class.java)

        // Initialize UI components
        initViews()
        setupListeners()

        // No need to set up toolbar as we're using a standalone back button

        // Set up observers
        observeBookingState()

        // Load tutor profile
        viewModel.loadTutorProfile()

        // Load tutor subjects
        loadTutorSubjects()
    }

    private fun verifyServerConnectivity() {
        // Run a background check of server connectivity to help with debugging
        lifecycleScope.launch {
            try {
                val result = NetworkUtils.verifyApiConnection()
                result.fold(
                    onSuccess = { message ->
                        Log.d("BookingActivity", "Server connectivity check: $message")
                    },
                    onFailure = { exception ->
                        Log.e("BookingActivity", "Server connectivity failed: ${exception.message}")
                        withContext(Dispatchers.Main) {
                            UiUtils.showWarningSnackbar(
                                findViewById(android.R.id.content),
                                "Warning: Server connection issues detected. Some features may not work properly."
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("BookingActivity", "Error checking server connectivity: ${e.message}", e)
            }
        }
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        tutorImage = findViewById(R.id.mentorImageView)
        tutorNameText = findViewById(R.id.mentorNameTextView)
        tutorExpertiseText = findViewById(R.id.mentorSpecialtyTextView)
        tutorRatingText = findViewById(R.id.mentorRatingBar)
        tutorRateText = findViewById(R.id.tutorRateText)
        notesEditText = findViewById(R.id.notesEditText)
        bookButton = findViewById(R.id.bookButton)
        messageButton = findViewById(R.id.messageButton)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorTextView)

        // Initialize time slot related views
        timeSlotRecyclerView = findViewById(R.id.timeSlotRecyclerView)
        noSlotsTextView = findViewById(R.id.noSlotsTextView)

        // Set up time slot RecyclerView
        timeSlotRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        timeSlotAdapter = TimeSlotAdapter(emptyList()) { timeSlot ->
            selectedTimeSlot = timeSlot
            // Update UI to show selected time slot
            highlightSelectedTimeSlot(timeSlot)
        }
        timeSlotRecyclerView.adapter = timeSlotAdapter

        // Initialize subjects RecyclerView
        subjectsRecyclerView = findViewById(R.id.subjectsRecyclerView)
        subjectsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        subjectsRecyclerView.isNestedScrollingEnabled = false
        subjectAdapter = TutorSubjectAdapter(emptyList()) { subject ->
            selectedSubject = subject.subject
            // Update UI to show selected subject
            highlightSelectedSubject(subject.subject)
        }
        subjectsRecyclerView.adapter = subjectAdapter

        // Set up session type
        selectedSessionType = "Online" // Default to online

        // Set up duration spinner with same options as the web version
        val durationSpinner = findViewById<Spinner>(R.id.durationSpinner)
        val durationOptions = arrayOf("1 hour", "1.5 hours", "2 hours", "2.5 hours", "3 hours")
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durationOptions)
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        durationSpinner.adapter = durationAdapter

        // Set default duration
        durationSpinner.setSelection(0)  // 1 hour default

        // Set duration change listener
        durationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val durationStr = durationOptions[position]
                // Extract the numeric part (e.g., "1.5" from "1.5 hours")
                selectedDuration = durationStr.split(" ")[0].toDouble()
                updateSessionSummary()
                
                // Refresh time slots when duration changes
                val selectedDateText = findViewById<TextView>(R.id.selectedDateTextView).text.toString()
                if (selectedDateText != "Select a date") {
                    try {
                        val date = dateFormatter.parse(selectedDateText)
                        if (date != null) {
                            calendar.time = date
                            val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())?.uppercase() ?: ""
                            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                            
                            // Reload time slots with the new duration
                            viewModel.loadTutorAvailability(dayOfWeek, formattedDate, selectedDuration)
                        }
                    } catch (e: Exception) {
                        Log.e("BookingActivity", "Error parsing date: ${e.message}")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Default to 1 hour
                selectedDuration = 1.0
                updateSessionSummary()
            }
        }
    }

    /**
     * Provide visual feedback for selected time slot instead of using Toast
     */
    private fun highlightSelectedTimeSlot(timeSlot: String) {
        // This will be handled by the adapter's selection logic
        // We can add a small text indication at the bottom of the screen
        val selectionText = findViewById<TextView>(R.id.selectionStatusTextView1)
        if (selectionText != null) {
            selectionText.text = "Selected time: $timeSlot"
            selectionText.visibility = View.VISIBLE
        }
    }

    /**
     * Provide visual feedback for selected subject instead of using Toast
     */
    private fun highlightSelectedSubject(subject: String) {
        // This will be handled by the adapter's selection logic
        // We can add a small text indication at the bottom of the screen
        val selectionText = findViewById<TextView>(R.id.selectionStatusTextView1)
        if (selectionText != null) {
            selectionText.text = "Selected subject: $subject"
            selectionText.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        // Set up back button
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Find the date selection button
        val selectDateButton = findViewById<Button>(R.id.selectDateButton)
        selectDateButton?.setOnClickListener {
            showDatePicker()
        }

        // Find radio buttons for session type
        val onlineRadioButton = findViewById<RadioButton>(R.id.onlineRadioButton)
        val inPersonRadioButton = findViewById<RadioButton>(R.id.inPersonRadioButton)

        // Find location selection elements
        val selectLocationButton = findViewById<Button>(R.id.selectLocationButton)
        val locationDetailsCard = findViewById<View>(R.id.locationDetailsCard)
        val selectedLocationText = findViewById<TextView>(R.id.selectedLocationText)

        // Set initial visibility
        selectLocationButton?.visibility = View.GONE
        locationDetailsCard?.visibility = View.GONE

        // Set up session type selection
        onlineRadioButton?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedSessionType = "Online"
                // Hide location selection UI
                selectLocationButton?.visibility = View.GONE
                locationDetailsCard?.visibility = View.GONE
            }
        }

        inPersonRadioButton?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedSessionType = "In-Person"
                // Show location selection UI
                selectLocationButton?.visibility = View.VISIBLE
                if (selectedMeetingLocationName.isNotEmpty()) {
                    locationDetailsCard?.visibility = View.VISIBLE
                    selectedLocationText?.text = selectedMeetingLocationName
                }
            }
        }

        // Set up location selection button
        selectLocationButton?.setOnClickListener {
            selectMeetingLocation()
        }

        // Set up message button click listener
        messageButton.setOnClickListener {
            startChatWithTutor()
        }

        // Book button
        bookButton.setOnClickListener {
            if (validateInputs()) {
                bookSession()
            }
        }
    }

    private fun observeBookingState() {
        viewModel.bookingState.observe(this) { state ->
            // Handle tutor profile
            state.tutorProfile?.let { profile ->
                tutorNameText.text = profile.name
                tutorExpertiseText.text = profile.subjects.joinToString(", ")
                tutorRatingText.rating = profile.rating

                // Set hourly rate with error handling
                try {
                    // Format the rate string once
                    val rateString = String.format("$%.2f/hr", profile.hourlyRate)
                    
                    // Apply to the main rate text view and light blue summary element only
                    tutorRateText?.let { it.text = rateString }
                    findViewById<TextView>(R.id.summaryCardBlueRateText3)?.text = rateString
                    
                    // Update the session summary with new rate
                    updateSessionSummary()
                } catch (e: Exception) {
                    Log.e("BookingActivity", "Error setting tutor rate text: ${e.message}", e)
                }
            }

            // Handle loading state - IMPORTANT FIX: Only show progressBar when actively loading
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Handle errors
            state.error?.let { 
                errorText.text = it
                errorText.visibility = View.VISIBLE
            } ?: run {
                errorText.visibility = View.GONE
            }

            // Handle successful booking completion
            if (state.bookingComplete) {
                // Show confirmation dialog and finish activity
                showBookingConfirmationDialog()
            }
        }

        // Observe availability state for time slots
        viewModel.availabilityState.observe(this) { state ->
            // Format the time slots for display in 12-hour format
            val formattedTimeSlots = if (state.timeSlots.isNotEmpty()) {
                viewModel.formatTimeSlotsForDisplay(state.timeSlots)
            } else {
                emptyList()
            }

            // Update the adapter
            timeSlotAdapter.updateTimeSlots(formattedTimeSlots)

            // Show/hide the loading indicator
            if (state.isLoading) {
                noSlotsTextView.text = "Loading available time slots..."
                noSlotsTextView.visibility = View.VISIBLE
                timeSlotRecyclerView.visibility = View.GONE
            } else {
                if (formattedTimeSlots.isEmpty()) {
                    if (findViewById<TextView>(R.id.selectedDateTextView).text != "Select a date") {
                        noSlotsTextView.text = "No time slots available for the selected date"
                    } else {
                        noSlotsTextView.text = "Please select a date to see available time slots"
                    }
                    noSlotsTextView.visibility = View.VISIBLE
                    timeSlotRecyclerView.visibility = View.GONE
                } else {
                    noSlotsTextView.visibility = View.GONE
                    timeSlotRecyclerView.visibility = View.VISIBLE
                }
            }

            // Log for debugging
            Log.d("BookingActivity", "Updated time slots: ${formattedTimeSlots.size} slots available")
        }

        // Observe unavailable time slots
        viewModel.unavailableTimeSlots.observe(this) { unavailableSlots ->
            // Format the unavailable time slots for display in 12-hour format
            val formattedUnavailableSlots = if (unavailableSlots.isNotEmpty()) {
                viewModel.formatTimeSlotsForDisplay(unavailableSlots.toList()).toSet()
            } else {
                emptySet()
            }

            // Update the adapter with unavailable time slots
            timeSlotAdapter.setUnavailableTimeSlots(formattedUnavailableSlots)

            // Log for debugging
            Log.d("BookingActivity", "Updated unavailable time slots: ${formattedUnavailableSlots.size} slots unavailable")
        }
    }

    private fun loadTutorSubjects() {
        lifecycleScope.launch {
            try {
                val result = NetworkUtils.getSubjectsByTutorProfileId(tutorId)
                result.fold(
                    onSuccess = { subjects ->
                        withContext(Dispatchers.Main) {
                            // Debug log to check how many subjects are being returned
                            Log.d("BookingActivity", "Loaded ${subjects.size} subjects: ${subjects.map { it.subject }}")

                            tutorSubjects = subjects
                            
                            // Adjust grid span count based on number of subjects
                            val spanCount = when {
                                subjects.size <= 2 -> 2
                                subjects.size <= 4 -> 2
                                else -> 3
                            }
                            (subjectsRecyclerView.layoutManager as GridLayoutManager).spanCount = spanCount
                            
                            // Update the adapter with all subjects
                            subjectAdapter.updateSubjects(subjects)

                            // If we already have a subject selected from intent, check if it's valid
                            if (selectedSubject.isNotEmpty()) {
                                // Verify the selected subject is in the list
                                val validSubject = subjects.any { it.subject.equals(selectedSubject, ignoreCase = true) }
                                if (!validSubject && subjects.isNotEmpty()) {
                                    // If not, select the first subject
                                    selectedSubject = subjects[0].subject
                                }

                                // Find the index of the selected subject and select it in the adapter
                                val selectedIndex = subjects.indexOfFirst { it.subject.equals(selectedSubject, ignoreCase = true) }
                                if (selectedIndex >= 0) {
                                    // Programmatically select the subject in the adapter
                                    subjectAdapter.selectSubject(selectedIndex)
                                }
                            } else if (subjects.isNotEmpty()) {
                                // If no subject selected yet, select the first one
                                selectedSubject = subjects[0].subject
                                subjectAdapter.selectSubject(0)
                            }
                        }
                    },
                    onFailure = { e ->
                        Log.e("BookingActivity", "Error loading tutor subjects: ${e.message}")
                        withContext(Dispatchers.Main) {
                            // If we fail to load subjects, try using subjects from the tutor profile
                            viewModel.bookingState.value?.tutorProfile?.subjects?.let { profileSubjects ->
                                if (profileSubjects.isNotEmpty()) {
                                    Log.d("BookingActivity", "Using profile subjects: $profileSubjects")

                                    // Create TutorSubject objects from the profile subjects
                                    val subjectsFromProfile = profileSubjects.mapIndexed { index, subject ->
                                        NetworkUtils.TutorSubject(
                                            id = index.toLong(),
                                            tutorProfileId = tutorId,
                                            subject = subject,
                                            createdAt = ""
                                        )
                                    }
                                    
                                    // Adjust grid span count based on number of subjects
                                    val spanCount = when {
                                        subjectsFromProfile.size <= 2 -> 2
                                        subjectsFromProfile.size <= 4 -> 2
                                        else -> 3
                                    }
                                    (subjectsRecyclerView.layoutManager as GridLayoutManager).spanCount = spanCount
                                    
                                    tutorSubjects = subjectsFromProfile
                                    subjectAdapter.updateSubjects(subjectsFromProfile)

                                    // Select the first subject if none selected
                                    if (selectedSubject.isEmpty() && subjectsFromProfile.isNotEmpty()) {
                                        selectedSubject = subjectsFromProfile[0].subject
                                        subjectAdapter.selectSubject(0)
                                    } else if (selectedSubject.isNotEmpty()) {
                                        // Find the index of the selected subject and select it in the adapter
                                        val selectedIndex = subjectsFromProfile.indexOfFirst { it.subject.equals(selectedSubject, ignoreCase = true) }
                                        if (selectedIndex >= 0) {
                                            // Programmatically select the subject in the adapter
                                            subjectAdapter.selectSubject(selectedIndex)
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("BookingActivity", "Exception loading tutor subjects: ${e.message}", e)
            }
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Format the date
                val formattedDate = dateFormatter.format(calendar.time)

                // Update the UI with selected date
                val selectedDateTextView = findViewById<TextView>(R.id.selectedDateTextView)
                selectedDateTextView?.text = formattedDate

                // Clear the selected time slot when a new date is selected
                selectedTimeSlot = null

                // Load availability for the selected day
                val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "MONDAY"
                    Calendar.TUESDAY -> "TUESDAY"
                    Calendar.WEDNESDAY -> "WEDNESDAY"
                    Calendar.THURSDAY -> "THURSDAY"
                    Calendar.FRIDAY -> "FRIDAY"
                    Calendar.SATURDAY -> "SATURDAY"
                    Calendar.SUNDAY -> "SUNDAY"
                    else -> ""
                }

                // Format the date for API call (yyyy-MM-dd)
                val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val specificDate = apiDateFormat.format(calendar.time)

                // Pass both the day of week, specific date, and selected duration to the ViewModel
                viewModel.loadTutorAvailability(dayOfWeek, specificDate, selectedDuration)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun selectMeetingLocation() {
        // Launch map picker activity for location selection
        val intent = Intent(this, MapPickerActivity::class.java)
        startActivityForResult(intent, REQUEST_LOCATION_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_LOCATION_PICK && resultCode == RESULT_OK && data != null) {
            // Get location data from result
            selectedMeetingLatitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, 0.0)
            selectedMeetingLongitude = data.getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, 0.0)
            selectedMeetingLocationName = data.getStringExtra(MapPickerActivity.EXTRA_LOCATION_NAME) ?: ""

            // Update UI to show selected location
            val locationDetailsCard = findViewById<View>(R.id.locationDetailsCard)
            val selectedLocationText = findViewById<TextView>(R.id.selectedLocationText)

            if (selectedMeetingLocationName.isNotEmpty()) {
                locationDetailsCard?.visibility = View.VISIBLE
                selectedLocationText?.text = selectedMeetingLocationName
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val rootView = findViewById<View>(android.R.id.content)
        val errorMessages = mutableListOf<String>()

        // Check if date is selected
        val selectedDateTextView = findViewById<TextView>(R.id.selectedDateTextView)
        if (selectedDateTextView?.text.isNullOrEmpty() || selectedDateTextView?.text == "Select a date") {
            errorMessages.add("Please select a date")
            isValid = false
        }

        // Check if time slot is selected
        if (selectedTimeSlot == null) {
            errorMessages.add("Please select a time slot")
            isValid = false
        }

        // Check if subject is selected
        if (selectedSubject.isEmpty()) {
            errorMessages.add("Please select a subject")
            isValid = false
        }

        // If in-person session, check if location is selected
        if (selectedSessionType == "In-Person" && selectedMeetingLocationName.isEmpty()) {
            errorMessages.add("Please select a meeting location")
            isValid = false
        }

        // Show all validation errors in a single snackbar if there are any
        if (errorMessages.isNotEmpty()) {
            UiUtils.showErrorSnackbar(rootView, errorMessages.joinToString(", "))
        }

        return isValid
    }

    private fun bookSession() {
        showLoading(true, "Booking your session...")
        val rootView = findViewById<View>(android.R.id.content)

        if (selectedTimeSlot == null) {
            UiUtils.showErrorSnackbar(rootView, "Please select a time slot")
            showLoading(false)
            return
        }

        if (selectedSubject.isEmpty()) {
            UiUtils.showErrorSnackbar(rootView, "Please select a subject")
            showLoading(false)
            return
        }

        // Get the notes
        val notesEditText = findViewById<EditText>(R.id.notesEditText)
        val notes = notesEditText.text.toString()

        // Split the selected time slot to get start time
        val timeParts = selectedTimeSlot!!.split(" - ")
        if (timeParts.isEmpty()) {
            showLoading(false)
            return
        }

        val selectedDate = findViewById<TextView>(R.id.selectedDateTextView).text.toString()
        if (selectedDate == "Select a date") {
            UiUtils.showErrorSnackbar(rootView, "Please select a date")
            showLoading(false)
            return
        }

        try {
            // Parse the selected date
            val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            val date = dateFormat.parse(selectedDate) ?: return

            // Get calendar instance for start time
            val startCalendar = Calendar.getInstance()
            startCalendar.time = date

            // Parse the start time (in 12-hour format from the UI)
            val startTimeParts = timeParts[0].trim()
            val timeFormat12 = SimpleDateFormat("h:mm a", Locale.getDefault())
            val timeParsed = timeFormat12.parse(startTimeParts) ?: return

            val timeCalendar = Calendar.getInstance()
            timeCalendar.time = timeParsed

            // Set the time components of the start calendar
            startCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            startCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            startCalendar.set(Calendar.SECOND, 0)

            // Calculate end time based on duration
            val endCalendar = Calendar.getInstance()
            endCalendar.timeInMillis = startCalendar.timeInMillis
            endCalendar.add(Calendar.MINUTE, (selectedDuration * 60).toInt())

            // Format dates for the API
            val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val startTimeString = apiDateFormat.format(startCalendar.time)
            val endTimeString = apiDateFormat.format(endCalendar.time)

            Log.d("BookingActivity", "Booking session: Start=$startTimeString, End=$endTimeString, Subject=$selectedSubject, Type=$selectedSessionType")

            // Call ViewModel to book the session
            viewModel.bookSession(
                startTime = startTimeString,
                endTime = endTimeString,
                subject = selectedSubject,
                sessionType = selectedSessionType,
                notes = notes,
                location = if (selectedSessionType == "In-Person" && selectedMeetingLocationName.isNotEmpty()) {
                    // Format location data as a string with coordinates and name for backward compatibility
                    "Lat: $selectedMeetingLatitude, Long: $selectedMeetingLongitude, Name: $selectedMeetingLocationName"
                } else {
                    ""
                },
                latitude = if (selectedSessionType == "In-Person") selectedMeetingLatitude else null,
                longitude = if (selectedSessionType == "In-Person") selectedMeetingLongitude else null,
                locationName = if (selectedSessionType == "In-Person") selectedMeetingLocationName else null
            ) { success ->
                // Note: This callback will be called by the ViewModel and should handle hiding loading
                runOnUiThread {
                    showLoading(false)
                    if (!success) {
                        // Only show error snackbar - successful booking is handled by the observer
                        UiUtils.showErrorSnackbar(rootView, "Failed to book session. Please try again.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BookingActivity", "Error booking session: ${e.message}", e)
            UiUtils.showErrorSnackbar(rootView, "Error: ${e.message}")
            showLoading(false)
        }
    }

    /**
     * Updates the session summary section with the current duration and calculated price
     */
    private fun updateSessionSummary() {
        try {
            // Find primary text views if not already initialized
            val tutorRateTextView = findViewById<TextView>(R.id.tutorRateText)
            
            // Find summary card blue section text views with suffix 3 (light blue summary)
            val summaryCardBlueRateTextView3 = findViewById<TextView>(R.id.summaryCardBlueRateText3)
            val summaryCardBlueDurationTextView3 = findViewById<TextView>(R.id.summaryCardBlueDurationText3)
            val summaryCardBlueTotalTextView3 = findViewById<TextView>(R.id.summaryCardBlueTotalText3)
            
            // Get the tutor's hourly rate from the profile
            val tutorProfile = viewModel.bookingState.value?.tutorProfile
            val hourlyRate = tutorProfile?.hourlyRate ?: 0.0

            // Format the duration text
            val durationText = if (selectedDuration == 1.0) {
                "1 hour"
            } else {
                "$selectedDuration hours"
            }

            // Calculate the total price
            val totalPrice = hourlyRate * selectedDuration

            // Update tutor rate text view
            tutorRateTextView?.text = String.format("$%.2f/hr", hourlyRate)
            
            // Update the light blue summary section only
            summaryCardBlueRateTextView3?.text = String.format("$%.2f/hr", hourlyRate)
            summaryCardBlueDurationTextView3?.text = durationText
            summaryCardBlueTotalTextView3?.text = String.format("$%.2f", totalPrice)

            Log.d("BookingActivity", "Updated session summary: Rate=$hourlyRate, Duration=$selectedDuration, Total=$totalPrice")
        } catch (e: Exception) {
            Log.e("BookingActivity", "Error updating session summary: ${e.message}", e)
        }
    }

    /**
     * Start a conversation with the tutor
     */
    private fun startChatWithTutor() {
        lifecycleScope.launch {
            try {
                // Show progress indicator
                showLoading(true, "Starting conversation...")
                val rootView = findViewById<View>(android.R.id.content)

                // Get the current user ID from preferences
                val currentUserId = com.mobile.utils.PreferenceUtils.getUserId(this@BookingActivity)
                if (currentUserId == null || currentUserId <= 0) {
                    UiUtils.showErrorSnackbar(rootView, "You need to be logged in to send messages")
                    showLoading(false)
                    return@launch
                }

                // Get the userId from tutorId first
                val userIdResult = NetworkUtils.getUserIdFromTutorId(tutorId)
                
                if (userIdResult.isFailure) {
                    Log.e("BookingActivity", "Failed to get userId from tutorId: ${userIdResult.exceptionOrNull()?.message}")
                    showLoading(false)
                    UiUtils.showErrorSnackbar(
                        rootView,
                        "Could not get tutor's user ID: ${userIdResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
                
                val tutorUserId = userIdResult.getOrNull()
                if (tutorUserId == null) {
                    Log.e("BookingActivity", "Failed to get userId from tutorId: No user ID returned")
                    showLoading(false)
                    UiUtils.showErrorSnackbar(rootView, "Could not find tutor's user account")
                    return@launch
                }
                
                // Log the conversion for debugging
                Log.d("BookingActivity", "Converting tutorId=$tutorId to userId=$tutorUserId")

                // Create conversation with tutor's userId instead of tutorId
                val result = NetworkUtils.createConversationWithTutor(currentUserId, tutorUserId)

                // Hide progress indicator
                showLoading(false)

                // Handle result
                result.fold(
                    onSuccess = { conversation ->
                        Log.d("BookingActivity", "Created conversation: ${conversation.id}")

                        // Navigate to MessageActivity
                        val intent = Intent(this@BookingActivity, com.mobile.ui.chat.MessageActivity::class.java).apply {
                            putExtra("CONVERSATION", conversation)
                        }
                        startActivity(intent)
                    },
                    onFailure = { error ->
                        Log.e("BookingActivity", "Failed to create conversation", error)
                        UiUtils.showErrorSnackbar(
                            rootView,
                            "Could not start conversation: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                Log.e("BookingActivity", "Error starting conversation", e)
                UiUtils.showErrorSnackbar(
                    findViewById(android.R.id.content),
                    "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Show or hide loading indicators with optional message
     */
    private fun showLoading(isLoading: Boolean, message: String = "") {
        val progressOverlay = findViewById<View>(R.id.progressOverlay)
        val loadingText = findViewById<TextView>(R.id.loadingText)

        if (isLoading) {
            // Set the loading message if provided
            if (message.isNotEmpty()) {
                loadingText.text = message
            } else {
                loadingText.text = "Processing your booking..."
            }

            // Show the full screen overlay
            progressOverlay.visibility = View.VISIBLE
        } else {
            // Hide the overlay when done
            progressOverlay.visibility = View.GONE
        }
    }

    // Add a new method to show a styled confirmation dialog
    private fun showBookingConfirmationDialog() {
        try {
            Log.d("BookingActivity", "Showing booking confirmation dialog")
            val rootView = findViewById<View>(android.R.id.content)

            // Set result code immediately to indicate success
            setResult(RESULT_OK)
            Log.d("BookingActivity", "Set result code to RESULT_OK")

            // Get the tutor name from the viewModel
            val tutorName = viewModel.bookingState.value?.tutorProfile?.name ?: "the tutor"
            val message = "Your session has been booked successfully! " +
                         "You'll receive a notification when $tutorName confirms the session."

            // Show a success snackbar instead of toast
            UiUtils.showSuccessSnackbar(rootView, message)
            
            // Set result to navigate to sessions tab
            setResult(RESULT_VIEW_SESSIONS)
            
            // Directly navigate to StudentDashboardActivity and show sessions
            try {
                Log.d("BookingActivity", "Automatically redirecting to dashboard after successful booking")
                val intent = Intent(this, Class.forName("com.mobile.ui.dashboard.StudentDashboardActivity"))
                intent.putExtra("SHOW_SESSIONS", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e("BookingActivity", "Error navigating to dashboard: ${e.message}", e)
                // Fall back to just finishing if we can't navigate
                finish()
            }
        } catch (e: Exception) {
            Log.e("BookingActivity", "Error showing confirmation dialog: ${e.message}", e)
            
            // Use success snackbar as fallback
            UiUtils.showSuccessSnackbar(findViewById(android.R.id.content), "Session booked successfully!")
            
            // Fall back to just finishing if we encounter an error
            try {
                val intent = Intent(this, Class.forName("com.mobile.ui.dashboard.StudentDashboardActivity"))
                intent.putExtra("SHOW_SESSIONS", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            } catch (ex: Exception) {
                Log.e("BookingActivity", "Error in fallback navigation: ${ex.message}", ex)
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // When the up button is pressed, set result as canceled and finish
        setResult(RESULT_CANCELED)
        Log.d("BookingActivity", "Navigate up pressed, setting RESULT_CANCELED")
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        Log.d("BookingActivity", "Back button pressed, setting RESULT_CANCELED")
        super.onBackPressed()
    }

    companion object {
        const val EXTRA_TUTOR_ID = "extra_tutor_id"
        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val REQUEST_LOCATION_PICK = 1001
        const val REQUEST_BOOKING = 2001
        const val RESULT_VIEW_SESSIONS = 100 // Custom result code to show sessions

        fun newIntent(context: Context, tutorId: Long): Intent {
            return Intent(context, BookingActivity::class.java).apply {
                putExtra(EXTRA_TUTOR_ID, tutorId)
            }
        }

        fun newIntent(context: Context, tutorId: Long, subjectId: Long, subjectName: String): Intent {
            return Intent(context, BookingActivity::class.java).apply {
                putExtra(EXTRA_TUTOR_ID, tutorId)
                putExtra(EXTRA_SUBJECT_ID, subjectId)
                putExtra(EXTRA_SUBJECT_NAME, subjectName)
            }
        }
    }
}

// Subject adapter for displaying and selecting tutor subjects
class TutorSubjectAdapter(
    private var subjects: List<NetworkUtils.TutorSubject>,
    private val onSubjectSelected: (NetworkUtils.TutorSubject) -> Unit
) : RecyclerView.Adapter<TutorSubjectAdapter.SubjectViewHolder>() {

    private var selectedPosition = -1

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subjectText: TextView = itemView.findViewById(R.id.subjectNameTextView)
        val cardView: CardView = itemView as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjects[position]
        holder.subjectText.text = subject.subject

        // Set selected state with a cleaner design
        val isSelected = position == selectedPosition
        if (isSelected) {
            // Selected style
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.primary_blue)
            )
            holder.subjectText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.white)
            )
            // Increase elevation for better visual feedback
            holder.cardView.cardElevation = 4f
        } else {
            // Unselected style
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.white)
            )
            holder.subjectText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.text_primary)
            )
            // Default elevation
            holder.cardView.cardElevation = 2f
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition

            // Update UI for previous selected item if necessary
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected)
            }

            // Update UI for newly selected item
            notifyItemChanged(selectedPosition)

            // Notify callback
            onSubjectSelected(subject)
        }
    }

    override fun getItemCount(): Int = subjects.size

    fun updateSubjects(newSubjects: List<NetworkUtils.TutorSubject>) {
        subjects = newSubjects
        // Reset selection when new subjects are loaded
        selectedPosition = -1

        // Log to ensure we're updating with all subjects
        Log.d("TutorSubjectAdapter", "Updated with ${newSubjects.size} subjects: ${newSubjects.map { it.subject }}")

        notifyDataSetChanged()
    }

    fun selectSubject(position: Int) {
        if (position in 0 until subjects.size) {
            val previousSelected = selectedPosition
            selectedPosition = position

            // Update UI for previous selected item if necessary
            if (previousSelected != -1 && previousSelected != position) {
                notifyItemChanged(previousSelected)
            }

            // Update UI for newly selected item
            notifyItemChanged(selectedPosition)

            // Notify callback
            onSubjectSelected(subjects[position])
        }
    }
}
