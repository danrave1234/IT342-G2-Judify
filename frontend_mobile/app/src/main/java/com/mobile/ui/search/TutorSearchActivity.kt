package com.mobile.ui.search

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mobile.R
import com.mobile.ui.booking.BookingActivity
import com.mobile.ui.chat.ChatFragment
import com.mobile.ui.dashboard.LearnerDashboardActivity
import com.mobile.ui.map.MapFragment
import com.mobile.ui.profile.ProfileFragment

class TutorSearchActivity : AppCompatActivity() {

    private lateinit var viewModel: TutorSearchViewModel
    private lateinit var adapter: TutorListAdapter

    // UI Components
    private lateinit var searchInputLayout: TextInputLayout
    private lateinit var searchEditText: TextInputEditText
    private lateinit var filterButton: MaterialButton
    private lateinit var nearbyCheckbox: CheckBox
    private lateinit var locationProgressBar: ProgressBar
    private lateinit var tutorsRecyclerView: RecyclerView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyTextView: TextView
    private lateinit var errorTextView: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    // Location permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.getCurrentLocation(this)
            nearbyCheckbox.isChecked = true
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            nearbyCheckbox.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutor_search)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(TutorSearchViewModel::class.java)

        // Initialize UI components
        initializeViews()
        setupListeners()
        setupRecyclerView()
        setupObservers()
        setupBottomNavigation()

        // Load random tutors initially
        viewModel.loadRandomTutors()
    }

    private fun initializeViews() {
        searchInputLayout = findViewById(R.id.searchInputLayout)
        searchEditText = findViewById(R.id.searchEditText)
        filterButton = findViewById(R.id.filterButton)
        nearbyCheckbox = findViewById(R.id.nearbyCheckbox)
        locationProgressBar = findViewById(R.id.locationProgressBar)
        tutorsRecyclerView = findViewById(R.id.subjectsRecyclerView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        emptyTextView = findViewById(R.id.emptyTextView)
        errorTextView = findViewById(R.id.errorTextView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupListeners() {
        // Search icon click
        searchInputLayout.setEndIconOnClickListener {
            performSearch()
        }

        // Filter button click
        filterButton.setOnClickListener {
            // TODO: Show filter dialog
            Toast.makeText(this, "Filter functionality coming soon", Toast.LENGTH_SHORT).show()
        }

        // Search on keyboard action
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                return@setOnEditorActionListener true
            }
            false
        }

        // Nearby checkbox
        nearbyCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) -> {
                        viewModel.getCurrentLocation(this)
                    }
                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            } else {
                viewModel.clearLocationFilter()
                performSearch()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TutorListAdapter { tutorId ->
            navigateToTutorDetail(tutorId)
        }
        tutorsRecyclerView.adapter = adapter
    }

    private fun setupObservers() {
        // Observe search results
        viewModel.searchState.observe(this) { state ->
            // Update UI based on state
            loadingProgressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Handle location loading state
            locationProgressBar.visibility = if (state.isLocationLoading) View.VISIBLE else View.GONE

            // Handle error state
            if (state.error != null) {
                errorTextView.text = "Error: ${state.error}"
                errorTextView.visibility = View.VISIBLE
                emptyTextView.visibility = View.GONE
                tutorsRecyclerView.visibility = View.GONE
            } else {
                errorTextView.visibility = View.GONE

                // Handle empty results
                if (state.tutors.isEmpty() && !state.isLoading) {
                    emptyTextView.visibility = View.VISIBLE
                    tutorsRecyclerView.visibility = View.GONE
                } else {
                    emptyTextView.visibility = View.GONE
                    tutorsRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(state.tutors)
                }
            }
        }
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        viewModel.searchTutors(query)
    }

    private fun navigateToTutorDetail(tutorId: Long) {
        // In a production app, navigate to the TutorDetailActivity
        // For now, navigate straight to BookingActivity
        val intent = BookingActivity.newIntent(this, tutorId)
        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        // Set the current tab to sessions/subjects
        bottomNavigation.selectedItemId = R.id.navigation_sessions

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Navigate to the home/dashboard
                    val intent = Intent(this, LearnerDashboardActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_sessions -> {
                    // Already on the subjects page
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_map -> {
                    // Navigate to the dashboard with the Map fragment
                    val intent = Intent(this, LearnerDashboardActivity::class.java)
                    intent.putExtra("FRAGMENT", "MAP")
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_chat -> {
                    // Navigate to the dashboard with the Chat fragment
                    val intent = Intent(this, LearnerDashboardActivity::class.java)
                    intent.putExtra("FRAGMENT", "CHAT")
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_profile -> {
                    // Navigate to the dashboard with the Profile fragment
                    val intent = Intent(this, LearnerDashboardActivity::class.java)
                    intent.putExtra("FRAGMENT", "PROFILE")
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    return@setOnItemSelectedListener true
                }
                else -> return@setOnItemSelectedListener false
            }
        }
    }
}
