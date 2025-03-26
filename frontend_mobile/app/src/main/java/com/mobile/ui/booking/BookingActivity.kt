package com.mobile.ui.booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mobile.R
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PaymentUtils
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingActivity : AppCompatActivity() {

    private lateinit var viewModel: BookingViewModel

    // UI components
    private lateinit var toolbar: Toolbar
    private lateinit var tutorImage: CircleImageView
    private lateinit var tutorNameText: TextView
    private lateinit var tutorExpertiseText: TextView
    private lateinit var tutorRatingText: TextView
    private lateinit var tutorPriceText: TextView
    private lateinit var dateEditText: TextInputEditText
    private lateinit var timeEditText: TextInputEditText
    private lateinit var durationDropdown: AutoCompleteTextView
    private lateinit var subjectDropdown: AutoCompleteTextView
    private lateinit var sessionTypeDropdown: AutoCompleteTextView
    private lateinit var notesEditText: TextInputEditText
    private lateinit var tutorRateText: TextView
    private lateinit var summaryDurationText: TextView
    private lateinit var totalPriceText: TextView
    private lateinit var bookButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    // Date and time variables
    private val calendar = Calendar.getInstance()
    private var selectedDuration = 1.0
    private var tutorId: Long = 0
    private var selectedSessionType = ""

    // Formatters
    private val dateFormatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val apiDateTimeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        // Get the tutor ID from the intent
        tutorId = intent.getLongExtra(EXTRA_TUTOR_ID, -1)
        if (tutorId == -1L) {
            Toast.makeText(this, "Invalid tutor ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup the ViewModel with factory
        viewModel = ViewModelProvider(
            this,
            BookingViewModelFactory(application, tutorId)
        ).get(BookingViewModel::class.java)

        // Initialize UI components
        initViews()
        setupListeners()
        
        // Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Set up observers
        observeBookingState()
        
        // Load tutor profile
        viewModel.loadTutorProfile()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tutorImage = findViewById(R.id.tutorImage)
        tutorNameText = findViewById(R.id.tutorNameText)
        tutorExpertiseText = findViewById(R.id.tutorExpertiseText)
        tutorRatingText = findViewById(R.id.tutorRatingText)
        tutorPriceText = findViewById(R.id.tutorPriceText)
        dateEditText = findViewById(R.id.dateEditText)
        timeEditText = findViewById(R.id.timeEditText)
        durationDropdown = findViewById(R.id.durationDropdown)
        subjectDropdown = findViewById(R.id.subjectDropdown)
        sessionTypeDropdown = findViewById(R.id.sessionTypeDropdown)
        notesEditText = findViewById(R.id.notesEditText)
        tutorRateText = findViewById(R.id.tutorRateText)
        summaryDurationText = findViewById(R.id.summaryDurationText)
        totalPriceText = findViewById(R.id.totalPriceText)
        bookButton = findViewById(R.id.bookButton)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        
        // Set up dropdown adapters
        setupDropdowns()
    }

    private fun setupDropdowns() {
        // Duration dropdown
        val durations = resources.getStringArray(R.array.duration_options)
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, durations)
        durationDropdown.setAdapter(durationAdapter)
        durationDropdown.setText(durations[0], false) // Default to 1 hour
        
        // Subject dropdown (will be populated when tutor data is loaded)
        val subjects = mutableListOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "History")
        val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, subjects)
        subjectDropdown.setAdapter(subjectAdapter)
        
        // Session type dropdown
        val sessionTypes = resources.getStringArray(R.array.session_types)
        val sessionTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sessionTypes)
        sessionTypeDropdown.setAdapter(sessionTypeAdapter)
        sessionTypeDropdown.setText(sessionTypes[0], false) // Default to face-to-face
        selectedSessionType = sessionTypes[0]
    }

    private fun setupListeners() {
        // Date selection
        dateEditText.setOnClickListener {
            showDatePicker()
        }
        
        // Time selection
        timeEditText.setOnClickListener {
            showTimePicker()
        }
        
        // Duration selection
        durationDropdown.setOnItemClickListener { _, _, position, _ ->
            val durations = resources.getStringArray(R.array.duration_options)
            val selectedDurationText = durations[position]
            
            // Extract number from string like "1 hour", "1.5 hours", etc.
            selectedDuration = selectedDurationText.split(" ")[0].toDouble()
            summaryDurationText.text = selectedDurationText
            
            updateTotalPrice()
        }
        
        // Session type selection
        sessionTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            val sessionTypes = resources.getStringArray(R.array.session_types)
            selectedSessionType = sessionTypes[position]
        }
        
        // Book button
        bookButton.setOnClickListener {
            if (validateInputs()) {
                bookSession()
            }
        }
    }

    private fun observeBookingState() {
        viewModel.bookingState.observe(this) { state ->
            when {
                state.isLoading -> {
                    progressBar.visibility = View.VISIBLE
                    errorText.visibility = View.GONE
                    bookButton.isEnabled = false
                }
                state.error != null -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = state.error
                    bookButton.isEnabled = true
                }
                state.bookingComplete -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    
                    // Show success message and finish activity
                    Toast.makeText(this, "Session booked successfully!", Toast.LENGTH_LONG).show()
                    finish()
                }
                state.tutorProfile != null -> {
                    progressBar.visibility = View.GONE
                    errorText.visibility = View.GONE
                    
                    // Update UI with tutor data
                    updateTutorInfo(state.tutorProfile)
                }
            }
        }
    }

    private fun updateTutorInfo(tutorProfile: NetworkUtils.TutorProfile) {
        tutorNameText.text = tutorProfile.name
        tutorExpertiseText.text = tutorProfile.bio
        tutorRatingText.text = tutorProfile.rating.toString()
        
        val priceText = "$${tutorProfile.hourlyRate}/hour"
        tutorPriceText.text = priceText
        tutorRateText.text = priceText
        
        // Populate subjects from tutor's subjects list
        val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tutorProfile.subjects)
        subjectDropdown.setAdapter(subjectAdapter)
        
        if (tutorProfile.subjects.isNotEmpty()) {
            subjectDropdown.setText(tutorProfile.subjects[0], false)
        }
        
        // Update total price
        updateTotalPrice()
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Set min date to today
        val today = Calendar.getInstance()
        datePickerDialog.datePicker.minDate = today.timeInMillis
        
        // Set max date to 3 months from now
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.MONTH, 3)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
        
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                updateTimeInView()
            },
            hour,
            minute,
            false
        )
        
        timePickerDialog.show()
    }

    private fun updateDateInView() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateEditText.setText(dateFormat.format(calendar.time))
    }

    private fun updateTimeInView() {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        timeEditText.setText(timeFormat.format(calendar.time))
    }

    private fun updateTotalPrice() {
        val tutorProfile = viewModel.bookingState.value?.tutorProfile
        if (tutorProfile != null) {
            val totalPrice = tutorProfile.hourlyRate * selectedDuration
            totalPriceText.text = String.format("$%.2f", totalPrice)
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Check date
        if (dateEditText.text.isNullOrEmpty()) {
            (dateEditText.parent.parent as TextInputLayout).error = "Please select a date"
            isValid = false
        } else {
            (dateEditText.parent.parent as TextInputLayout).error = null
        }
        
        // Check time
        if (timeEditText.text.isNullOrEmpty()) {
            (timeEditText.parent.parent as TextInputLayout).error = "Please select a time"
            isValid = false
        } else {
            (timeEditText.parent.parent as TextInputLayout).error = null
        }
        
        // Check subject
        if (subjectDropdown.text.isNullOrEmpty()) {
            (subjectDropdown.parent.parent as TextInputLayout).error = "Please select a subject"
            isValid = false
        } else {
            (subjectDropdown.parent.parent as TextInputLayout).error = null
        }
        
        // Check session type
        if (sessionTypeDropdown.text.isNullOrEmpty()) {
            (sessionTypeDropdown.parent.parent as TextInputLayout).error = "Please select a session type"
            isValid = false
        } else {
            (sessionTypeDropdown.parent.parent as TextInputLayout).error = null
        }
        
        // Check if date is in the future
        val now = Calendar.getInstance()
        if (calendar.before(now)) {
            (dateEditText.parent.parent as TextInputLayout).error = "Please select a future date and time"
            isValid = false
        }
        
        return isValid
    }

    private fun bookSession() {
        // Show progress
        progressBar.visibility = View.VISIBLE
        bookButton.isEnabled = false
        
        // Get values from inputs
        val date = calendar.timeInMillis
        val startTime = apiDateTimeFormatter.format(calendar.time)
        
        // Calculate end time
        val endCalendar = (calendar.clone() as Calendar).apply {
            add(Calendar.MINUTE, (selectedDuration * 60).toInt())
        }
        val endTime = apiDateTimeFormatter.format(endCalendar.time)
        
        val subject = subjectDropdown.text.toString()
        val sessionType = sessionTypeDropdown.text.toString()
        val notes = notesEditText.text.toString()
        
        // Calculate total price
        val tutorProfile = viewModel.bookingState.value?.tutorProfile
        val totalPrice = tutorProfile?.hourlyRate?.times(selectedDuration) ?: 0.0
        
        // Create booking request
        viewModel.bookSession(
            startTime = startTime,
            endTime = endTime,
            subject = subject,
            sessionType = sessionType,
            notes = notes
        ) { success ->
            if (success) {
                // If booking is successful, proceed to payment
                processPayment(totalPrice)
            } else {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    bookButton.isEnabled = true
                    Toast.makeText(this, "Failed to book session", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun processPayment(amount: Double) {
        // Show payment processing state
        progressBar.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        errorText.text = "Processing payment..."
        
        // Launch coroutine for payment processing
        lifecycleScope.launch {
            try {
                // Create a payment source using Paymongo
                val callbackUrl = "https://judify.com/payment/callback"
                val result = PaymentUtils.createPaymentSource(
                    amount = amount,
                    description = "Tutoring Session with ${tutorNameText.text}",
                    callbackUrl = callbackUrl
                )
                
                result.fold(
                    onSuccess = { source ->
                        // In a real app, you would:
                        // 1. Store the payment source ID in your backend
                        // 2. Redirect the user to the payment page using the source.redirect.checkout_url
                        // 3. Handle the callback when the user completes or cancels the payment
                        
                        // For demo purposes, we'll simulate a successful payment
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@BookingActivity,
                                "Payment processed successfully! Reference: ${source.reference}",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Show success and finish activity
                            progressBar.visibility = View.GONE
                            finish()
                        }
                    },
                    onFailure = { exception ->
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            bookButton.isEnabled = true
                            errorText.visibility = View.VISIBLE
                            errorText.text = "Payment error: ${exception.message}"
                            
                            Toast.makeText(
                                this@BookingActivity,
                                "Payment failed: ${exception.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    bookButton.isEnabled = true
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Payment error: ${e.message}"
                    
                    Toast.makeText(
                        this@BookingActivity,
                        "Payment failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    companion object {
        const val EXTRA_TUTOR_ID = "extra_tutor_id"
        
        fun newIntent(context: Context, tutorId: Long): Intent {
            return Intent(context, BookingActivity::class.java).apply {
                putExtra(EXTRA_TUTOR_ID, tutorId)
            }
        }
    }
}

// Factory for creating BookingViewModel with a parameter
// Removing duplicate factory class since we've moved it to a separate file 