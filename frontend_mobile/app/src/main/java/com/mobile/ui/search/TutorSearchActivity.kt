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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mobile.R
import com.mobile.ui.booking.BookingActivity

class TutorSearchActivity : AppCompatActivity() {

    private lateinit var viewModel: TutorSearchViewModel
    private lateinit var adapter: TutorListAdapter

    // UI Components
    private lateinit var toolbar: Toolbar
    private lateinit var searchInputLayout: TextInputLayout
    private lateinit var searchEditText: TextInputEditText
    private lateinit var nearbyCheckbox: CheckBox
    private lateinit var locationProgressBar: ProgressBar
    private lateinit var tutorsRecyclerView: RecyclerView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var emptyTextView: TextView
    private lateinit var errorTextView: TextView

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

        // Initial search
        viewModel.searchTutors("")
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        searchInputLayout = findViewById(R.id.searchInputLayout)
        searchEditText = findViewById(R.id.searchEditText)
        nearbyCheckbox = findViewById(R.id.nearbyCheckbox)
        locationProgressBar = findViewById(R.id.locationProgressBar)
        tutorsRecyclerView = findViewById(R.id.tutorsRecyclerView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        emptyTextView = findViewById(R.id.emptyTextView)
        errorTextView = findViewById(R.id.errorTextView)
    }

    private fun setupListeners() {
        // Search icon click
        searchInputLayout.setEndIconOnClickListener {
            performSearch()
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
} 
