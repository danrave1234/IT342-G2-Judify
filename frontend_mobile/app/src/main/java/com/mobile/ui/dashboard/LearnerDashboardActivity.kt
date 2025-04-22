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

class LearnerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnerDashboardBinding
    private val TAG = "LearnerDashboard"

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

        // Load top tutors
        loadTopTutors()
        
        // Load learner sessions
        loadLearnerSessions()

        // Set up bottom navigation
        setupBottomNavigation()

        // Handle navigation from other activities
        handleNavigationExtras()
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

            // Set click listener to view tutor profile
            tutorView.setOnClickListener {
                val intent = Intent(this, BookingActivity::class.java).apply {
                    putExtra(BookingActivity.EXTRA_TUTOR_ID, tutor.id)
                }
                startActivity(intent)
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
     * Load sessions for the current learner
     */
    private fun loadLearnerSessions() {
        lifecycleScope.launch {
            try {
                // Get the user ID from preferences
                val userId = PreferenceUtils.getUserId(this@LearnerDashboardActivity)
                if (userId != null) {
                    // Get all sessions
                    val allSessionsResult = NetworkUtils.getLearnerSessions(userId)
                    if (allSessionsResult.isSuccess) {
                        val allSessions = allSessionsResult.getOrNull() ?: emptyList()
                        
                        // Get upcoming sessions
                        val upcomingSessionsResult = NetworkUtils.getUpcomingLearnerSessions(userId)
                        if (upcomingSessionsResult.isSuccess) {
                            val upcomingSessions = upcomingSessionsResult.getOrNull() ?: emptyList()
                            
                            // Display sessions in the UI
                            displaySessions(allSessions)
                        } else {
                            // Handle error
                            showNoSessions()
                        }
                    } else {
                        // Handle error
                        showNoSessions()
                    }
                } else {
                    // Handle error - user not found
                    Log.e(TAG, "User ID not found in preferences")
                    showNoSessions()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sessions: ${e.message}", e)
                showNoSessions()
            }
        }
    }
    
    /**
     * Display the sessions in the UI
     */
    private fun displaySessions(sessions: List<NetworkUtils.TutoringSession>) {
        // Set up RecyclerView for sessions
        val recyclerView = binding.allSessionsRecyclerView
        
        if (sessions.isEmpty()) {
            showNoSessions()
            return
        }
        
        // Create an adapter for the sessions
        val adapter = SessionAdapter(sessions)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    /**
     * Show a message when no sessions are available
     */
    private fun showNoSessions() {
        val recyclerView = binding.allSessionsRecyclerView
        recyclerView.visibility = View.GONE
        
        // Create and show a message in place of the RecyclerView
        val noSessionsView = layoutInflater.inflate(R.layout.item_no_sessions, null)
        val parent = recyclerView.parent as LinearLayout
        
        // Remove any existing no-sessions view first
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.id == R.id.noSessionsView) {
                parent.removeViewAt(i)
                break
            }
        }
        
        // Set up click listener for Find Tutors button
        noSessionsView.findViewById<com.google.android.material.button.MaterialButton>(R.id.findTutorButton)
            .setOnClickListener {
                val intent = Intent(this, TutorSearchActivity::class.java)
                startActivity(intent)
            }
        
        // Add the no-sessions view
        parent.addView(noSessionsView, parent.indexOfChild(recyclerView) + 1)
    }
    
    /**
     * Adapter for the sessions RecyclerView
     */
    private inner class SessionAdapter(private val sessions: List<NetworkUtils.TutoringSession>) : 
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
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val session = sessions[position]
            
            // Set session details
            holder.subject.text = session.subject
            
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
                    
                    holder.date.text = formattedDate
                    holder.time.text = "$formattedStartTime - $formattedEndTime"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting date: ${e.message}")
                holder.date.text = "Invalid date"
                holder.time.text = "Invalid time"
            }
            
            holder.status.text = session.status
            
            // Get tutor name - in a real app, you would load this from the network
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
            
            // Set click listener to view session details
            holder.itemView.setOnClickListener {
                // TODO: Navigate to session details
                Toast.makeText(this@LearnerDashboardActivity, 
                    "Session details coming soon!", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun getItemCount() = sessions.size
    }

}
