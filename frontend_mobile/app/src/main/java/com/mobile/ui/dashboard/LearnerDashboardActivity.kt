package com.mobile.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mobile.R
import com.mobile.databinding.ActivityLearnerDashboardBinding
import com.mobile.ui.chat.ChatFragment
import com.mobile.ui.map.MapFragment
import com.mobile.ui.profile.ProfileFragment
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import com.mobile.ui.booking.BookingActivity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.RatingBar
import com.mobile.ui.search.TutorSearchActivity
import com.mobile.utils.NetworkUtils.TutorProfile
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import android.widget.EditText

class LearnerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnerDashboardBinding
    private val TAG = "LearnerDashboard"

    // Session adapter for the RecyclerView
    private val sessionAdapter = SessionAdapter(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLearnerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up greeting based on time of day
        setupGreeting()

        // Set up user name from preferences
        setupUserName()

        // Set up click listeners
        setupClickListeners()

        // Set up app bar behavior
        setupAppBarBehavior()

        // Set up sessions recycler view
        setupSessionsRecyclerView()

        // Load top tutors
        loadTopTutors()

        // Load learner sessions
        loadLearnerSessions()

        // Set up bottom navigation
        setupBottomNavigation()

        // Handle navigation from other activities
        handleNavigationExtras()

        // Setup search bar
        setupSearchBar()

        // Load real data
        loadRealData()

        // Check if we need to show sessions section (from booking completion)
        if (intent.getBooleanExtra("SHOW_SESSIONS", false)) {
            Log.d(TAG, "SHOW_SESSIONS flag detected, scrolling to sessions section")

            // Give the UI time to render before scrolling
            binding.dashboardScrollView.post {
                try {
                    val sessionsHeader = findViewWithText(binding.root, "All Sessions")
                    if (sessionsHeader != null) {
                        val yPosition = sessionsHeader.y.toInt()
                        Log.d(TAG, "Found sessions header at position $yPosition, scrolling")
                        binding.dashboardScrollView.smoothScrollTo(0, yPosition - 50) // Offset to see the header
                    } else {
                        // Fallback to showing the RecyclerView
                        binding.allSessionsRecyclerView.y.let { yPosition ->
                            Log.d(TAG, "Could not find header, using RecyclerView at position $yPosition")
                            binding.dashboardScrollView.smoothScrollTo(0, yPosition.toInt() - 100)
                        }
                    }

                    // Show a toast to confirm successful booking
                    Toast.makeText(this, "Session booked successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error scrolling to sessions: ${e.message}", e)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")

        // Refresh data when returning to the dashboard, but only if we're showing main content
        if (binding.dashboardScrollView.visibility == View.VISIBLE) {
            // We don't need to refresh every time, only if there might be changes
            val refreshNeeded = PreferenceUtils.getBoolean(this, "needs_session_refresh", false)
            if (refreshNeeded) {
                Log.d(TAG, "Session refresh needed, reloading data")
                // Reset the flag
                PreferenceUtils.saveBoolean(this, "needs_session_refresh", false)
                // Reload data
                loadRealData()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == BookingActivity.REQUEST_BOOKING) {
            Log.d(TAG, "Returned from BookingActivity")

            if (resultCode == RESULT_OK || resultCode == BookingActivity.RESULT_VIEW_SESSIONS) {
                Log.d(TAG, "Booking was successful, setting refresh flag and reloading sessions")

                // Set flag that we need to refresh sessions
                PreferenceUtils.saveBoolean(this, "needs_session_refresh", true)

                // Refresh sessions list when returning from booking
                loadLearnerSessions()

                // If the result is to view sessions, update UI to focus on sessions
                if (resultCode == BookingActivity.RESULT_VIEW_SESSIONS) {
                    Log.d(TAG, "RESULT_VIEW_SESSIONS received, scrolling to sessions section")

                    // Scroll to sessions section - find the view by looking at TextView with "All Sessions" text
                    binding.dashboardScrollView.post {
                        try {
                            val sessionsHeader = findViewWithText(binding.root, "All Sessions")
                            if (sessionsHeader != null) {
                                val yPosition = sessionsHeader.y.toInt()
                                Log.d(TAG, "Found sessions header at position $yPosition, scrolling")
                                binding.dashboardScrollView.smoothScrollTo(0, yPosition - 50) // Offset to see the header
                            } else {
                                // Fallback to showing the RecyclerView
                                binding.allSessionsRecyclerView.y.let { yPosition ->
                                    Log.d(TAG, "Could not find header, using RecyclerView at position $yPosition")
                                    binding.dashboardScrollView.smoothScrollTo(0, yPosition.toInt() - 100)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error scrolling to sessions: ${e.message}", e)
                        }
                    }

                    // Show a success message
                    Toast.makeText(this, "Session booked successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Standard success result received, showing toast")
                    Toast.makeText(this, "Session booked successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d(TAG, "Booking was canceled or unsuccessful, resultCode=$resultCode")
            }
        }
    }

    // Load tutors from the network
    private fun loadTopTutors() {
        Log.d(TAG, "Starting to load tutors")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Fetch random tutors
                Log.d(TAG, "About to call getRandomTutors")
                val result = NetworkUtils.getRandomTutors()

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val tutors = result.getOrNull() ?: emptyList()
                        Log.d(TAG, "Successfully fetched ${tutors.size} tutors")

                        if (tutors.isNotEmpty()) {
                            // If we have tutors, display them
                            displayTopTutors(tutors)
                        } else {
                            Log.w(TAG, "No tutors returned from API, showing placeholders")
                            // If no tutors found, show placeholders
                            displayPlaceholderTutors()
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e(TAG, "Failed to fetch tutors: ${error?.message}", error)
                        // Handle failed result
                        displayPlaceholderTutors()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tutors: ${e.message}", e)

                // Get the full stack trace
                val stackTrace = e.stackTraceToString()
                Log.e(TAG, "Stack trace: $stackTrace")

                // Show placeholders in case of error
                withContext(Dispatchers.Main) {
                    displayPlaceholderTutors()
                }
            }
        }
    }

    // Display tutors in the UI
    private fun displayTopTutors(tutors: List<TutorProfile>) {
        val container = binding.topTutorsContainer
        container.removeAllViews()

        // Take up to 5 tutors to display
        val tutorsToShow = tutors.take(5)

        Log.d(TAG, "Displaying ${tutorsToShow.size} tutors in UI: ${tutorsToShow.map { it.name }}")

        // Add tutors to container
        tutorsToShow.forEach { tutor ->
            val tutorView = LayoutInflater.from(this).inflate(R.layout.item_top_tutor, container, false)

            // Set tutor data
            val tutorImageView = tutorView.findViewById<CircleImageView>(R.id.tutorImageView)
            val tutorNameTextView = tutorView.findViewById<TextView>(R.id.tutorNameTextView)
            val tutorSubjectTextView = tutorView.findViewById<TextView>(R.id.tutorSubjectTextView)
            val tutorRatingBadge = tutorView.findViewById<TextView>(R.id.tutorRatingBadge)

            // Set tutor name - use a default if empty
            val displayName = if (tutor.name.isBlank()) "Tutor #${tutor.id}" else tutor.name
            tutorNameTextView.text = displayName

            // Display subject if available
            val primarySubject = tutor.subjects.firstOrNull() ?: "Tutor"
            tutorSubjectTextView.text = primarySubject

            // Set rating badge
            val formattedRating = String.format("%.1f", tutor.rating)
            tutorRatingBadge.text = formattedRating

            // Set click listener to view tutor profile - use startActivityForResult
            tutorView.setOnClickListener {
                Log.d(TAG, "Tutor clicked: ${tutor.id} - ${tutor.name}")
                val intent = BookingActivity.newIntent(this, tutor.id)
                startActivityForResult(intent, BookingActivity.REQUEST_BOOKING)
            }

            container.addView(tutorView)
        }
    }

    // Display message when no tutors are available
    private fun displayPlaceholderTutors() {
        val container = binding.topTutorsContainer
        container.removeAllViews()

        // Create a message view
        val messageView = LayoutInflater.from(this).inflate(R.layout.item_top_tutor, container, false)

        // Set message data
        val tutorImageView = messageView.findViewById<CircleImageView>(R.id.tutorImageView)
        val tutorNameTextView = messageView.findViewById<TextView>(R.id.tutorNameTextView)
        val tutorSubjectTextView = messageView.findViewById<TextView>(R.id.tutorSubjectTextView)
        val tutorRatingBadge = messageView.findViewById<TextView>(R.id.tutorRatingBadge)

        // Style for placeholder
        tutorNameTextView.text = "No tutors"
        tutorSubjectTextView.text = "Try again later"
        tutorRatingBadge.visibility = View.GONE

        // Add a toast message when clicked
        messageView.setOnClickListener {
            Toast.makeText(this, "Please try again later", Toast.LENGTH_SHORT).show()
        }

        container.addView(messageView)

        // Show a toast message
        Toast.makeText(this, "Unable to load tutors. Please check your connection.", Toast.LENGTH_LONG).show()
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
                    // Navigate to the subjects activity
                    val intent = Intent(this, TutorSearchActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    // Return false to not update the selected item, since we're navigating away
                    return@setOnItemSelectedListener false
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

        // Initialize selected item based on intent extras
        val fragmentToShow = intent.getStringExtra("FRAGMENT")
        if (fragmentToShow != null) {
            when (fragmentToShow) {
                "MAP" -> bottomNavigation.selectedItemId = R.id.navigation_map
                "CHAT" -> bottomNavigation.selectedItemId = R.id.navigation_chat
                "PROFILE" -> bottomNavigation.selectedItemId = R.id.navigation_profile
                else -> bottomNavigation.selectedItemId = R.id.navigation_home
            }
        } else {
            // Set the home item as selected by default
            bottomNavigation.selectedItemId = R.id.navigation_home
        }
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
            binding.dashboardScrollView.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.GONE
            // Don't change the bottom navigation background - let the XML handle it
        } else {
            binding.appBarLayout.visibility = View.GONE
            binding.dashboardShadowDivider.visibility = View.GONE
            binding.dashboardScrollView.visibility = View.GONE
            binding.fragmentContainer.visibility = View.VISIBLE
            // Don't change the bottom navigation background - let the XML handle it
        }
    }

    private fun setupClickListeners() {
        // Set up search click listener
        binding.searchEditText.setOnClickListener {
            // Navigate to the search activity
            val intent = Intent(this, TutorSearchActivity::class.java)
            startActivity(intent)
        }

        // Disable actual editing in the search box
        binding.searchEditText.keyListener = null

        // Set up filter button click listener
        binding.filterButton.setOnClickListener {
            // TODO: Show filter options
            Toast.makeText(this, "Filters coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Set up see all tutors click listener
        binding.seeAllTutors.setOnClickListener {
            // Navigate to the search activity
            val intent = Intent(this, TutorSearchActivity::class.java)
            startActivity(intent)
        }

        // Set up profile image click listener
        binding.profileImage.setOnClickListener {
            // Show the profile fragment
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
            // For example, changing the elevation of the app bar as it scrolls
            if (scrollPercentage > 0.8) {
                // When mostly collapsed, increase elevation for shadow effect
                appBarLayout.elevation = 8f
            } else {
                // When expanded, keep elevation minimal
                appBarLayout.elevation = 0f
            }
        })
    }

    private fun handleNavigationExtras() {
        // Check if we have any navigation extras
        val fragmentToShow = intent.getStringExtra("FRAGMENT")
        if (fragmentToShow != null) {
            when (fragmentToShow) {
                "MAP" -> {
                    showMainContent(false)
                    loadFragment(MapFragment())
                }
                "CHAT" -> {
                    showMainContent(false)
                    loadFragment(ChatFragment())
                }
                "PROFILE" -> {
                    showMainContent(false)
                    loadFragment(ProfileFragment())
                }
            }
        }
    }

    /**
     * Load learner sessions from the API
     */
    private fun loadLearnerSessions() {
        // Show loading indicator
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.noSessionsText.visibility = View.GONE

        val userId = PreferenceUtils.getUserId(this)
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            binding.loadingProgressBar.visibility = View.GONE
            binding.noSessionsText.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                val result = NetworkUtils.getLearnerSessions(userId.toString())
                if (result.isSuccess) {
                    val sessions = result.getOrNull() ?: emptyList()
                    if (sessions.isNotEmpty()) {
                        binding.noSessionsText.visibility = View.GONE
                        binding.allSessionsRecyclerView.visibility = View.VISIBLE
                        sessionAdapter.submitList(sessions)
                        Log.d(TAG, "Loaded ${sessions.size} sessions for learner")
                    } else {
                        binding.noSessionsText.text = "You don't have any sessions yet. Book a session with a tutor to get started!"
                        binding.noSessionsText.visibility = View.VISIBLE
                        binding.allSessionsRecyclerView.visibility = View.GONE
                        Log.d(TAG, "No sessions found for learner")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    val errorMsg = error?.message ?: "Unknown error"

                    // Check if it's a 404 error which likely means the user just doesn't have any sessions yet
                    if (errorMsg.contains("404")) {
                        Log.d(TAG, "No sessions found for learner (404): likely a new user")
                        binding.noSessionsText.text = "You don't have any sessions yet. Book a session with a tutor to get started!"
                    } else {
                        Log.e(TAG, "Failed to load learner sessions: $errorMsg", error)
                        binding.noSessionsText.text = "Unable to load sessions. Pull down to refresh."
                        Toast.makeText(this@LearnerDashboardActivity, "Failed to load sessions", Toast.LENGTH_SHORT).show()
                    }

                    binding.noSessionsText.visibility = View.VISIBLE
                    binding.allSessionsRecyclerView.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading learner sessions: ${e.message}", e)
                binding.noSessionsText.text = "Unable to load sessions. Pull down to refresh."
                binding.noSessionsText.visibility = View.VISIBLE
                binding.allSessionsRecyclerView.visibility = View.GONE
                Toast.makeText(this@LearnerDashboardActivity, "Error loading sessions", Toast.LENGTH_SHORT).show()
            } finally {
                binding.loadingProgressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Adapter for the sessions RecyclerView
     */
    private inner class SessionAdapter(private var sessions: List<NetworkUtils.TutoringSession>) : 
        androidx.recyclerview.widget.RecyclerView.Adapter<SessionAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val subject: TextView = view.findViewById(R.id.sessionSubject)
            val date: TextView = view.findViewById(R.id.sessionDate)
            val time: TextView = view.findViewById(R.id.sessionTime)
            val status: TextView = view.findViewById(R.id.sessionStatus)
            val tutorName: TextView = view.findViewById(R.id.tutorName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_session, parent, false)
            return ViewHolder(view)
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

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val session = sessions[position]

            // Set session details
            holder.subject.text = session.subject

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

                holder.date.text = formattedDate
                holder.time.text = "$formattedStartTime - $formattedEndTime"
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting date: ${e.message}", e)
                holder.date.text = "Invalid date"
                holder.time.text = "Invalid time"
            }

            holder.status.text = session.status

            // Use tutorName from session if available, otherwise fetch from network
            if (session.tutorName.isNotEmpty()) {
                holder.tutorName.text = session.tutorName
            } else {
                // Get tutor name from network if not included in session data
                lifecycleScope.launch {
                    try {
                        val tutorResult = NetworkUtils.findTutorById(session.tutorId)
                        if (tutorResult.isSuccess) {
                            val tutor = tutorResult.getOrNull()
                            if (tutor != null) {
                                holder.tutorName.text = tutor.name
                            } else {
                                holder.tutorName.text = "Unknown Tutor"
                            }
                        } else {
                            holder.tutorName.text = "Unknown Tutor"
                        }
                    } catch (e: Exception) {
                        holder.tutorName.text = "Unknown Tutor"
                    }
                }
            }

            // Set click listener to open conversation with tutor
            holder.itemView.setOnClickListener {
                // Get the current user ID
                val currentUserId = PreferenceUtils.getUserId(this@LearnerDashboardActivity) ?: -1L

                if (currentUserId == -1L) {
                    Toast.makeText(this@LearnerDashboardActivity, 
                        "Unable to identify current user. Please log in again.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Show loading toast
                Toast.makeText(this@LearnerDashboardActivity, 
                    "Opening conversation...", Toast.LENGTH_SHORT).show()

                // Launch coroutine to handle conversation
                lifecycleScope.launch {
                    try {
                        // Get studentId and tutorId from the session
                        val studentId = session.learnerId.toLongOrNull() ?: currentUserId
                        val tutorId = session.tutorId

                        // Log the IDs for debugging
                        Log.d(TAG, "Attempting to create/find conversation - Student ID: $studentId, Tutor ID: $tutorId")

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
                                    openConversation(sessionConversation.id, tutorId, session.tutorName)
                                    return@launch
                                }
                            }

                            // Look for a conversation with this tutor
                            val existingConversation = conversations.find { conversation ->
                                (conversation.tutorId == tutorId && conversation.studentId == currentUserId) ||
                                (conversation.studentId == currentUserId && conversation.tutorId == tutorId)
                            }

                            if (existingConversation != null) {
                                // Log the existing conversation
                                Log.d(TAG, "Using existing conversation ${existingConversation.id} between student $currentUserId and tutor $tutorId")

                                // Open existing conversation
                                openConversation(existingConversation.id, tutorId, session.tutorName)
                            } else {
                                // Create new conversation using tutorId and studentId instead of user IDs
                                Log.d(TAG, "Creating new conversation with studentId=$currentUserId, tutorId=$tutorId, sessionId=${session.id}")

                                // Use createConversationWithTutor which handles the conversion of tutorId to userId
                                // Pass the session ID to associate the conversation with the session
                                val createResult = NetworkUtils.createConversationWithTutor(currentUserId, tutorId, session.id)

                                if (createResult.isSuccess) {
                                    val newConversation = createResult.getOrNull()
                                    if (newConversation != null) {
                                        // Log the created conversation
                                        Log.d(TAG, "Successfully created conversation ${newConversation.id} with student=${newConversation.studentId}, tutor=${newConversation.tutorId}")

                                        openConversation(newConversation.id, tutorId, session.tutorName)
                                    } else {
                                        Log.e(TAG, "Conversation creation succeeded but returned null")
                                        Toast.makeText(this@LearnerDashboardActivity, 
                                            "Failed to create conversation", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Log the error
                                    val error = createResult.exceptionOrNull()
                                    Log.e(TAG, "Failed to create conversation: ${error?.message}", error)

                                    Toast.makeText(this@LearnerDashboardActivity, 
                                        "Failed to create conversation", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Log the error
                            val error = conversationsResult.exceptionOrNull()
                            Log.e(TAG, "Failed to load conversations: ${error?.message}", error)

                            Toast.makeText(this@LearnerDashboardActivity, 
                                "Failed to load conversations", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error opening conversation: ${e.message}", e)
                        Toast.makeText(this@LearnerDashboardActivity, 
                            "Error opening conversation: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun getItemCount() = sessions.size

        /**
         * Update the list of sessions
         */
        fun submitList(newSessions: List<NetworkUtils.TutoringSession>) {
            sessions = newSessions
            notifyDataSetChanged()
        }
    }

    /**
     * Set up the RecyclerView for sessions
     */
    private fun setupSessionsRecyclerView() {
        binding.allSessionsRecyclerView.apply {
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(this@LearnerDashboardActivity)
        }
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

                    // Special handling for searchEditText in learner dashboard
                    // which already has a click listener to navigate to search
                    if (child.id != R.id.searchEditText) {
                        // Enable focusing only when explicitly clicked
                        child.setOnClickListener {
                            child.isFocusableInTouchMode = true
                            child.isFocusable = true
                            child.requestFocus()
                        }
                    }
                }
            }

            // Recursively check child view groups
            if (child is ViewGroup) {
                disableSearchEditTextAutoFocus(child)
            }
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
     * Loads the real user data from the server including profile and other relevant information
     */
    private fun loadRealData() {
        // Get the user email from preferences
        val userEmail = PreferenceUtils.getUserEmail(this)

        if (userEmail != null) {
            // Launch coroutine to fetch data
            lifecycleScope.launch {
                try {
                    // Find the user by email
                    val userResult = NetworkUtils.findUserByEmail(userEmail)
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        if (user != null) {
                            Log.d(TAG, "Successfully loaded user data for: ${user.firstName} ${user.lastName}")

                            // Update UI with user data if needed
                            // Example: Update profile image, name, etc.

                            // Load additional data if necessary
                            // loadAdditionalUserData(user.userId)
                        } else {
                            Log.e(TAG, "User is null for email: $userEmail")
                            Toast.makeText(
                                this@LearnerDashboardActivity,
                                "Failed to load user data. Please try again later.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Handle error
                        Log.e(TAG, "Failed to find user by email: $userEmail")
                        Toast.makeText(
                            this@LearnerDashboardActivity,
                            "Failed to load user data. Please try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading user data: ${e.message}", e)
                    Toast.makeText(
                        this@LearnerDashboardActivity,
                        "Error loading data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            // Handle case where user email is not available
            Log.e(TAG, "User email is not available")
            Toast.makeText(
                this@LearnerDashboardActivity,
                "User email not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Helper function to find a TextView with specific text in the view hierarchy
     */
    private fun findViewWithText(root: View, text: String): TextView? {
        if (root is TextView && root.text == text) {
            return root
        } else if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findViewWithText(root.getChildAt(i), text)
                if (found != null) return found
            }
        }
        return null
    }

}
