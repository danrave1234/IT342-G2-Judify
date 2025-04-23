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
            Toast.makeText(this, "Invalid tutor ID", Toast.LENGTH_SHORT).show()
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

        // Set up toolbar - using a modern approach without title in toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Hide the title in the toolbar

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
        notesEditText = findViewById(R.id.topicEditText)
        bookButton = findViewById(R.id.bookButton)
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
            Toast.makeText(this, "Selected time: $timeSlot", Toast.LENGTH_SHORT).show()
        }
        timeSlotRecyclerView.adapter = timeSlotAdapter

        // Initialize subjects RecyclerView
        subjectsRecyclerView = findViewById(R.id.subjectsRecyclerView)
        subjectsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        subjectsRecyclerView.isNestedScrollingEnabled = false
        subjectAdapter = TutorSubjectAdapter(emptyList()) { subject ->
            selectedSubject = subject.subject
            // Update UI to show selected subject
            Toast.makeText(this, "Selected subject: ${subject.subject}", Toast.LENGTH_SHORT).show()
        }
        subjectsRecyclerView.adapter = subjectAdapter

        // Set up session type
        selectedSessionType = "Online" // Default to online
    }

    private fun setupListeners() {
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
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Handle error state
            if (state.error != null) {
                errorText.text = state.error
                errorText.visibility = View.VISIBLE
            } else {
                errorText.visibility = View.GONE
            }

            // Update UI with tutor profile
            state.tutorProfile?.let { profile ->
                tutorNameText.text = profile.name
                tutorExpertiseText.text = profile.subjects.joinToString(", ")
                tutorRatingText.rating = profile.rating

                // If we have a selected subject, make sure it's valid for this tutor
                if (selectedSubject.isEmpty() && profile.subjects.isNotEmpty()) {
                    selectedSubject = profile.subjects[0]
                }
            }
        }

        // Observe availability state to update time slots
        viewModel.availabilityState.observe(this) { state ->
            // Update time slots in the adapter
            timeSlotAdapter.updateTimeSlots(state.timeSlots)

            // Show/hide no slots message
            if (state.timeSlots.isEmpty()) {
                // Check if we're loading or if a date has been selected
                if (state.isLoading) {
                    noSlotsTextView.text = "Loading available time slots..."
                } else if (findViewById<TextView>(R.id.selectedDateTextView).text != "Select a date") {
                    // A date has been selected but no slots are available
                    noSlotsTextView.text = "No time slots available for the selected date"
                } else {
                    // No date selected yet
                    noSlotsTextView.text = "Please select a date to see available time slots"
                }
                noSlotsTextView.visibility = View.VISIBLE
                timeSlotRecyclerView.visibility = View.GONE
            } else {
                noSlotsTextView.visibility = View.GONE
                timeSlotRecyclerView.visibility = View.VISIBLE
            }

            // Log for debugging
            Log.d("BookingActivity", "Updated time slots: ${state.timeSlots.size} slots available")
        }

        // Observe booking result
        viewModel.bookingResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Session request sent! Waiting for tutor to accept.", Toast.LENGTH_LONG).show()
                // Navigate back or to a confirmation screen
                finish()
            }
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

                // Pass both the day of week and the specific date to the ViewModel
                viewModel.loadTutorAvailability(dayOfWeek, specificDate)
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

        // Check if date is selected
        val selectedDateTextView = findViewById<TextView>(R.id.selectedDateTextView)
        if (selectedDateTextView?.text.isNullOrEmpty() || selectedDateTextView?.text == "Select a date") {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if time slot is selected
        if (selectedTimeSlot == null) {
            Toast.makeText(this, "Please select a time slot", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Check if subject is selected
        if (selectedSubject.isEmpty()) {
            Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // If in-person session, check if location is selected
        if (selectedSessionType == "In-Person" && selectedMeetingLocationName.isEmpty()) {
            Toast.makeText(this, "Please select a meeting location", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun bookSession() {
        // Get student ID from shared preferences
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val studentId = sharedPreferences.getLong("user_id", -1)

        if (studentId == -1L) {
            Toast.makeText(this, "Please login to request a session", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse selected time slot for start and end time
        val timeSlotParts = selectedTimeSlot?.split(" - ")
        if (timeSlotParts == null || timeSlotParts.size != 2) {
            Toast.makeText(this, "Invalid time slot format", Toast.LENGTH_SHORT).show()
            return
        }

        val startTimeString = timeSlotParts[0]
        val endTimeString = timeSlotParts[1]

        // Create Calendar instances for start and end times
        val startCalendar = calendar.clone() as Calendar
        val endCalendar = calendar.clone() as Calendar

        // Parse time strings to set hours and minutes
        val startTimeParts = startTimeString.split(":")
        val startHour = startTimeParts[0].toInt()
        val startMinute = startTimeParts[1].toInt()

        val endTimeParts = endTimeString.split(":")
        val endHour = endTimeParts[0].toInt()
        val endMinute = endTimeParts[1].toInt()

        startCalendar.set(Calendar.HOUR_OF_DAY, startHour)
        startCalendar.set(Calendar.MINUTE, startMinute)

        endCalendar.set(Calendar.HOUR_OF_DAY, endHour)
        endCalendar.set(Calendar.MINUTE, endMinute)

        // Format for API
        val startTimeFormatted = apiDateTimeFormatter.format(startCalendar.time)
        val endTimeFormatted = apiDateTimeFormatter.format(endCalendar.time)

        // Get notes
        val notes = notesEditText.text.toString().trim()

        // Session location data (for in-person sessions)
        val locationData = if (selectedSessionType == "In-Person") {
            "{\"latitude\":$selectedMeetingLatitude,\"longitude\":$selectedMeetingLongitude,\"name\":\"$selectedMeetingLocationName\"}"
        } else {
            null
        }

        // Book session via ViewModel
        viewModel.bookSession(
            startTime = startTimeFormatted,
            endTime = endTimeFormatted,
            subject = selectedSubject,
            sessionType = selectedSessionType,
            notes = notes,
            callback = { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Session request sent! Waiting for tutor to accept.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to request session. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_TUTOR_ID = "extra_tutor_id"
        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val REQUEST_LOCATION_PICK = 1001

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
        val subjectCard: CardView = itemView as CardView
        val subjectText: TextView = itemView.findViewById(R.id.subjectNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjects[position]
        holder.subjectText.text = subject.subject

        // Set selected state
        val isSelected = position == selectedPosition
        holder.subjectCard.setCardBackgroundColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSelected) R.color.primary_blue else R.color.white
            )
        )
        holder.subjectText.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSelected) R.color.white else R.color.black
            )
        )

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
