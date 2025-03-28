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

class LearnerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnerDashboardBinding
    private val TAG = "LearnerDashboard"

    private lateinit var popularCoursesAdapter: CourseAdapter
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

        // Set up category selection
        setupCategorySelection()

        // Set up courses
        setupCourses()

        // Set up bottom navigation
        setupBottomNavigation()
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
            startActivity(Intent(this, com.mobile.ui.search.TutorSearchActivity::class.java))
        }

        // Set up filter functionality
        binding.filterButton.setOnClickListener {
            // TODO: Implement filter functionality
        }

        // Set up see all mentors
        binding.seeAllMentors.setOnClickListener {
            // Launch TutorSearchActivity for finding tutors
            startActivity(Intent(this, com.mobile.ui.search.TutorSearchActivity::class.java))
        }

        // Set up see all categories
        binding.seeAllCategories.setOnClickListener {
            // TODO: Navigate to all categories screen
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

    private fun setupCategorySelection() {
        // Find all category views in the horizontal scroll view
        val categoriesContainer = binding.categoriesSection.getChildAt(1) as HorizontalScrollView
        val categoriesLayout = categoriesContainer.getChildAt(0) as android.view.ViewGroup

        // Set up click listeners for each category
        for (i in 0 until categoriesLayout.childCount) {
            val categoryView = categoriesLayout.getChildAt(i) as TextView
            categoryView.setOnClickListener {
                // Reset all categories to unselected state
                for (j in 0 until categoriesLayout.childCount) {
                    val otherCategory = categoriesLayout.getChildAt(j) as TextView
                    otherCategory.setBackgroundResource(R.drawable.category_background)
                    otherCategory.setTextColor(resources.getColor(android.R.color.black, theme))
                }

                // Set clicked category to selected state
                categoryView.setBackgroundResource(R.drawable.category_selected_background)
                categoryView.setTextColor(resources.getColor(android.R.color.white, theme))

                // Filter courses based on selected category
                val category = categoryView.text.toString()
                filterCoursesByCategory(if (category == "All") null else category)
            }
        }
    }

    private fun setupCourses() {
        // Initialize RecyclerViews
        binding.popularCoursesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.allCoursesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapters
        popularCoursesAdapter = CourseAdapter(
            onCourseClick = { course ->
                // Handle course click
                Log.d(TAG, "Popular course clicked: ${course.title}")
                // TODO: Navigate to course details
            }
        )

        allCoursesAdapter = CourseAdapter(
            onCourseClick = { course ->
                // Handle course click
                Log.d(TAG, "Course clicked: ${course.title}")
                // TODO: Navigate to course details
            }
        )

        // Set adapters to RecyclerViews
        binding.popularCoursesRecyclerView.adapter = popularCoursesAdapter
        binding.allCoursesRecyclerView.adapter = allCoursesAdapter

        // Load courses
        loadCourses()
    }

    private fun loadCourses() {
        lifecycleScope.launch {
            try {
                // Load popular courses
                val popularCourses = NetworkUtils.getPopularCourses()
                popularCoursesAdapter.submitList(popularCourses)

                // Load all courses
                val allCourses = NetworkUtils.getAllCourses()
                allCoursesAdapter.submitList(allCourses)

                Log.d(TAG, "Loaded ${popularCourses.size} popular courses and ${allCourses.size} all courses")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading courses: ${e.message}", e)
                // TODO: Show error message
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
