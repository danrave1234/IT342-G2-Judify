package com.mobile.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
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
import com.mobile.ui.courses.CoursesFragment
import com.mobile.ui.courses.adapters.CourseAdapter
import com.mobile.ui.courses.models.Course
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

    private lateinit var allCoursesAdapter: CourseAdapter

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

        // Set up courses
        setupCourses()

        // Set up bottom navigation
        setupBottomNavigation()
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
            val tutorRatingBar = tutorView.findViewById<RatingBar>(R.id.tutorRatingBar)

            // Set tutor name - use a default if empty
            val displayName = if (tutor.name.isBlank()) "Tutor #${tutor.id}" else tutor.name
            tutorNameTextView.text = displayName

            // Set rating - default to 4.0 if 0
            val rating = if (tutor.rating <= 0f) 4.0f else tutor.rating
            tutorRatingBar.rating = rating

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
        val tutorNameTextView = messageView.findViewById<TextView>(R.id.tutorNameTextView)
        val tutorRatingBar = messageView.findViewById<RatingBar>(R.id.tutorRatingBar)

        tutorNameTextView.text = "No tutors available"
        tutorRatingBar.visibility = View.GONE

        // Add a toast message when clicked
        messageView.setOnClickListener {
            Toast.makeText(this, "Please try again later", Toast.LENGTH_SHORT).show()
        }

        container.addView(messageView)

        // Show a toast message
        Toast.makeText(this, "Unable to load tutors. Please check your connection.", Toast.LENGTH_LONG).show()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Show the main dashboard content
                    showMainContent(true)
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_courses -> {
                    // Show the courses fragment
                    showMainContent(false)
                    loadFragment(CoursesFragment())
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
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
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
        // Set up search functionality
        binding.searchEditText.setOnClickListener {
            // Launch TutorSearchActivity for finding tutors
            startActivity(Intent(this, TutorSearchActivity::class.java))
        }

        // Set up filter functionality
        binding.filterButton.setOnClickListener {
            // TODO: Implement filter functionality
        }

        // Set up see all tutors
        binding.seeAllTutors.setOnClickListener {
            // Launch TutorSearchActivity for finding tutors
            startActivity(Intent(this, TutorSearchActivity::class.java))
        }

        // Set up notification icon click
        binding.notificationIcon.setOnClickListener {
            // TODO: Navigate to notifications screen
        }

        // Set up message icon click
        binding.messageIcon.setOnClickListener {
            // TODO: Navigate to messages screen
        }

        // Set up special offer card click
        binding.specialOfferCard.setOnClickListener {
            // TODO: Navigate to special offer details
        }

        // Set up profile image click
        binding.profileImage.setOnClickListener {
            // Show the profile fragment and hide the main content
            showMainContent(false)
            loadFragment(ProfileFragment())
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

    private fun setupCourses() {
        // Load courses asynchronously
        lifecycleScope.launch {
            try {
                // All Courses RecyclerView
                binding.allCoursesRecyclerView.layoutManager = LinearLayoutManager(this@LearnerDashboardActivity)

                allCoursesAdapter = CourseAdapter(
                    onCourseClick = { course ->
                        // Handle course click - navigate to book a session with the tutor
                        if (course.tutorId == null) {
                            Toast.makeText(this@LearnerDashboardActivity, "Tutor information not available", Toast.LENGTH_SHORT).show()
                            return@CourseAdapter
                        }

                        // Create intent with proper extras
                        val intent = Intent(this@LearnerDashboardActivity, BookingActivity::class.java).apply {
                            putExtra(BookingActivity.EXTRA_TUTOR_ID, course.tutorId)
                            putExtra(BookingActivity.EXTRA_COURSE_ID, course.id)
                            putExtra(BookingActivity.EXTRA_COURSE_TITLE, course.title)
                        }
                        startActivity(intent)
                    }
                )
                binding.allCoursesRecyclerView.adapter = allCoursesAdapter

                // Load course data
                val allCourses = NetworkUtils.getAllCourses()
                allCoursesAdapter.submitList(allCourses)

                Log.d(TAG, "Loaded ${allCourses.size} all courses")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading courses: ${e.message}", e)
            }
        }
    }

    private fun filterCoursesByCategory(category: String?) {
        lifecycleScope.launch {
            try {
                val filteredCourses = NetworkUtils.getCoursesByCategory(category)
                allCoursesAdapter.submitList(filteredCourses)
                Log.d(TAG, "Filtered courses by category: $category, found ${filteredCourses.size} courses")
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering courses: ${e.message}", e)
                // TODO: Show error message
            }
        }
    }
} 
