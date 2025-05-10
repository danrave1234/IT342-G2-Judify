package com.mobile.ui.dashboard

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
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
import com.mobile.ui.notifications.NotificationsFragment
import com.mobile.ui.profile.ProfileFragment
import com.mobile.ui.sessions.SessionsFragment
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import com.mobile.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TutorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorDashboardBinding
    private val TAG = "TutorDashboard"

    private lateinit var availabilityAdapter: TutorAvailabilityAdapter
    private val availabilityList = mutableListOf<NetworkUtils.TutorAvailability>()
    private var currentTutorId: Long = 0
    private var currentTutorHourlyRate: Double = 0.0
    private var currentTutorUserId: Long? = null

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

        // Handle navigation extras (for deep linking)
        handleNavigationExtras()

        // Set up availability RecyclerView
        setupAvailabilityRecyclerView()

        // Set up swipe to refresh
        setupSwipeRefresh()

        // Prevent search edit text auto-focusing
        setupSearchBar()

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

            // Get the 24-hour format times from tags before dismissing the dialog
            val startTime24 = startTimeEditText.tag as? String ?: startTime
            val endTime24 = endTimeEditText.tag as? String ?: endTime

            if (startTime.isEmpty() || endTime.isEmpty()) {
                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Please select both start and end times")
                return@setOnClickListener
            }

            // Dismiss the dialog
            dialog.dismiss()

            // Create the availability slot with the captured time values
            createAvailabilitySlot(dayOfWeek, startTime24, endTime24)
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

            // Get the 24-hour format times from tags before dismissing the dialog
            val startTime24 = startTimeEditText.tag as? String ?: startTime
            val endTime24 = endTimeEditText.tag as? String ?: endTime

            if (startTime.isEmpty() || endTime.isEmpty()) {
                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Please select both start and end times")
                return@setOnClickListener
            }

            // Dismiss the dialog
            dialog.dismiss()

            // Update the availability slot with the captured time values
            updateAvailabilitySlot(availability.id, dayOfWeek, startTime24, endTime24)
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
                // Format the time in 12-hour format for display (h:mm a)
                val displayFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val backendFormat = SimpleDateFormat("HH:mm", Locale.getDefault()) // 24-hour format for backend

                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)

                // Display the time in 12-hour format with AM/PM
                val displayTime = displayFormat.format(calendar.time)
                editText.setText(displayTime)

                // Store the 24-hour format as a tag for use when saving
                val backendTime = backendFormat.format(calendar.time)
                editText.tag = backendTime
            },
            hour,
            minute,
            false // Use 12-hour format in the picker dialog
        ).show()
    }

    // Helper function to get the 24-hour format time from an EditText
    private fun getTime24Hour(editText: EditText): String {
        // Check if we have a 24-hour format time stored as a tag
        if (editText.tag != null && editText.tag is String) {
            return editText.tag as String
        }

        // Otherwise, try to parse the visible text
        val timeText = editText.text.toString()

        try {
            // Parse the 12-hour format time
            val inputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val date = inputFormat.parse(timeText)
            if (date != null) {
                return outputFormat.format(date)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: ${e.message}", e)
        }

        // Return the original text if parsing fails
        return timeText
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
                    // Show the sessions/booking fragment instead of map
                    showMainContent(false)
                    loadFragment(SessionsFragment())
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
            showManageSubjectsDialog()
        }

        // Set up notification icon click
        binding.notificationIcon.setOnClickListener {
            // Show the notifications fragment
            showMainContent(false)
            loadFragment(NotificationsFragment())
            // Don't update the bottom navigation selection since notifications isn't in the bottom nav
        }

        // Set up message icon click
        binding.messageIcon.setOnClickListener {
            // Show the chat fragment
            showMainContent(false)
            loadFragment(ChatFragment())
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
            bottomNavigation.selectedItemId = R.id.navigation_chat
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

    /**
     * Prevents search edit text elements from auto-focusing when navigating between screens
     */
    private fun setupSearchBar() {
        // Find all search edit text views in the activity
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        disableSearchEditTextAutoFocus(rootView)
    }

    /**
     * Recursively finds and configures all EditText views with "search" in their ID
     * to prevent auto-focus and showing keyboard
     */
    private fun disableSearchEditTextAutoFocus(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            // Check if this is an EditText with "search" in its ID
            if (child is EditText && child.id != View.NO_ID) {
                val idString = resources.getResourceEntryName(child.id)
                if (idString.contains("search", ignoreCase = true)) {
                    // Disable focus and make not focusable in touch mode
                    child.isFocusable = false
                    child.isFocusableInTouchMode = false
                    // Enable focusing only when explicitly clicked
                    child.setOnClickListener {
                        child.isFocusableInTouchMode = true
                        child.isFocusable = true
                        child.requestFocus()
                    }
                }
            }

            // Recursively check child view groups
            if (child is ViewGroup) {
                disableSearchEditTextAutoFocus(child)
            }
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
     * Handle navigation extras from intent
     * This allows deep linking to specific fragments
     */
    private fun handleNavigationExtras() {
        // Check if we have any navigation extras
        val fragmentToShow = intent.getStringExtra("FRAGMENT")
        if (fragmentToShow != null) {
            when (fragmentToShow) {
                "MAP" -> {
                    showMainContent(false)
                    loadFragment(MapFragment())
                    findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.navigation_map
                }
                "CHAT" -> {
                    showMainContent(false)
                    loadFragment(ChatFragment())
                    findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.navigation_chat
                }
                "PROFILE" -> {
                    showMainContent(false)
                    loadFragment(ProfileFragment())
                    findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.navigation_profile
                }
                "NOTIFICATIONS" -> {
                    showMainContent(false)
                    loadFragment(NotificationsFragment())
                }
                "SESSIONS" -> {
                    showMainContent(false)
                    loadFragment(SessionsFragment())
                    findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.navigation_sessions
                }
            }
        }

        // Check if we have a specific session ID to display
        val sessionId = intent.getLongExtra("SESSION_ID", -1L)
        if (sessionId != -1L) {
            // Log that we received a session ID
            Log.d(TAG, "Received request to view session with ID: $sessionId")

            // Here we would ideally navigate to a session detail view
            // For now, we just show the sessions list
            showMainContent(false)
            loadFragment(SessionsFragment())
            findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.navigation_sessions

            // Show a toast to indicate we received the session ID
            UiUtils.showSnackbar(findViewById(android.R.id.content), "Viewing sessions (Session ID: $sessionId)")
        }
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
                UiUtils.showSuccessSnackbar(findViewById(android.R.id.content), "Dashboard refreshed")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing dashboard: ${e.message}", e)
                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to refresh data. Check your connection.")
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

                                    // Store tutor profileId for later use
                                    currentTutorId = tutorProfile.id
                                    
                                    // Store tutor userId for APIs that require it
                                    currentTutorUserId = tutorProfile.userId
                                    
                                    // Store hourly rate for session cards
                                    currentTutorHourlyRate = tutorProfile.hourlyRate

                                    // Load subjects
                                    loadSubjects(tutorProfile.subjects)

                                    // Get tutoring sessions - use userId instead of profileId for sessions
                                    tutorProfile.userId?.let { userId ->
                                        loadSessions(userId)  // Use userId instead of tutor profile ID
                                    } ?: run {
                                        // Fallback if userId is null (shouldn't happen based on our fix)
                                        Log.e(TAG, "tutorProfile.userId is null, falling back to profileId for session loading")
                                        loadSessions(tutorProfile.id)
                                    }

                                    // Load availability - use tutorId (profile ID) for availability APIs
                                    loadAvailability(tutorProfile.id)  // Use tutorId (profile ID) for availability
                                } else {
                                    // Handle null tutor profile
                                    Log.e(TAG, "Tutor profile is null for userId: ${user.userId}, tutorProfileResult isSuccess: ${tutorProfileResult.isSuccess}")
                                    UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to load tutor profile. Please try again later.")
                                }
                            } else {
                                // Handle error
                                Log.e(TAG, "Failed to load tutor profile for userId: ${user.userId}")
                                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to load tutor profile. Please try again later.")
                            }
                        } else {
                            // Handle null user
                            Log.e(TAG, "User is null for email: $userEmail")
                            UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to load user data. Please try again later.")
                        }
                    } else {
                        // Handle error
                        Log.e(TAG, "Failed to find user by email: $userEmail")
                        UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to load user data. Please try again later.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading data: ${e.message}", e)
                    UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Error loading data: ${e.message}")
                }
            }
        } else {
            // Handle case where user email is not available
            Log.e(TAG, "User email is not available")
            UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "User email not found. Please log in again.")
        }
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
                Log.d(TAG, "Fetching availability using tutorId (profile ID): $tutorId")
                var result = NetworkUtils.getTutorAvailability(tutorId)

                // If first attempt fails, try again with retry mechanism
                if (result.isFailure) {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "First attempt to load availability failed: $errorMsg, retrying...")
                    
                    // Short delay before retry
                    delay(1000)
                    result = NetworkUtils.getTutorAvailability(tutorId)
                }

                if (result.isSuccess) {
                    val availabilitySlots = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Successfully loaded ${availabilitySlots.size} availability slots. " +
                              "First slot tutorId value (should be profile ID): ${if (availabilitySlots.isNotEmpty()) availabilitySlots[0].tutorId else "N/A"}")

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
                    // Handle error without using fallback data
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Error loading availability: $errorMsg")

                    // Clear any existing data
                    availabilityList.clear()
                    availabilityAdapter.notifyDataSetChanged()

                    // Show error message
                    binding.noAvailabilityText.text = "Could not load availability. Please try again later."
                    binding.noAvailabilityText.visibility = View.VISIBLE
                    binding.availabilityRecyclerView.visibility = View.GONE

                    // Show a toast indicating the error
                    UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Unable to load availability: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading availability: ${e.message}", e)

                // Clear any existing data
                availabilityList.clear()
                availabilityAdapter.notifyDataSetChanged()

                // Show error message
                binding.noAvailabilityText.text = "Could not load availability. Please try again later."
                binding.noAvailabilityText.visibility = View.VISIBLE
                binding.availabilityRecyclerView.visibility = View.GONE

                // Show a toast indicating the error
                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Unable to load availability: ${e.message}")
            }
        }
    }

    // Helper method to create fallback availability data when network requests fail
    private fun createFallbackAvailabilityData(tutorId: Long): List<NetworkUtils.TutorAvailability> {
        // Don't create fallback data anymore - just return an empty list
        return emptyList()
    }

    private fun createAvailabilitySlot(dayOfWeek: String, startTime: String, endTime: String) {
        lifecycleScope.launch {
            try {
                // Use the 24-hour time values directly passed to this method
                Log.d(TAG, "Creating availability with 24-hour times: start=$startTime, end=$endTime")
                
                // Use the tutor profile ID (not userId) for availability APIs
                Log.d(TAG, "Using tutorId (profile ID: $currentTutorId) for creating availability - this is correct")
                val result = NetworkUtils.createTutorAvailability(
                    tutorId = currentTutorId,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime
                )

                if (result.isSuccess) {
                    // Reload availability data using tutorId
                    Log.d(TAG, "Successfully created availability slot, reloading data with tutorId: $currentTutorId")
                    loadAvailability(currentTutorId)
                    UiUtils.showSuccessSnackbar(findViewById(android.R.id.content), "Availability added successfully")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Failed to create availability slot: $errorMsg")
                    showError("Error creating availability slot: $errorMsg")
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
                // Use the 24-hour time values directly passed to this method
                Log.d(TAG, "Updating availability with 24-hour times: start=$startTime, end=$endTime")
                
                // Use the tutor profile ID (not userId) for availability APIs
                Log.d(TAG, "Using tutorId (profile ID: $currentTutorId) for updating availability - this is correct")
                val result = NetworkUtils.updateTutorAvailability(
                    id = id,
                    tutorId = currentTutorId,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime
                )

                if (result.isSuccess) {
                    // Reload availability data using tutorId
                    Log.d(TAG, "Successfully updated availability slot, reloading data with tutorId: $currentTutorId")
                    loadAvailability(currentTutorId)
                    UiUtils.showSuccessSnackbar(findViewById(android.R.id.content), "Availability updated successfully")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Failed to update availability slot: $errorMsg")
                    showError("Error updating availability slot: $errorMsg")
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
                    // Reload availability data using tutorId (profile ID)
                    Log.d(TAG, "Reloading availability with tutorId (profile ID): $currentTutorId after deletion")
                    loadAvailability(currentTutorId)
                    UiUtils.showSuccessSnackbar(findViewById(android.R.id.content), "Availability deleted successfully")
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
                // Note: This method should be called with the USER ID, not the profile ID
                // The backend expects a user ID for /api/tutoring-sessions/findByUser endpoint
                Log.d(TAG, "Fetching sessions using userId: $tutorId")
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
                    UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Could not load sessions from server. Please check your connection.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadSessions: ${e.message}", e)
                binding.totalSessionsCount.text = "0"
                binding.upcomingSessionsCount.text = "0"
                showNoSessions()

                // Show error toast
                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Error loading sessions: ${e.message}")
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

    /**
     * Converts a time string in 24-hour format (HH:mm:ss) to 12-hour format (h:mm a)
     */
    private fun formatTime(timeString: String): String {
        try {
            // Extract hour and minute from the time string (format: HH:mm:ss or HH:mm)
            val timeParts = timeString.split(":")
            if (timeParts.size >= 2) {
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                // Convert to 12-hour format
                val hour12 = when {
                    hour == 0 -> 12 // 00:00 becomes 12:00 AM
                    hour > 12 -> hour - 12 // 13:00 becomes 1:00 PM
                    else -> hour // 10:00 stays as 10:00 AM
                }

                // Determine AM/PM
                val amPm = if (hour < 12) "AM" else "PM"

                // Format the time
                return String.format("%d:%02d %s", hour12, minute, amPm)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting time: $timeString", e)
        }

        // Return original if parsing fails
        return timeString
    }

    private fun addSessionCard(session: NetworkUtils.TutoringSession) {
        // Create a card for the session
        val cardView = layoutInflater.inflate(R.layout.item_session_card, null) as CardView

        // Set session details
        val titleText = cardView.findViewById<TextView>(R.id.sessionTitle)
        val dateText = cardView.findViewById<TextView>(R.id.sessionDate)
        val timeText = cardView.findViewById<TextView>(R.id.sessionTime)
        val statusText = cardView.findViewById<TextView>(R.id.sessionStatus)
        val studentNameText = cardView.findViewById<TextView>(R.id.sessionStudentName)
        val sessionTypeText = cardView.findViewById<TextView>(R.id.sessionType)
        
        // Get the session type container to ensure it's visible
        val sessionTypeContainer = cardView.findViewById<LinearLayout>(R.id.sessionTypeContainer)
        sessionTypeContainer.visibility = View.VISIBLE

        // Get action buttons (they might be GONE by default in the layout)
        val approveButton = cardView.findViewById<ImageButton>(R.id.approveButton)
        val rejectButton = cardView.findViewById<ImageButton>(R.id.rejectButton)

        // Add extensive logging for session type debugging - expanded version
        Log.d(TAG, "=====================================================")
        Log.d(TAG, "Session ${session.id} DISPLAY PROCESS:")
        Log.d(TAG, "Raw data from NetworkUtils - sessionType: '${session.sessionType}'")
        Log.d(TAG, "SessionType data type: ${session.sessionType?.javaClass?.name}")
        Log.d(TAG, "=====================================================")
        
        try {
            // Extract date and time parts from the session time strings
            val startTimeParts = if (session.startTime.contains("T")) {
                // ISO format (2025-05-05T17:00:00)
                val parts = session.startTime.split("T")
                if (parts.size == 2) {
                    Pair(parts[0], parts[1]) // date, time
                } else {
                    Pair("", "")
                }
            } else {
                // SQL format (2025-05-05 17:00:00)
                val parts = session.startTime.split(" ")
                if (parts.size == 2) {
                    Pair(parts[0], parts[1]) // date, time
                } else {
                    Pair("", "")
                }
            }

            val endTimeParts = if (session.endTime.contains("T")) {
                val parts = session.endTime.split("T")
                if (parts.size == 2) {
                    Pair(parts[0], parts[1]) // date, time
                } else {
                    Pair("", "")
                }
            } else {
                val parts = session.endTime.split(" ")
                if (parts.size == 2) {
                    Pair(parts[0], parts[1]) // date, time
                } else {
                    Pair("", "")
                }
            }

            // Format the date
            val dateStr = startTimeParts.first
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayDateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

            val date = dateFormatter.parse(dateStr)
            val formattedDate = if (date != null) {
                displayDateFormatter.format(date)
            } else {
                "Date not available"
            }

            // Format the time using our simple formatter
            val startTimeStr = startTimeParts.second
            val endTimeStr = endTimeParts.second

            // Get just the HH:mm part (remove seconds if present)
            val startTimeHHMM = startTimeStr.split(".")[0].substring(0, Math.min(5, startTimeStr.length))
            val endTimeHHMM = endTimeStr.split(".")[0].substring(0, Math.min(5, endTimeStr.length))

            // Convert to 12-hour format
            val formattedStartTime = formatTime(startTimeHHMM)
            val formattedEndTime = formatTime(endTimeHHMM)

            // Log time information for debugging
            Log.d(TAG, "Session time - Original: '${session.startTime}' to '${session.endTime}'")
            Log.d(TAG, "Session time - Extracted: '$startTimeHHMM' to '$endTimeHHMM'")
            Log.d(TAG, "Session time - Formatted: '$formattedStartTime' to '$formattedEndTime'")

            titleText.text = session.subject
            dateText.text = formattedDate
            timeText.text = "$formattedStartTime - $formattedEndTime"
            statusText.text = session.status

            // Display the student name and session type separately
            if (session.studentName.isNotEmpty()) {
                studentNameText.text = session.studentName
            } else {
                studentNameText.visibility = View.GONE
            }
            
            // CRITICAL FIX: Force-set the session type exactly as received from backend
            // Do not transform it in any way - additionally use uppercase to standardize display
            val originalSessionType = session.sessionType
            
            // Ensure the session type text is visible and set it directly from the source
            sessionTypeText.text = originalSessionType
            
            // Debug log before and after setting the text
            Log.d(TAG, "Setting session type: Original='${originalSessionType}', Final view text='${sessionTypeText.text}'")

            // Show approve/reject buttons for pending or requested sessions
            val sessionStatus = session.status.lowercase(Locale.getDefault())
            if (sessionStatus == "pending" || sessionStatus == "requested") {
                approveButton.visibility = View.VISIBLE
                rejectButton.visibility = View.VISIBLE

                // Set click listeners for approve and reject buttons
                approveButton.setOnClickListener {
                    updateSessionStatus(session.id, "APPROVED")
                }

                rejectButton.setOnClickListener {
                    updateSessionStatus(session.id, "REJECTED")
                }
            } else {
                approveButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
            }
        } catch (e: Exception) {
            // Log the error for debugging
            Log.e(TAG, "Error parsing session time: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()

            // Fallback if parsing fails
            titleText.text = session.subject
            dateText.text = "Date not available"
            timeText.text = "Time not available"
            statusText.text = session.status

            // Display the student name and session type separately even in fallback case
            if (session.studentName.isNotEmpty()) {
                studentNameText.text = session.studentName
            } else {
                studentNameText.visibility = View.GONE
            }
            
            // CRITICAL FIX in error case too
            val originalSessionType = session.sessionType
            sessionTypeText.text = originalSessionType
            Log.d(TAG, "Setting session type (fallback): Original='${originalSessionType}', Final view text='${sessionTypeText.text}'")
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
                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Unable to identify current user. Please log in again.")
                return@setOnClickListener
            }

            // Show loading toast
            UiUtils.showSnackbar(findViewById(android.R.id.content), "Opening conversation...")

            // Launch coroutine to handle conversation
            lifecycleScope.launch {
                try {
                    // Get student ID and tutor ID from the session
                    val studentId = session.studentId.toLong()
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
                            Log.d(TAG, "Creating new conversation with tutorId=$tutorId, studentId=$studentId, sessionId=${session.id}")

                            // First, get the userId associated with the tutorId
                            val tutorUserIdResult = NetworkUtils.getUserIdFromTutorId(tutorId)
                            if (tutorUserIdResult.isFailure) {
                                Log.e(TAG, "Failed to get userId from tutorId: ${tutorUserIdResult.exceptionOrNull()?.message}")
                                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Could not get tutor's user ID")
                                return@launch
                            }
                            
                            val tutorUserId = tutorUserIdResult.getOrNull()
                            if (tutorUserId == null) {
                                Log.e(TAG, "Failed to get userId from tutorId: No user ID returned")
                                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Could not find tutor's user account")
                                return@launch
                            }
                            
                            Log.d(TAG, "Converting tutorId=$tutorId to userId=$tutorUserId")

                            // Use createConversationWithTutor with the converted userId
                            // Pass the session ID to associate the conversation with the session
                            val createResult = NetworkUtils.createConversationWithTutor(studentId, tutorUserId, session.id)

                            if (createResult.isSuccess) {
                                val newConversation = createResult.getOrNull()
                                if (newConversation != null) {
                                    // Log the created conversation
                                    Log.d(TAG, "Successfully created conversation ${newConversation.id} with student=${newConversation.studentId}, tutor=${newConversation.tutorId}")

                                    openConversation(newConversation.id, studentId, learnerName)
                                } else {
                                    Log.e(TAG, "Conversation creation succeeded but returned null")
                                    UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to create conversation")
                                }
                            } else {
                                // Log the error
                                val error = createResult.exceptionOrNull()
                                Log.e(TAG, "Failed to create conversation: ${error?.message}", error)

                                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to create conversation")
                            }
                        }
                    } else {
                        // Log the error
                        val error = conversationsResult.exceptionOrNull()
                        Log.e(TAG, "Failed to load conversations: ${error?.message}", error)

                        UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to load conversations")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening conversation: ${e.message}", e)
                    UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Error opening conversation: ${e.message}")
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
        UiUtils.showErrorSnackbar(findViewById(android.R.id.content), message)
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

    /**
     * Shows a dialog for managing tutor subjects
     */
    /**
     * Update the status of a tutoring session
     * @param sessionId The ID of the session to update
     * @param newStatus The new status for the session (e.g., "APPROVED", "REJECTED", "COMPLETED")
     */
    private fun updateSessionStatus(sessionId: Long, newStatus: String) {
        // Show loading indicator
        UiUtils.showSnackbar(findViewById(android.R.id.content), "Updating session status...")

        lifecycleScope.launch {
            try {
                val result = NetworkUtils.updateSessionStatus(sessionId, newStatus)

                if (result.isSuccess) {
                    val updatedSession = result.getOrThrow()
                    withContext(Dispatchers.Main) {
                        UiUtils.showSuccessSnackbar(findViewById(android.R.id.content), "Session ${updatedSession.id} status updated to ${updatedSession.status}")

                        // Refresh the dashboard to show the updated session
                        refreshDashboard()
                    }
                } else {
                    val error = result.exceptionOrNull()
                    withContext(Dispatchers.Main) {
                        UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to update session status: ${error?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Error updating session status: ${e.message}")
                }
            }
        }
    }

    private fun showManageSubjectsDialog() {
        // Create dialog
        val dialog = AlertDialog.Builder(this).create()
        val dialogView = layoutInflater.inflate(R.layout.dialog_manage_subjects, null)
        dialog.setView(dialogView)

        // Get references to views
        val currentSubjectsContainer = dialogView.findViewById<LinearLayout>(R.id.currentSubjectsContainer)
        val noSubjectsTextView = dialogView.findViewById<TextView>(R.id.noSubjectsTextView)
        val subjectEditText = dialogView.findViewById<EditText>(R.id.subjectEditText)
        val addSubjectButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.addSubjectButton)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)
        val saveButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveButton)

        // Get current subjects
        val currentSubjects = mutableListOf<String>()

        // If we have a tutor profile, get the subjects
        if (currentTutorId > 0) {
            // Add all subjects from the tutor profile
            for (i in 0 until binding.subjectsContainer.childCount) {
                val view = binding.subjectsContainer.getChildAt(i)
                if (view is TextView && view.id != R.id.noSubjectsText) {
                    currentSubjects.add(view.text.toString())
                }
            }
        }

        // Function to update the subjects list in the dialog
        fun updateSubjectsList() {
            // Clear the container
            currentSubjectsContainer.removeAllViews()

            if (currentSubjects.isEmpty()) {
                noSubjectsTextView.visibility = View.VISIBLE
            } else {
                noSubjectsTextView.visibility = View.GONE

                // Add each subject to the container
                for (subject in currentSubjects) {
                    val subjectView = layoutInflater.inflate(R.layout.item_subject, null)
                    val subjectNameTextView = subjectView.findViewById<TextView>(R.id.subjectNameTextView)
                    val removeSubjectButton = subjectView.findViewById<ImageButton>(R.id.removeSubjectButton)

                    subjectNameTextView.text = subject

                    // Set up remove button
                    removeSubjectButton.setOnClickListener {
                        currentSubjects.remove(subject)
                        updateSubjectsList()
                    }

                    currentSubjectsContainer.addView(subjectView)
                }
            }
        }

        // Initial update of subjects list
        updateSubjectsList()

        // Set up add button
        addSubjectButton.setOnClickListener {
            val newSubject = subjectEditText.text.toString().trim()
            if (newSubject.isNotEmpty() && !currentSubjects.contains(newSubject)) {
                currentSubjects.add(newSubject)
                updateSubjectsList()
                subjectEditText.text.clear()
            } else if (currentSubjects.contains(newSubject)) {
                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Subject already added")
            }
        }

        // Set up cancel button
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Set up save button
        saveButton.setOnClickListener {
            if (currentTutorId > 0) {
                // Save the subjects
                lifecycleScope.launch {
                    try {
                        // First delete all existing subjects
                        val deleteResult = NetworkUtils.deleteAllSubjectsForTutor(currentTutorId)

                        if (deleteResult.isSuccess) {
                            // Then add the new subjects
                            val addResult = NetworkUtils.addSubjectsForTutor(currentTutorId, currentSubjects)

                            if (addResult.isSuccess) {
                                // Update the UI
                                loadSubjects(currentSubjects)
                                UiUtils.showSuccessSnackbar(findViewById(android.R.id.content), "Subjects updated successfully")
                            } else {
                                val error = addResult.exceptionOrNull()
                                Log.e(TAG, "Error adding subjects: ${error?.message}", error)
                                UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to update subjects")
                            }
                        } else {
                            val error = deleteResult.exceptionOrNull()
                            Log.e(TAG, "Error deleting subjects: ${error?.message}", error)
                            UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Failed to update subjects")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception updating subjects: ${e.message}", e)
                        UiUtils.showErrorSnackbar(findViewById(android.R.id.content), "Error: ${e.message}")
                    }
                }
            }

            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }
}
