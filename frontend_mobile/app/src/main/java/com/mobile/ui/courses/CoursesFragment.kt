package com.mobile.ui.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.mobile.R
import com.mobile.ui.base.BaseFragment
import com.mobile.ui.courses.adapters.CourseAdapter
import com.mobile.ui.courses.models.Course
import com.mobile.ui.dashboard.TutorDashboardActivity
import com.mobile.ui.booking.BookingActivity
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment for displaying courses/subjects
 */
class CoursesFragment : BaseFragment() {

    // UI Components
    private lateinit var searchEditText: EditText
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var allCoursesRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var createCourseButton: FloatingActionButton

    // Flag to track if user is a tutor
    private var isTutor = false

    // Adapters
    private lateinit var allCoursesAdapter: CourseAdapter

    // ViewModel
    private lateinit var viewModel: CoursesViewModel

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_courses
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(CoursesViewModel::class.java)

        // Initialize UI components
        initializeViews(view)

        // Check if user is a tutor (based on parent activity)
        isTutor = activity is TutorDashboardActivity

        // Show create course button if user is a tutor
        if (isTutor) {
            createCourseButton.visibility = View.VISIBLE
        }

        // Set up RecyclerViews
        setupRecyclerViews()

        // Set up listeners
        setupListeners()

        // Set up observers
        setupObservers()

