package com.mobile.ui.booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mobile.R
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PaymentUtils
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingActivity : AppCompatActivity() {

    private lateinit var viewModel: BookingViewModel

    // UI components
    private lateinit var toolbar: Toolbar
    private lateinit var tutorImage: CircleImageView
    private lateinit var tutorNameText: TextView
    private lateinit var tutorExpertiseText: TextView
    private lateinit var tutorRatingText: RatingBar
    private lateinit var tutorPriceText: TextView
    private lateinit var dateEditText: TextInputEditText
    private lateinit var timeEditText: TextInputEditText
    private lateinit var durationDropdown: AutoCompleteTextView
    private lateinit var subjectDropdown: AutoCompleteTextView
    private lateinit var sessionTypeDropdown: AutoCompleteTextView
    private lateinit var notesEditText: TextInputEditText
    private lateinit var tutorRateText: TextView
    private lateinit var summaryDurationText: TextView
    private lateinit var totalPriceText: TextView
    private lateinit var bookButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var timeSlotRecyclerView: RecyclerView
    private lateinit var noSlotsTextView: TextView
    private lateinit var timeSlotAdapter: TimeSlotAdapter

    // Date and time variables
    private val calendar = Calendar.getInstance()
    private var selectedDuration = 1.0
    private var tutorId: Long = 0
    private var selectedSessionType = ""
    private var selectedTimeSlot: String? = null

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
            Toast.makeText(this, "Invalid tutor ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get course information if available
        val courseId = intent.getLongExtra(EXTRA_COURSE_ID, -1)
        val courseTitle = intent.getStringExtra(EXTRA_COURSE_TITLE)

        Log.d("BookingActivity", "Received: tutorId=$tutorId, courseId=$courseId, courseTitle=$courseTitle")

        // Debug: Check server connectivity
        verifyServerConnectivity()

        // Setup the ViewModel with factory
        viewModel = ViewModelProvider(
            this,
            BookingViewModelFactory(application, tutorId, courseId, courseTitle)
        ).get(BookingViewModel::class.java)

        // Initialize UI components
        initViews()
        setupListeners()

        // Set up toolbar - using a modern approach without title in toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Hide the title in the toolbar

        // Set up observers
        observeBookingState()

        // Load tutor profile
        viewModel.loadTutorProfile()
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
                            Toast.makeText(
                                this@BookingActivity,
                                "Warning: Server connection issues detected. Some features may not work properly.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("BookingActivity", "Error checking server connectivity: ${e.message}", e)
            }
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tutorImage = findViewById(R.id.mentorImageView)
        tutorNameText = findViewById(R.id.mentorNameTextView)
        tutorExpertiseText = findViewById(R.id.mentorSpecialtyTextView)
        tutorRatingText = findViewById(R.id.mentorRatingBar)
        // tutorPriceText is not found in the layout, commenting out for now
        // tutorPriceText = findViewById(R.id.tutorPriceText)
        // dateEditText and timeEditText are not found in the layout, using alternative views
        // dateEditText = findViewById(R.id.dateEditText)
        // timeEditText = findViewById(R.id.timeEditText)
        // These dropdowns are not found in the layout, commenting out for now
        // durationDropdown = findViewById(R.id.durationDropdown)
        // subjectDropdown = findViewById(R.id.subjectDropdown)
        // sessionTypeDropdown = findViewById(R.id.sessionTypeDropdown)
        notesEditText = findViewById(R.id.topicEditText)
        // These text views are not found in the layout, commenting out for now
        // tutorRateText = findViewById(R.id.tutorRateText)
        // summaryDurationText = findViewById(R.id.summaryDurationText)
        // totalPriceText = findViewById(R.id.totalPriceText)
        bookButton = findViewById(R.id.bookButton)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorTextView)

        // Initialize time slot related views
        timeSlotRecyclerView = findViewById(R.id.timeSlotRecyclerView)
        noSlotsTextView = findViewById(R.id.noSlotsTextView)

        // Set up RecyclerView
        timeSlotRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        timeSlotAdapter = TimeSlotAdapter(emptyList()) { timeSlot ->
            selectedTimeSlot = timeSlot
            // Update UI to show selected time slot
            Toast.makeText(this, "Selected time: $timeSlot", Toast.LENGTH_SHORT).show()
        }
        timeSlotRecyclerView.adapter = timeSlotAdapter

        // Set up dropdown adapters
        setupDropdowns()
    }

    private fun setupDropdowns() {
        // The layout doesn't have dropdown elements, so we'll skip this for now
        // This method would normally set up the dropdown adapters for duration, subject, and session type

        // For now, we'll just set a default session type
        selectedSessionType = "Online" // Default to online
    }

    private fun setupListeners() {
        // The layout has different UI elements for date and time selection
        // We need to find and use the correct elements from the layout

        // Find the date selection button
        val selectDateButton = findViewById<Button>(R.id.selectDateButton)
        selectDateButton?.setOnClickListener {
            showDatePicker()
        }

        // Find radio buttons for session type
        val onlineRadioButton = findViewById<RadioButton>(R.id.onlineRadioButton)
        val inPersonRadioButton = findViewById<RadioButton>(R.id.inPersonRadioButton)

        // Set up session type selection
        onlineRadioButton?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedSessionType = "Online"
            }
        }

        inPersonRadioButton?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedSessionType = "In-Person"
            }
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
            // Handle loading state
            if (state.isLoading) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.INVISIBLE
            }

            // Handle error state
            if (state.error != null) {
                // Only show errors that should prevent booking
                if (state.error.contains("Unable to load") || state.error.contains("limited")) {
                    // These are non-critical errors, don't show them to the user
                    errorText.visibility = View.GONE
                } else {
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.error
                }
            } else {
                errorText.visibility = View.GONE
            }

            // Update UI with tutor profile
            state.tutorProfile?.let { profile ->
                updateTutorProfile(profile)
            }

            // Pre-fill subject if available
            state.selectedSubject?.let { subject ->
                // Find and set the subject field - adapting to the actual layout
                notesEditText.setText("Session about: $subject")
            }

            // Handle booking completion
            if (state.bookingComplete) {
                showBookingSuccess()
            }
        }
    }

    private fun updateTutorProfile(tutorProfile: NetworkUtils.TutorProfile) {
        try {
            // Set tutor name with proper formatting - accept any name that's not a generic fallback
            val formattedName = tutorProfile.name
                .takeIf { !it.contains("Tutor #") && !it.contains("ID:") && !it.contains("Tutor (ID:") }
                ?: "Academic Tutor"

            tutorNameText.text = formattedName

            // Set tutor expertise/subjects - use exactly what comes from the API
            val expertise = tutorProfile.subjects.joinToString(", ")
            tutorExpertiseText.text = expertise

            // Log the expertise for debugging
            Log.d("BookingActivity", "Setting expertise text to: $expertise (raw subjects: ${tutorProfile.subjects})")

            // Set rating - use exactly what comes from the API
            tutorRatingText.rating = tutorProfile.rating

            // Log the rating for debugging
            Log.d("BookingActivity", "Setting rating to: ${tutorProfile.rating}")

            // Set tutor image (if needed)
            // tutorImage.setImageResource(R.drawable.tutor_placeholder)

            // Logging for debugging
            Log.d("BookingActivity", "Updated tutor profile UI with name: ${tutorProfile.name}, subjects: ${tutorProfile.subjects}")
            Log.d("BookingActivity", "Tutor profile details - bio: ${tutorProfile.bio}, email: ${tutorProfile.email}, hourlyRate: ${tutorProfile.hourlyRate}")

        } catch (e: Exception) {
            Log.e("BookingActivity", "Error updating tutor profile UI: ${e.message}", e)
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set min date to today
        val today = Calendar.getInstance()
        datePickerDialog.datePicker.minDate = today.timeInMillis

        // Set max date to 3 months from now
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.MONTH, 3)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                updateTimeInView()
            },
            hour,
            minute,
            false
        )

        timePickerDialog.show()
    }

    private fun updateDateInView() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        // Update the selectedDateTextView instead of dateEditText
        val selectedDateTextView = findViewById<TextView>(R.id.selectedDateTextView)
        selectedDateTextView?.text = dateFormat.format(calendar.time)

        // Reset selected time slot
        selectedTimeSlot = null

        // Load time slots for the selected date
        loadTimeSlots()
    }

    private fun loadTimeSlots() {
        // Get day of week from calendar
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            Calendar.SUNDAY -> "SUNDAY"
            else -> "MONDAY" // Default to Monday
        }

        // Show loading state
        noSlotsTextView.text = "Loading available time slots..."
        noSlotsTextView.visibility = View.VISIBLE
        timeSlotRecyclerView.visibility = View.GONE

        // Load tutor availability for the selected day
        viewModel.loadTutorAvailability(dayOfWeek)

        // Observe availability state
        viewModel.availabilityState.observe(this) { state ->
            if (state.isLoading) {
                noSlotsTextView.text = "Loading available time slots..."
                noSlotsTextView.visibility = View.VISIBLE
                timeSlotRecyclerView.visibility = View.GONE
            } else if (state.error != null) {
                noSlotsTextView.text = "Error loading time slots: ${state.error}"
                noSlotsTextView.visibility = View.VISIBLE
                timeSlotRecyclerView.visibility = View.GONE
            } else if (state.availability.isEmpty()) {
                noSlotsTextView.text = "No time slots available for this date"
                noSlotsTextView.visibility = View.VISIBLE
                timeSlotRecyclerView.visibility = View.GONE
            } else {
                // Generate time slots from availability
                val timeSlots = generateTimeSlots(state.availability)

                if (timeSlots.isEmpty()) {
                    noSlotsTextView.text = "No time slots available for this date"
                    noSlotsTextView.visibility = View.VISIBLE
                    timeSlotRecyclerView.visibility = View.GONE
                } else {
                    // Update adapter with time slots
                    timeSlotAdapter.updateTimeSlots(timeSlots)
                    noSlotsTextView.visibility = View.GONE
                    timeSlotRecyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun generateTimeSlots(availability: List<NetworkUtils.TutorAvailability>): List<String> {
        val timeSlots = mutableListOf<String>()

        for (slot in availability) {
            // Parse start and end times
            val startTime = parseTime(slot.startTime)
            val endTime = parseTime(slot.endTime)

            // Generate hourly slots
            var currentTime = startTime
            while (currentTime.before(endTime)) {
                timeSlots.add(timeFormatter.format(currentTime.time))
                currentTime.add(Calendar.HOUR_OF_DAY, 1)
            }
        }

        return timeSlots
    }

    private fun parseTime(timeString: String): Calendar {
        val time = Calendar.getInstance()
        try {
            val parsedTime = timeFormatter.parse(timeString)
            if (parsedTime != null) {
                time.time = parsedTime
                time.set(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return time
    }

    private fun updateTimeInView() {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        // Since there's no specific time text view in the layout,
        // we might need to update a different view or handle this differently
        // For now, we'll just log the selected time
        val selectedTime = timeFormat.format(calendar.time)
        println("Selected time: $selectedTime")
    }

    private fun updateTotalPrice() {
        // Since totalPriceText doesn't exist in the layout, we'll just calculate the total price
        // and store it for later use, but not update any UI element
        val tutorProfile = viewModel.bookingState.value?.tutorProfile
        if (tutorProfile != null) {
            val totalPrice = tutorProfile.hourlyRate * selectedDuration
            // Store the total price for later use if needed
            // For debugging purposes, we'll log the total price
            println("Total price: $${String.format("%.2f", totalPrice)}")
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Check if date is selected
        val selectedDateTextView = findViewById<TextView>(R.id.selectedDateTextView)
        if (selectedDateTextView?.text == "Select a date") {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if date is in the future
        val now = Calendar.getInstance()
        if (calendar.before(now)) {
            Toast.makeText(this, "Please select a future date and time", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if time slot is selected
        if (selectedTimeSlot == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if topic is entered
        val topicEditText = findViewById<TextInputEditText>(R.id.topicEditText)
        if (topicEditText?.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun bookSession() {
        // Show progress
        progressBar.visibility = View.VISIBLE
        bookButton.isEnabled = false

        // Get values from inputs
        // Set the calendar time to the selected time slot
        val timeSlot = selectedTimeSlot ?: return

        try {
            // Parse the selected time slot
            val parsedTime = timeFormatter.parse(timeSlot)
            if (parsedTime != null) {
                // Set the hour and minute from the selected time slot
                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = parsedTime

                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            bookButton.isEnabled = true
            return
        }

        val startTime = apiDateTimeFormatter.format(calendar.time)

        // Calculate end time (1 hour session)
        val endCalendar = (calendar.clone() as Calendar).apply {
            add(Calendar.MINUTE, (selectedDuration * 60).toInt())
        }
        val endTime = apiDateTimeFormatter.format(endCalendar.time)

        // Get topic from the topic edit text
        val topicEditText = findViewById<TextInputEditText>(R.id.topicEditText)
        val notes = topicEditText?.text?.toString() ?: ""

        // Use the selected session type from the radio buttons
        // This was set in setupListeners()

        // Calculate total price
        val tutorProfile = viewModel.bookingState.value?.tutorProfile
        val totalPrice = tutorProfile?.hourlyRate?.times(selectedDuration) ?: 0.0

        // Create booking request
        viewModel.bookSession(
            startTime = startTime,
            endTime = endTime,
            subject = "General", // Default subject since we don't have a dropdown
            sessionType = selectedSessionType,
            notes = notes
        ) { success ->
            if (success) {
                // If booking is successful, proceed to payment
                processPayment(totalPrice)
            } else {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    bookButton.isEnabled = true
                    Toast.makeText(this, "Failed to book session", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun processPayment(amount: Double) {
        // Show payment processing state
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        errorText.text = "Processing payment..."

        // Get the tutor name for the payment description
        val tutorProfile = viewModel.bookingState.value?.tutorProfile
        val tutorName = tutorProfile?.name ?: "Tutor (ID: $tutorId)"

        // Launch coroutine for payment processing
        lifecycleScope.launch {
            try {
                // Create a payment source using Paymongo
                val callbackUrl = "https://judify.com/payment/callback"
                val result = PaymentUtils.createPaymentSource(
                    amount = amount,
                    description = "Tutoring Session with $tutorName",
                    callbackUrl = callbackUrl
                )

                result.fold(
                    onSuccess = { source ->
                        // In a real app, you would:
                        // 1. Store the payment source ID in your backend
                        // 2. Redirect the user to the payment page using the source.redirect.checkout_url
                        // 3. Handle the callback when the user completes or cancels the payment

                        // For demo purposes, we'll simulate a successful payment
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@BookingActivity,
                                "Payment processed successfully! Reference: ${source.reference}",
                                Toast.LENGTH_LONG
                            ).show()

                            // Show success and finish activity
                            progressBar.visibility = View.GONE
                            finish()
                        }
                    },
                    onFailure = { exception ->
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            bookButton.isEnabled = true
                            errorText.visibility = View.VISIBLE
                            errorText.text = "Payment error: ${exception.message}"

                            Toast.makeText(
                                this@BookingActivity,
                                "Payment failed: ${exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    bookButton.isEnabled = true
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Payment error: ${e.message}"

                    Toast.makeText(
                        this@BookingActivity,
                        "Payment failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showBookingSuccess() {
        // Show success message and finish activity
        Toast.makeText(this, "Session booked successfully!", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_TUTOR_ID = "extra_tutor_id"
        const val EXTRA_COURSE_ID = "extra_course_id"
        const val EXTRA_COURSE_TITLE = "extra_course_title"

        fun newIntent(context: Context, tutorId: Long): Intent {
            return Intent(context, BookingActivity::class.java).apply {
                putExtra(EXTRA_TUTOR_ID, tutorId)
            }
        }
    }
}

// Factory for creating BookingViewModel with a parameter
// Removing duplicate factory class since we've moved it to a separate file 
