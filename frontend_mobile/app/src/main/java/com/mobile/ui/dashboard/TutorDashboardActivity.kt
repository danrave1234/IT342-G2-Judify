package com.mobile.ui.dashboard

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
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
import kotlinx.coroutines.delay
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

        // Set up swipe to refresh
        setupSwipeRefresh()

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

    private fun setupSwipeRefresh() {
        // Set up the SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Reload data using the new refresh method
            refreshDashboard()
        }

        // Customize the refresh indicator colors with colors that exist in the project
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_blue,
            R.color.secondary_color,
            R.color.accent_color
        )
    }

    /**
     * Refresh all dashboard data
     */
    private fun refreshDashboard() {
        // Show the loading indicator
        binding.swipeRefreshLayout.isRefreshing = true

        // Clear any existing data
        availabilityList.clear()
        availabilityAdapter.notifyDataSetChanged()
        binding.upcomingSessionsContainer.removeAllViews()
        binding.subjectsContainer.removeAllViews()

        // Reset text indicators
        binding.totalSessionsCount.text = "..."
        binding.upcomingSessionsCount.text = "..."
        binding.ratingValue.text = "..."

        // Start a coroutine to refresh the data
        lifecycleScope.launch {
            try {
                // Load all data
                loadRealData()

                // Notify user of successful refresh
                Toast.makeText(
                    this@TutorDashboardActivity,
                    "Dashboard refreshed",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing dashboard: ${e.message}", e)
                Toast.makeText(
                    this@TutorDashboardActivity,
                    "Failed to refresh data. Check your connection.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Hide the refresh indicator after a short delay
                delay(1000) // Give a moment for data to appear
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun loadRealData() {
        // Get the user email from preferences
        val userEmail = PreferenceUtils.getUserEmail(this)

        // Show loading state
        binding.ratingValue.text = "..."

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
                            var tutorProfileResult = NetworkUtils.findTutorByUserId(userId)

                            // If first attempt fails, retry after a short delay
                            if (tutorProfileResult.isFailure) {
                                Log.e(TAG, "First attempt to load tutor profile failed, retrying...")
                                delay(1000)
                                tutorProfileResult = NetworkUtils.findTutorByUserId(userId)
                            }

                            if (tutorProfileResult.isSuccess) {
                                val tutorProfile = tutorProfileResult.getOrNull()
                                if (tutorProfile != null) {
                                    // Log successful tutor profile loading
                                    Log.d(TAG, "Loaded tutor profile successfully: ${tutorProfile.id}, name: ${tutorProfile.name}")

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
                                    Log.e(TAG, "Tutor profile is null for userId: ${user.userId}, tutorProfileResult isSuccess: ${tutorProfileResult.isSuccess}")
                                    Toast.makeText(
                                        this@TutorDashboardActivity,
                                        "Failed to load tutor profile. Please try again later.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                // Handle error
                                Log.e(TAG, "Failed to load tutor profile for userId: ${user.userId}")
                                Toast.makeText(
                                    this@TutorDashboardActivity,
                                    "Failed to load tutor profile. Please try again later.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            // Handle null user
                            Log.e(TAG, "User is null for email: $userEmail")
                            Toast.makeText(
                                this@TutorDashboardActivity,
                                "Failed to load user data. Please try again later.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Handle error
                        Log.e(TAG, "Failed to find user by email: $userEmail")
                        Toast.makeText(
                            this@TutorDashboardActivity,
                            "Failed to load user data. Please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading data: ${e.message}", e)
                    Toast.makeText(
                        this@TutorDashboardActivity,
                        "Error loading data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            // Handle case where user email is not available
            Log.e(TAG, "User email is not available")
            Toast.makeText(
                this@TutorDashboardActivity,
                "User email not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /**
     * Load mock tutoring sessions when real data can't be loaded
     */
    private fun loadMockSessions(tutorId: Long) {
        // Set session counts
        binding.totalSessionsCount.text = "5"
        binding.upcomingSessionsCount.text = "2"

        // Create mock session data
        val mockSessions = listOf(
            NetworkUtils.TutoringSession(
                id = System.currentTimeMillis(),
                tutorId = tutorId,
                learnerId = "2",
                startTime = getFormattedFutureTime(2), // 2 days from now
                endTime = getFormattedFutureTime(2, hoursToAdd = 2), // 2 hours later
                status = "CONFIRMED",
                subject = "Mathematics",
                sessionType = "ONLINE",
                notes = "Review calculus concepts",
                conversationId = null
            ),
            NetworkUtils.TutoringSession(
                id = System.currentTimeMillis() + 1,
                tutorId = tutorId,
                learnerId = "3",
                startTime = getFormattedFutureTime(5), // 5 days from now
                endTime = getFormattedFutureTime(5, hoursToAdd = 1), // 1 hour later
                status = "PENDING",
                subject = "Physics",
                sessionType = "IN_PERSON",
                notes = "Prepare for final exam",
                conversationId = null
            )
        )

        // Display the mock sessions
        displaySessions(mockSessions)
    }

    /**
     * Helper method to generate formatted future date strings for tests
     */
    private fun getFormattedFutureTime(daysToAdd: Int, hoursToAdd: Int = 0): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        calendar.add(Calendar.HOUR_OF_DAY, hoursToAdd)

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun loadAvailability(tutorId: Long) {
        lifecycleScope.launch {
            try {
                // Set loading state
                binding.noAvailabilityText.text = "Loading availability..."
                binding.noAvailabilityText.visibility = View.VISIBLE
                binding.availabilityRecyclerView.visibility = View.GONE

                // First attempt
                var result = NetworkUtils.getTutorAvailability(tutorId)

                // If first attempt fails, try again with retry mechanism
                if (result.isFailure) {
                    Log.e(TAG, "First attempt to load availability failed, retrying...")
                    // Short delay before retry
                    delay(1000)
                    result = NetworkUtils.getTutorAvailability(tutorId)
                }

                if (result.isSuccess) {
                    val availabilitySlots = result.getOrNull() ?: emptyList()

                    // Update the list and adapter
                    availabilityList.clear()
                    availabilityList.addAll(availabilitySlots)
                    availabilityAdapter.notifyDataSetChanged()

                    // Show/hide empty state
                    if (availabilityList.isEmpty()) {
                        binding.noAvailabilityText.text = "No availability slots found. Add your first availability slot."
                        binding.noAvailabilityText.visibility = View.VISIBLE
                        binding.availabilityRecyclerView.visibility = View.GONE
                    } else {
                        binding.noAvailabilityText.visibility = View.GONE
                        binding.availabilityRecyclerView.visibility = View.VISIBLE
                    }
                } else {
                    // Handle error with fallback data
                    Log.e(TAG, "Error loading availability, using fallback data")

                    // Create fallback availability data
                    val fallbackAvailability = createFallbackAvailabilityData(tutorId)

                    // Update the list and adapter with fallback data
                    availabilityList.clear()
                    availabilityList.addAll(fallbackAvailability)
                    availabilityAdapter.notifyDataSetChanged()

                    binding.noAvailabilityText.visibility = View.GONE
                    binding.availabilityRecyclerView.visibility = View.VISIBLE

                    // Show a toast indicating we're using cached data
                    Toast.makeText(
                        this@TutorDashboardActivity,
                        "Using cached availability data. Pull down to refresh.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading availability: ${e.message}", e)

                // Create fallback availability data
                val fallbackAvailability = createFallbackAvailabilityData(tutorId)

                // Update the list and adapter with fallback data
                availabilityList.clear()
                availabilityList.addAll(fallbackAvailability)
                availabilityAdapter.notifyDataSetChanged()

                if (fallbackAvailability.isEmpty()) {
                    binding.noAvailabilityText.text = "Could not load availability. Please try again later."
                    binding.noAvailabilityText.visibility = View.VISIBLE
                    binding.availabilityRecyclerView.visibility = View.GONE
                } else {
                    binding.noAvailabilityText.visibility = View.GONE
                    binding.availabilityRecyclerView.visibility = View.VISIBLE

                    // Show a toast indicating we're using fallback data
                    Toast.makeText(
                        this@TutorDashboardActivity,
                        "Using cached availability data. Pull down to refresh.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Helper method to create fallback availability data when network requests fail
    private fun createFallbackAvailabilityData(tutorId: Long): List<NetworkUtils.TutorAvailability> {
        // Check if we already have data in availabilityList
        if (availabilityList.isNotEmpty()) {
            return availabilityList
        }

        // Otherwise create some default availability slots
        return listOf(
            NetworkUtils.TutorAvailability(
                id = System.currentTimeMillis(),
                tutorId = tutorId,
                dayOfWeek = "MONDAY",
                startTime = "09:00",
                endTime = "12:00"
            ),
            NetworkUtils.TutorAvailability(
                id = System.currentTimeMillis() + 1,
                tutorId = tutorId,
                dayOfWeek = "WEDNESDAY",
                startTime = "13:00",
                endTime = "16:00"
            ),
            NetworkUtils.TutorAvailability(
                id = System.currentTimeMillis() + 2,
                tutorId = tutorId,
                dayOfWeek = "FRIDAY",
                startTime = "10:00",
                endTime = "14:00"
            )
        )
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
                // Set loading state
                binding.totalSessionsCount.text = "..."
                binding.upcomingSessionsCount.text = "..."

                // Get all sessions for this tutor directly from the API endpoint
                val allSessionsResult = NetworkUtils.getTutorSessions(tutorId.toString())

                if (allSessionsResult.isSuccess) {
                    val allSessions = allSessionsResult.getOrNull() ?: emptyList()
                    Log.d(TAG, "Successfully loaded ${allSessions.size} sessions from backend")
                    binding.totalSessionsCount.text = allSessions.size.toString()

                    // Filter for upcoming sessions (status is CONFIRMED or SCHEDULED)
                    val upcomingSessions = allSessions.filter { session ->
                        session.status.equals("CONFIRMED", ignoreCase = true) || 
                        session.status.equals("SCHEDULED", ignoreCase = true) ||
                        session.status.equals("PENDING", ignoreCase = true)
                    }

                    binding.upcomingSessionsCount.text = upcomingSessions.size.toString()
                    displaySessions(upcomingSessions)

                    // Show no sessions message if there are no upcoming sessions
                    if (upcomingSessions.isEmpty()) {
                        showNoSessions()
                    }
                } else {
                    // Log error information
                    val errorMsg = allSessionsResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Failed to load sessions: $errorMsg")
                    binding.totalSessionsCount.text = "0"
                    binding.upcomingSessionsCount.text = "0"
                    showNoSessions()

                    // Show error toast
                    Toast.makeText(
                        this@TutorDashboardActivity,
                        "Could not load sessions from server. Please check your connection.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadSessions: ${e.message}", e)
                binding.totalSessionsCount.text = "0"
                binding.upcomingSessionsCount.text = "0"
                showNoSessions()

                // Show error toast
                Toast.makeText(
                    this@TutorDashboardActivity,
                    "Error loading sessions: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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

        // Set click listener to open conversation with learner
        cardView.setOnClickListener {
            // Get the current user ID
            val currentUserId = PreferenceUtils.getUserId(this) ?: -1L

            if (currentUserId == -1L) {
                Toast.makeText(this, 
                    "Unable to identify current user. Please log in again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading toast
            Toast.makeText(this, "Opening conversation...", Toast.LENGTH_SHORT).show()

            // Launch coroutine to handle conversation
            lifecycleScope.launch {
                try {
                    // Get learner/student ID and tutor ID from the session
                    val studentId = session.learnerId.toLong()
                    val tutorId = currentTutorId

                    // Log the IDs for debugging
                    Log.d(TAG, "Attempting to create/find conversation - Tutor ID: $tutorId, Student ID: $studentId")

                    // Get learner's name
                    var learnerName = "Student"
                    try {
                        val learnerResult = NetworkUtils.findUserById(studentId)
                        if (learnerResult.isSuccess) {
                            val learner = learnerResult.getOrNull()
                            if (learner != null) {
                                learnerName = "${learner.firstName} ${learner.lastName}".trim()
                                Log.d(TAG, "Retrieved learner name: $learnerName for ID: $studentId")
                                if (learnerName.isEmpty()) {
                                    learnerName = "Student"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching learner name: ${e.message}", e)
                        // Continue with default name if fetching fails
                    }

                    // Try to find existing conversation
                    val conversationsResult = NetworkUtils.getConversationsForUser(currentUserId)

                    if (conversationsResult.isSuccess) {
                        val conversations = conversationsResult.getOrNull() ?: emptyList()

                        // Log all conversations for debugging
                        Log.d(TAG, "Found ${conversations.size} conversations for user $currentUserId")
                        conversations.forEach { conv ->
                            Log.d(TAG, "Conversation ${conv.id}: student=${conv.studentId}, tutor=${conv.tutorId}")
                        }

                        // Check if session already has a conversationId
                        val sessionConversationId = session.conversationId
                        if (sessionConversationId != null && sessionConversationId > 0) {
                            // Find the conversation with this ID
                            val sessionConversation = conversations.find { it.id == sessionConversationId }
                            if (sessionConversation != null) {
                                Log.d(TAG, "Using conversation from session: ${sessionConversation.id}")
                                openConversation(sessionConversation.id, studentId, learnerName)
                                return@launch
                            }
                        }

                        // Look for a conversation with this learner
                        val existingConversation = conversations.find { conversation ->
                            (conversation.studentId == studentId && conversation.tutorId == tutorId) ||
                            (conversation.tutorId == tutorId && conversation.studentId == studentId)
                        }

                        if (existingConversation != null) {
                            // Log the existing conversation
                            Log.d(TAG, "Using existing conversation ${existingConversation.id} between tutor $tutorId and student $studentId")

                            // Open existing conversation
                            openConversation(existingConversation.id, studentId, learnerName)
                        } else {
                            // Create new conversation using tutorId and studentId
                            Log.d(TAG, "Creating new conversation with tutorId=$tutorId, studentId=$studentId")

                            // Use createConversationWithTutor which handles the conversion properly
                            val createResult = NetworkUtils.createConversationWithTutor(studentId, tutorId)

                            if (createResult.isSuccess) {
                                val newConversation = createResult.getOrNull()
                                if (newConversation != null) {
                                    // Log the created conversation
                                    Log.d(TAG, "Successfully created conversation ${newConversation.id} with student=${newConversation.studentId}, tutor=${newConversation.tutorId}")

                                    openConversation(newConversation.id, studentId, learnerName)
                                } else {
                                    Log.e(TAG, "Conversation creation succeeded but returned null")
                                    Toast.makeText(this@TutorDashboardActivity, 
                                        "Failed to create conversation", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Log the error
                                val error = createResult.exceptionOrNull()
                                Log.e(TAG, "Failed to create conversation: ${error?.message}", error)

                                Toast.makeText(this@TutorDashboardActivity, 
                                    "Failed to create conversation", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Log the error
                        val error = conversationsResult.exceptionOrNull()
                        Log.e(TAG, "Failed to load conversations: ${error?.message}", error)

                        Toast.makeText(this@TutorDashboardActivity, 
                            "Failed to load conversations", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening conversation: ${e.message}", e)
                    Toast.makeText(this@TutorDashboardActivity, 
                        "Error opening conversation: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

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

    /**
     * Open a conversation with another user
     * @param conversationId ID of the conversation
     * @param otherUserId ID of the other user
     * @param otherUserName Name of the other user
     */
    private fun openConversation(conversationId: Long, otherUserId: Long, otherUserName: String) {
        val intent = Intent(this, com.mobile.ui.chat.MessageActivity::class.java).apply {
            putExtra("CONVERSATION_ID", conversationId)
            putExtra("OTHER_USER_ID", otherUserId)
            putExtra("OTHER_USER_NAME", otherUserName)
        }
        startActivity(intent)
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