        // Load courses data
        loadCoursesData()
    }

    private fun initializeViews(view: View) {
        searchEditText = view.findViewById(R.id.searchEditText)
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup)
        allCoursesRecyclerView = view.findViewById(R.id.allCoursesRecyclerView)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        createCourseButton = view.findViewById(R.id.createCourseButton)
    }

    private fun setupRecyclerViews() {
        // All Courses RecyclerView
        allCoursesAdapter = CourseAdapter(
            onCourseClick = { course ->
                // Handle course click - navigate to book a session with the tutor
                navigateToBookSession(course)
            },
            isTutor = isTutor,
            onEditCourse = { course ->
                // Handle edit course
                showEditCourseDialog(course)
            },
            onDeleteCourse = { course ->
                // Handle delete course
                showDeleteCourseConfirmation(course)
            }
        )
        allCoursesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        allCoursesRecyclerView.adapter = allCoursesAdapter
    }

    /**
     * Show dialog to edit a course
     */
    private fun showEditCourseDialog(course: Course) {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_course, null)

        // Get references to the input fields
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.courseTitle)
        val subtitleInput = dialogView.findViewById<TextInputEditText>(R.id.courseSubtitle)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.courseDescription)
        val categoryInput = dialogView.findViewById<TextInputEditText>(R.id.courseCategory)
        val priceInput = dialogView.findViewById<TextInputEditText>(R.id.coursePrice)

        // Pre-fill the fields with the course data
        titleInput.setText(course.title)
        subtitleInput.setText(course.subtitle)
        descriptionInput.setText(course.description)
        categoryInput.setText(course.category)
        priceInput.setText(course.averagePrice.toString())

        // Create and show the dialog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Course")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                // Get the input values
                val title = titleInput.text.toString().trim()
                val subtitle = subtitleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val category = categoryInput.text.toString().trim()
                val priceText = priceInput.text.toString().trim()

                // Validate inputs
                if (title.isEmpty() || subtitle.isEmpty() || description.isEmpty() || category.isEmpty() || priceText.isEmpty()) {
                    Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Parse price
                val price = try {
                    priceText.toDouble()
                } catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "Invalid price format", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Show loading indicator
                loadingProgressBar.visibility = View.VISIBLE

                // Create a CourseDTO object
                val courseDTO = com.mobile.data.model.CourseDTO(
                    id = course.id,
                    title = title,
                    subtitle = subtitle,
                    description = description,
                    category = category,
                    price = price
                )

                // Launch a coroutine to update the course
                lifecycleScope.launch {
                    try {
                        val result = com.mobile.utils.NetworkUtils.updateCourse(course.id, courseDTO)

                        withContext(Dispatchers.Main) {
                            loadingProgressBar.visibility = View.GONE

                            result.fold(
                                onSuccess = { updatedCourse ->
                                    // Show success dialog
                                    MaterialAlertDialogBuilder(requireContext())
                                        .setTitle("Success")
                                        .setMessage("Course \"${updatedCourse.title}\" has been updated successfully!")
                                        .setIcon(R.drawable.ic_check)
                                        .setPositiveButton("OK") { _, _ ->
                                            // Reload courses data
                                            loadCoursesData()
                                        }
                                        .show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to update course: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            loadingProgressBar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show confirmation dialog to delete a course
     */
    private fun showDeleteCourseConfirmation(course: Course) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete the course \"${course.title}\"? This action cannot be undone.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete") { _, _ ->
                // Show loading indicator
                loadingProgressBar.visibility = View.VISIBLE

                // Launch a coroutine to delete the course
                lifecycleScope.launch {
                    try {
                        val result = com.mobile.utils.NetworkUtils.deleteCourse(course.id)

                        withContext(Dispatchers.Main) {
                            loadingProgressBar.visibility = View.GONE

                            result.fold(
                                onSuccess = {
                                    // Show success dialog
                                    MaterialAlertDialogBuilder(requireContext())
                                        .setTitle("Success")
                                        .setMessage("Course \"${course.title}\" has been deleted successfully!")
                                        .setIcon(R.drawable.ic_check)
                                        .setPositiveButton("OK") { _, _ ->
                                            // Reload courses data
                                            loadCoursesData()
                                        }
                                        .show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to delete course: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            loadingProgressBar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupListeners() {
        // Search EditText
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchCourses(query)
                }
                return@setOnEditorActionListener true
            }
            false
        }

        // Category Chip Group
        // Using doOnCheckedChanged extension function to avoid deprecation warning
        categoryChipGroup.setOnCheckedChangeListener(object : ChipGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: ChipGroup, checkedId: Int) {
                when (checkedId) {
                    R.id.chipAll -> viewModel.filterCoursesByCategory(null)
                    R.id.chipMath -> viewModel.filterCoursesByCategory("Mathematics")
                    R.id.chipScience -> viewModel.filterCoursesByCategory("Science")
                    R.id.chipLanguage -> viewModel.filterCoursesByCategory("Languages")
                    R.id.chipProgramming -> viewModel.filterCoursesByCategory("Programming")
                }
            }
        })

        // Create Course Button (for tutors only)
        createCourseButton.setOnClickListener {
            showCreateCourseDialog()
        }
    }

    private fun showCreateCourseDialog() {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_course, null)

        // Create and show the dialog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create New Course")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                // Get references to the input fields
                val titleInput = dialogView.findViewById<TextInputEditText>(R.id.courseTitle)
                val subtitleInput = dialogView.findViewById<TextInputEditText>(R.id.courseSubtitle)
                val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.courseDescription)
                val categoryInput = dialogView.findViewById<TextInputEditText>(R.id.courseCategory)
                val priceInput = dialogView.findViewById<TextInputEditText>(R.id.coursePrice)

                // Get the input values
                val title = titleInput.text.toString().trim()
                val subtitle = subtitleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()
                val category = categoryInput.text.toString().trim()
                val priceText = priceInput.text.toString().trim()

                // Validate inputs
                if (title.isEmpty() || subtitle.isEmpty() || description.isEmpty() || category.isEmpty() || priceText.isEmpty()) {
                    Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Parse price
                val price = try {
                    priceText.toDouble()
                } catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "Invalid price format", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Get the tutor profile ID from preferences or user ID
                val userEmail = PreferenceUtils.getUserEmail(requireContext())
                if (userEmail == null) {
                    Toast.makeText(requireContext(), "User email not found", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Show loading indicator
                loadingProgressBar.visibility = View.VISIBLE

                // Create a CourseDTO object
                val courseDTO = com.mobile.data.model.CourseDTO(
                    title = title,
                    subtitle = subtitle,
                    description = description,
                    category = category,
                    price = price
                )

                // Launch a coroutine to create the course
                lifecycleScope.launch {
                    try {
                        // First, find the user by email
                        val userResult = com.mobile.utils.NetworkUtils.findUserByEmail(userEmail)

                        userResult.fold(
                            onSuccess = { user ->
                                // Then, find the tutor profile by user ID
                                val tutorResult = com.mobile.utils.NetworkUtils.findTutorByUserId(user.userId ?: 1L)

                                tutorResult.fold(
                                    onSuccess = { tutorProfile ->
                                        // Now create the course with the actual tutor ID
                                        val result = com.mobile.utils.NetworkUtils.createCourse(tutorProfile.id, courseDTO)

                                        withContext(Dispatchers.Main) {
                                            loadingProgressBar.visibility = View.GONE

                                            result.fold(
                                                onSuccess = { createdCourse ->
                                                    // Show success dialog
                                                    MaterialAlertDialogBuilder(requireContext())
                                                        .setTitle("Success")
                                                        .setMessage("Course \"${createdCourse.title}\" has been created successfully!")
                                                        .setIcon(R.drawable.ic_check)
                                                        .setPositiveButton("OK") { _, _ ->
                                                            // Reload courses data
                                                            loadCoursesData()
                                                        }
                                                        .show()
                                                },
                                                onFailure = { error ->
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Failed to create course: ${error.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    },
                                    onFailure = { error ->
                                        withContext(Dispatchers.Main) {
                                            loadingProgressBar.visibility = View.GONE
                                            Toast.makeText(
                                                requireContext(),
                                                "Failed to load tutor profile: ${error.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            },
                            onFailure = { error ->
                                withContext(Dispatchers.Main) {
                                    loadingProgressBar.visibility = View.GONE
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to find user: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            loadingProgressBar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupObservers() {
        viewModel.coursesState.observe(viewLifecycleOwner) { state ->
            // Update UI based on state
            loadingProgressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            if (state.error != null) {
                Toast.makeText(requireContext(), state.error, Toast.LENGTH_SHORT).show()
                emptyStateLayout.visibility = View.VISIBLE
            } else {
                emptyStateLayout.visibility = if (state.allCourses.isEmpty()) View.VISIBLE else View.GONE
            }

            // Update adapters with new data
            allCoursesAdapter.submitList(state.allCourses)
        }
    }

    private fun loadCoursesData() {
        if (isTutor) {
            // For tutors, load only their courses
            loadTutorCoursesWithEmail()
        } else {
            // For learners, load all courses
            viewModel.loadCourses()
        }
    }

    private fun loadTutorCoursesWithEmail() {
        val userEmail = PreferenceUtils.getUserEmail(requireContext())
        if (userEmail == null) {
            Toast.makeText(requireContext(), "User email not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        loadingProgressBar.visibility = View.VISIBLE

        // Launch a coroutine to get the tutor ID
        lifecycleScope.launch {
            try {
                // First, find the user by email
                val userResult = com.mobile.utils.NetworkUtils.findUserByEmail(userEmail)

                userResult.fold(
                    onSuccess = { user ->
                        // Then, find the tutor profile by user ID
                        val tutorResult = com.mobile.utils.NetworkUtils.findTutorByUserId(user.userId ?: 1L)

                        tutorResult.fold(
                            onSuccess = { tutorProfile ->
                                // Now load the courses with the actual tutor ID
                                viewModel.loadTutorCourses(tutorProfile.id)
                            },
                            onFailure = { error ->
                                withContext(Dispatchers.Main) {
                                    loadingProgressBar.visibility = View.GONE
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to load tutor profile: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Fallback to a default ID if needed
                                    viewModel.loadTutorCourses(1L)
                                }
                            }
                        )
                    },
                    onFailure = { error ->
                        withContext(Dispatchers.Main) {
                            loadingProgressBar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                "Failed to find user: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Fallback to a default ID if needed
                            viewModel.loadTutorCourses(1L)
                        }
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Fallback to a default ID if needed
                    viewModel.loadTutorCourses(1L)
                }
            }
        }
    }

    private fun navigateToBookSession(course: Course) {
        // Check if course ID is available
        if (course.id <= 0) {
            Toast.makeText(requireContext(), "Course information not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        loadingProgressBar.visibility = View.VISIBLE

        // Fetch tutor profile by course ID
        lifecycleScope.launch {
            try {
                val result = NetworkUtils.getTutorProfileByCourseId(course.id)

                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE

                    result.fold(
                        onSuccess = { tutorProfile: NetworkUtils.TutorProfile ->
                            // Create intent to BookingActivity with tutor ID and course info
                            val intent = Intent(requireContext(), BookingActivity::class.java).apply {
                                putExtra(BookingActivity.EXTRA_TUTOR_ID, tutorProfile.id)
                                putExtra(BookingActivity.EXTRA_COURSE_ID, course.id)
                                putExtra(BookingActivity.EXTRA_COURSE_TITLE, course.title)
                            }
                            startActivity(intent)
                        },
                        onFailure = { error: Throwable ->
                            // If fetching by course ID fails, fall back to using the tutorId from the course
                            if (course.tutorId != null) {
                                val intent = Intent(requireContext(), BookingActivity::class.java).apply {
                                    putExtra(BookingActivity.EXTRA_TUTOR_ID, course.tutorId)
                                    putExtra(BookingActivity.EXTRA_COURSE_ID, course.id)
                                    putExtra(BookingActivity.EXTRA_COURSE_TITLE, course.title)
                                }
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to load tutor profile: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Fall back to using the tutorId from the course
                    if (course.tutorId != null) {
                        val intent = Intent(requireContext(), BookingActivity::class.java).apply {
                            putExtra(BookingActivity.EXTRA_TUTOR_ID, course.tutorId)
                            putExtra(BookingActivity.EXTRA_COURSE_ID, course.id)
                            putExtra(BookingActivity.EXTRA_COURSE_TITLE, course.title)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance() = CoursesFragment()
    }
}
