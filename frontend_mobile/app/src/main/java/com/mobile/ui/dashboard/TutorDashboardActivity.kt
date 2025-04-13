package com.mobile.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.mobile.R
import com.mobile.databinding.ActivityTutorDashboardBinding
import com.mobile.ui.chat.ChatFragment
import com.mobile.ui.courses.CoursesFragment
import com.mobile.ui.map.MapFragment
import com.mobile.ui.profile.ProfileFragment
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TutorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorDashboardBinding
    private val TAG = "TutorDashboard"

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

        // Load mock data for demonstration
        loadMockData()
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
            binding.bottomNavigation.selectedItemId = R.id.navigation_profile
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

    private fun loadMockData() {
        // Set mock rating
        binding.ratingValue.text = "4.8"

        // Set mock session counts
        binding.totalSessionsCount.text = "24"
        binding.upcomingSessionsCount.text = "3"

        // Add mock upcoming sessions
        addMockSessions()

        // Add mock subjects
        addMockSubjects()
    }

    private fun addMockSessions() {
        // Clear existing sessions
        binding.upcomingSessionsContainer.removeAllViews()
        binding.noSessionsText.visibility = View.GONE

        // Add mock session cards
        addMockSessionCard("Mathematics", "May 15, 2023", "2:00 PM - 3:30 PM", "Scheduled")
        addMockSessionCard("Physics", "May 16, 2023", "10:00 AM - 11:30 AM", "Confirmed")
        addMockSessionCard("Chemistry", "May 18, 2023", "4:00 PM - 5:30 PM", "Scheduled")
    }

    private fun addMockSessionCard(subject: String, date: String, time: String, status: String) {
        // Create a card for the session
        val cardView = layoutInflater.inflate(R.layout.item_session_card, null) as CardView

        // Set session details
        val titleText = cardView.findViewById<TextView>(R.id.sessionTitle)
        val dateText = cardView.findViewById<TextView>(R.id.sessionDate)
        val timeText = cardView.findViewById<TextView>(R.id.sessionTime)
        val statusText = cardView.findViewById<TextView>(R.id.sessionStatus)

        titleText.text = subject
        dateText.text = date
        timeText.text = time
        statusText.text = status

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

    private fun addMockSubjects() {
        // Clear existing subjects
        binding.subjectsContainer.removeAllViews()
        binding.noSubjectsText.visibility = View.GONE

        // Add mock subject chips
        addSubjectChip("Mathematics")
        addSubjectChip("Physics")
        addSubjectChip("Chemistry")
        addSubjectChip("Biology")
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
