package com.mobile.ui.dashboard

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobile.R
import com.mobile.databinding.ActivityTutorDashboardBinding
import com.mobile.ui.chat.ChatFragment
import com.mobile.ui.map.MapFragment
import com.mobile.ui.profile.ProfileFragment
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TutorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorDashboardBinding
    private val TAG = "TutorDashboard"

    private lateinit var availabilityAdapter: TutorAvailabilityAdapter
    private val availabilityList = mutableListOf<NetworkUtils.TutorAvailability>()
    private var currentTutorId: Long = 0

    override fun onDestroy() {
        try {
            super.onDestroy()
        } catch (e: IllegalArgumentException) {
            // Catch the OplusScrollToTopManager exception
            // This is a system-level issue specific to Oppo/OnePlus devices
            Log.e(TAG, "Error during onDestroy: ${e.message}", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTutorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up greeting based on time of day
        setupGreeting()

        // Set up user name from preferences
        setupUserName()

        // Set up click listeners
        setupClickListeners()

        // Set up app bar behavior
        setupAppBarBehavior()

        // Set up bottom navigation
        setupBottomNavigation()

        // Set up availability RecyclerView
        setupAvailabilityRecyclerView()

        // Load real data
        loadRealData()
    }

    private fun setupAvailabilityRecyclerView() {
        // Initialize the adapter with empty list and click listeners
        availabilityAdapter = TutorAvailabilityAdapter(
            availabilityList,
            onEditClick = { availability ->
                showEditAvailabilityDialog(availability)
            },
            onDeleteClick = { availability ->
                showDeleteConfirmationDialog(availability)
            }
        )

        // Set up RecyclerView
        val recyclerView = binding.availabilityRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = availabilityAdapter

        // Set up add availability button click listener
        binding.addAvailability.setOnClickListener {
            showAddAvailabilityDialog()
        }
    }

    private fun showAddAvailabilityDialog() {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_availability, null)

        // Get references to the views
        val dayOfWeekSpinner = dialogView.findViewById<Spinner>(R.id.dayOfWeekSpinner)
        val startTimeEditText = dialogView.findViewById<EditText>(R.id.startTimeEditText)
        val endTimeEditText = dialogView.findViewById<EditText>(R.id.endTimeEditText)

        // Set up the day of week spinner
        val daysOfWeek = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, daysOfWeek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dayOfWeekSpinner.adapter = adapter

        // Set up time pickers
        startTimeEditText.setOnClickListener {
            showTimePicker(startTimeEditText)
        }

        endTimeEditText.setOnClickListener {
            showTimePicker(endTimeEditText)
        }

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set up button click listeners
        dialogView.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.saveButton).setOnClickListener {
            // Validate inputs
            val dayOfWeek = dayOfWeekSpinner.selectedItem.toString().uppercase()
            val startTime = startTimeEditText.text.toString()
            val endTime = endTimeEditText.text.toString()

            if (startTime.isEmpty() || endTime.isEmpty()) {
                Toast.makeText(this, "Please select both start and end times", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create the availability slot
            createAvailabilitySlot(dayOfWeek, startTime, endTime)

            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditAvailabilityDialog(availability: NetworkUtils.TutorAvailability) {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_availability, null)

        // Get references to the views
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title)
        titleTextView.text = "Edit Availability"

        val dayOfWeekSpinner = dialogView.findViewById<Spinner>(R.id.dayOfWeekSpinner)
        val startTimeEditText = dialogView.findViewById<EditText>(R.id.startTimeEditText)
        val endTimeEditText = dialogView.findViewById<EditText>(R.id.endTimeEditText)

        // Set up the day of week spinner
        val daysOfWeek = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, daysOfWeek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dayOfWeekSpinner.adapter = adapter

        // Set the current values
        val dayIndex = daysOfWeek.indexOfFirst { it.equals(availability.dayOfWeek, ignoreCase = true) }
        if (dayIndex >= 0) {
            dayOfWeekSpinner.setSelection(dayIndex)
        }

        startTimeEditText.setText(availability.startTime)
        endTimeEditText.setText(availability.endTime)

        // Set up time pickers
        startTimeEditText.setOnClickListener {
            showTimePicker(startTimeEditText)
        }

        endTimeEditText.setOnClickListener {
            showTimePicker(endTimeEditText)
        }

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set up button click listeners
        dialogView.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.saveButton).setOnClickListener {
            // Validate inputs
            val dayOfWeek = dayOfWeekSpinner.selectedItem.toString().uppercase()
            val startTime = startTimeEditText.text.toString()
            val endTime = endTimeEditText.text.toString()

            if (startTime.isEmpty() || endTime.isEmpty()) {
                Toast.makeText(this, "Please select both start and end times", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update the availability slot
            updateAvailabilitySlot(availability.id, dayOfWeek, startTime, endTime)

            // Dismiss the dialog
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(availability: NetworkUtils.TutorAvailability) {
        AlertDialog.Builder(this)
            .setTitle("Delete Availability")
            .setMessage("Are you sure you want to delete this availability slot?")
            .setPositiveButton("Delete") { _, _ ->
                deleteAvailabilitySlot(availability.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                // Format the time
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    selectedHour,
                    selectedMinute
                )
                editText.setText(formattedTime)
            },
            hour,
            minute,
            true // 24-hour format
        ).show()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Show the main dashboard content
                    showMainContent(true)
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_sessions -> {
                    // Show the map fragment instead of sessions
                    showMainContent(false)
                    loadFragment(MapFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_map -> {
                    // Show the map fragment
                    showMainContent(false)
                    loadFragment(MapFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_chat -> {
                    // Show the chat fragment
                    showMainContent(false)
                    loadFragment(ChatFragment())
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_profile -> {
                    // Show the profile fragment
                    showMainContent(false)
                    loadFragment(ProfileFragment())
                    return@setOnItemSelectedListener true
                }
                else -> return@setOnItemSelectedListener false
            }
        }

        // Set the home item as selected by default
        bottomNavigation.selectedItemId = R.id.navigation_home
    }

    private fun setupGreeting() {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when {
            hourOfDay < 12 -> "Good Morning 👋"
            hourOfDay < 18 -> "Good Afternoon 👋"
            else -> "Good Evening 👋"
        }

        binding.greetingText.text = greeting
    }

    private fun setupUserName() {
        // Get user's name from preferences
        val firstName = PreferenceUtils.getUserFirstName(this)
        val lastName = PreferenceUtils.getUserLastName(this)

        if (firstName != null && lastName != null) {
            binding.userNameText.text = "$firstName $lastName"
        } else {
            // Fallback to email if name is not available
            val email = PreferenceUtils.getUserEmail(this)
            if (email != null) {
                binding.userNameText.text = email
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        Log.d(TAG, "Loading fragment: ${fragment.javaClass.simpleName}")
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun showMainContent(show: Boolean) {
        Log.d(TAG, "Showing main content: $show")
        if (show) {
            binding.appBarLayout.visibility = View.VISIBLE
            binding.dashboardShadowDivider.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.GONE
        } else {
            binding.appBarLayout.visibility = View.GONE
            binding.dashboardShadowDivider.visibility = View.GONE
            binding.fragmentContainer.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        // Set up see all sessions
        binding.seeAllSessions.setOnClickListener {
            // TODO: Navigate to all sessions screen
        }

        // Set up manage subjects
        binding.manageSubjects.setOnClickListener {
            // TODO: Navigate to manage subjects screen
        }

        // Set up notification icon click
        binding.notificationIcon.setOnClickListener {
            // TODO: Navigate to notifications screen
        }

        // Set up message icon click
        binding.messageIcon.setOnClickListener {
            // TODO: Navigate to messages screen
        }

        // Set up profile image click
        binding.profileImage.setOnClickListener {
            // Show the profile fragment and hide the main content
            showMainContent(false)
            loadFragment(ProfileFragment())
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            bottomNavigation.selectedItemId = R.id.navigation_profile
        }
    }

    private fun setupAppBarBehavior() {
        // Configure the AppBarLayout behavior for smooth scrolling
        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            // The verticalOffset is 0 when the AppBarLayout is fully expanded
            // and -appBarLayout.totalScrollRange when it's fully collapsed

            // Calculate the scroll percentage (0.0 to 1.0)
            val scrollPercentage = Math.abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange.toFloat()

            // You can use this to implement visual effects based on scroll position
            if (scrollPercentage > 0.8) {
                // When mostly collapsed, increase elevation for shadow effect
                appBarLayout.elevation = 8f
            } else {
                // When expanded, keep elevation minimal
                appBarLayout.elevation = 0f
            }
        })
    }

    private fun loadRealData() {
        // Get the user email from preferences
        val userEmail = PreferenceUtils.getUserEmail(this)

        if (userEmail != null) {
            // Launch coroutine to fetch data
            lifecycleScope.launch {
                try {
                    // First find the user by email
                    val userResult = NetworkUtils.findUserByEmail(userEmail)
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        if (user != null) {
                            // Now find the tutor profile using the user ID
                            // Handle null userId
                            val userId = user.userId ?: 0L  // Default to 0 if null
                            val tutorProfileResult = NetworkUtils.findTutorByUserId(userId)
                            if (tutorProfileResult.isSuccess) {
                                val tutorProfile = tutorProfileResult.getOrNull()
                                if (tutorProfile != null) {
                                    // Set rating
                                    binding.ratingValue.text = tutorProfile.rating.toString()

                                    // Store tutor ID for later use
                                    currentTutorId = tutorProfile.id

                                    // Load subjects
                                    loadSubjects(tutorProfile.subjects)

                                    // Get tutoring sessions
                                    loadSessions(tutorProfile.id)

                                    // Load availability
                                    loadAvailability(tutorProfile.id)
                                } else {
                                    // Handle null tutor profile
                                    showError("Could not load tutor profile")
                                }
                            } else {
                                // Handle error
                                showError("Error loading tutor profile")
                            }
                        } else {
                            // Handle null user
                            showError("Could not find user with email: $userEmail")
                        }
                    } else {
                        // Handle error
                        showError("Error finding user with email: $userEmail")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading data: ${e.message}", e)
                    showError("Error loading data: ${e.message}")
                }
            }
        } else {
            // Handle case where user email is not available
            showError("User email not found")
        }
    }

    private fun loadAvailability(tutorId: Long) {
        lifecycleScope.launch {
            try {
                val result = NetworkUtils.getTutorAvailability(tutorId)
                if (result.isSuccess) {
                    val availabilitySlots = result.getOrNull() ?: emptyList()

                    // Update the list and adapter
                    availabilityList.clear()
                    availabilityList.addAll(availabilitySlots)
                    availabilityAdapter.notifyDataSetChanged()

                    // Show/hide empty state
                    if (availabilityList.isEmpty()) {
                        binding.noAvailabilityText.visibility = View.VISIBLE
                        binding.availabilityRecyclerView.visibility = View.GONE
                    } else {
                        binding.noAvailabilityText.visibility = View.GONE
                        binding.availabilityRecyclerView.visibility = View.VISIBLE
                    }
                } else {
                    // Handle error
                    showError("Error loading availability")
                    binding.noAvailabilityText.visibility = View.VISIBLE
                    binding.availabilityRecyclerView.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading availability: ${e.message}", e)
                showError("Error loading availability: ${e.message}")
                binding.noAvailabilityText.visibility = View.VISIBLE
                binding.availabilityRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun createAvailabilitySlot(dayOfWeek: String, startTime: String, endTime: String) {
        lifecycleScope.launch {
            try {
                val result = NetworkUtils.createTutorAvailability(
                    tutorId = currentTutorId,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime
                )

                if (result.isSuccess) {
                    // Reload availability data
                    loadAvailability(currentTutorId)
                    Toast.makeText(this@TutorDashboardActivity, "Availability added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Error creating availability slot")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating availability slot: ${e.message}", e)
                showError("Error creating availability slot: ${e.message}")
            }
        }
    }

    private fun updateAvailabilitySlot(id: Long, dayOfWeek: String, startTime: String, endTime: String) {
        lifecycleScope.launch {
            try {
                val result = NetworkUtils.updateTutorAvailability(
                    id = id,
                    tutorId = currentTutorId,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime
                )

                if (result.isSuccess) {
                    // Reload availability data
                    loadAvailability(currentTutorId)
                    Toast.makeText(this@TutorDashboardActivity, "Availability updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Error updating availability slot")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating availability slot: ${e.message}", e)
                showError("Error updating availability slot: ${e.message}")
            }
        }
    }

    private fun deleteAvailabilitySlot(id: Long) {
        lifecycleScope.launch {
            try {
                val result = NetworkUtils.deleteTutorAvailability(id)

                if (result.isSuccess) {
                    // Reload availability data
                    loadAvailability(currentTutorId)
                    Toast.makeText(this@TutorDashboardActivity, "Availability deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Error deleting availability slot")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting availability slot: ${e.message}", e)
                showError("Error deleting availability slot: ${e.message}")
            }
        }
    }

    private fun loadSessions(tutorId: Long) {
        lifecycleScope.launch {
            try {
                // Get all sessions for counting total
                val allSessionsResult = NetworkUtils.getTutorSessions(tutorId)
                if (allSessionsResult.isSuccess) {
                    val allSessions = allSessionsResult.getOrNull() ?: emptyList()
                    binding.totalSessionsCount.text = allSessions.size.toString()

                    // Get upcoming sessions
                    val upcomingSessionsResult = NetworkUtils.getUpcomingTutorSessions(tutorId)
                    if (upcomingSessionsResult.isSuccess) {
                        val upcomingSessions = upcomingSessionsResult.getOrNull() ?: emptyList()
                        binding.upcomingSessionsCount.text = upcomingSessions.size.toString()

                        // Display upcoming sessions
                        displaySessions(upcomingSessions)
                    } else {
                        // Handle error
                        binding.upcomingSessionsCount.text = "0"
                        showNoSessions()
                    }
                } else {
                    // Handle error
                    binding.totalSessionsCount.text = "0"
                    binding.upcomingSessionsCount.text = "0"
                    showNoSessions()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sessions: ${e.message}", e)
                binding.totalSessionsCount.text = "0"
                binding.upcomingSessionsCount.text = "0"
                showNoSessions()
            }
        }
    }

    private fun displaySessions(sessions: List<NetworkUtils.TutoringSession>) {
        // Clear existing sessions
        binding.upcomingSessionsContainer.removeAllViews()

        if (sessions.isEmpty()) {
            showNoSessions()
            return
        }

        binding.noSessionsText.visibility = View.GONE

        // Add session cards
        for (session in sessions) {
            addSessionCard(session)
        }
    }

    private fun showNoSessions() {
        binding.upcomingSessionsContainer.removeAllViews()
        binding.noSessionsText.visibility = View.VISIBLE
    }

    private fun addSessionCard(session: NetworkUtils.TutoringSession) {
        // Create a card for the session
        val cardView = layoutInflater.inflate(R.layout.item_session_card, null) as CardView

        // Set session details
        val titleText = cardView.findViewById<TextView>(R.id.sessionTitle)
        val dateText = cardView.findViewById<TextView>(R.id.sessionDate)
        val timeText = cardView.findViewById<TextView>(R.id.sessionTime)
        val statusText = cardView.findViewById<TextView>(R.id.sessionStatus)

        // Format date and time
        val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

        try {
            val startDateTime = dateTimeFormatter.parse(session.startTime)
            val endDateTime = dateTimeFormatter.parse(session.endTime)

            if (startDateTime != null && endDateTime != null) {
                val formattedDate = dateFormatter.format(startDateTime)
                val formattedStartTime = timeFormatter.format(startDateTime)
                val formattedEndTime = timeFormatter.format(endDateTime)

                titleText.text = session.subject
                dateText.text = formattedDate
                timeText.text = "$formattedStartTime - $formattedEndTime"
                statusText.text = session.status
            } else {
                // Fallback if parsing fails
                titleText.text = session.subject
                dateText.text = "Date not available"
                timeText.text = "Time not available"
                statusText.text = session.status
            }
        } catch (e: Exception) {
            // Fallback if parsing fails
            titleText.text = session.subject
            dateText.text = "Date not available"
            timeText.text = "Time not available"
            statusText.text = session.status
        }

        // Add margin to the card
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.marginEnd = resources.getDimensionPixelSize(R.dimen.session_card_margin)
        cardView.layoutParams = layoutParams

        // Add the card to the container
        binding.upcomingSessionsContainer.addView(cardView)
    }

    private fun loadSubjects(subjects: List<String>) {
        // Clear existing subjects
        binding.subjectsContainer.removeAllViews()

        if (subjects.isEmpty()) {
            binding.noSubjectsText.visibility = View.VISIBLE
            return
        }

        binding.noSubjectsText.visibility = View.GONE

        // Add subject chips
        for (subject in subjects) {
            addSubjectChip(subject)
        }
    }

    private fun showError(message: String) {
        // Show error message to user
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun addSubjectChip(subjectName: String) {
        // Create a TextView for the subject chip
        val chip = TextView(this)
        chip.text = subjectName
        chip.setBackgroundResource(R.drawable.category_background)
        chip.setPadding(
            resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
            resources.getDimensionPixelSize(R.dimen.activity_vertical_margin) / 2,
            resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
            resources.getDimensionPixelSize(R.dimen.activity_vertical_margin) / 2
        )
        chip.setTextColor(resources.getColor(android.R.color.black, theme))

        // Add margin to the chip
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.marginEnd = resources.getDimensionPixelSize(R.dimen.session_card_margin)
        chip.layoutParams = layoutParams

        // Add the chip to the container
        binding.subjectsContainer.addView(chip)
    }
}
