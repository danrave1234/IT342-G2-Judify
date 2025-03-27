package com.mobile.ui.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.mobile.R
import com.mobile.ui.courses.adapters.CourseAdapter
import com.mobile.ui.search.TutorSearchActivity
import android.content.Intent
import android.widget.Toast

/**
 * Fragment for displaying courses/subjects
 */
class CoursesFragment : Fragment() {

    // UI Components
    private lateinit var searchEditText: EditText
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var popularCoursesRecyclerView: RecyclerView
    private lateinit var allCoursesRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var loadingProgressBar: ProgressBar

    // Adapters
    private lateinit var popularCoursesAdapter: CourseAdapter
    private lateinit var allCoursesAdapter: CourseAdapter

    // ViewModel
    private lateinit var viewModel: CoursesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_courses, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(CoursesViewModel::class.java)

        // Initialize UI components
        initializeViews(view)

        // Set up RecyclerViews
        setupRecyclerViews()

        // Set up listeners
        setupListeners()

        // Set up observers
        setupObservers()

        // Load courses data
        loadCoursesData()

        return view
    }

    private fun initializeViews(view: View) {
        searchEditText = view.findViewById(R.id.searchEditText)
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup)
        popularCoursesRecyclerView = view.findViewById(R.id.popularCoursesRecyclerView)
        allCoursesRecyclerView = view.findViewById(R.id.allCoursesRecyclerView)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
    }

    private fun setupRecyclerViews() {
        // Popular Courses RecyclerView
        popularCoursesAdapter = CourseAdapter { course ->
            // Handle course click - navigate to tutors for this course
            navigateToTutorSearch(course.title)
        }
        popularCoursesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        popularCoursesRecyclerView.adapter = popularCoursesAdapter

        // All Courses RecyclerView
        allCoursesAdapter = CourseAdapter { course ->
            // Handle course click - navigate to tutors for this course
            navigateToTutorSearch(course.title)
        }
        allCoursesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        allCoursesRecyclerView.adapter = allCoursesAdapter
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
        categoryChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipAll -> viewModel.filterCoursesByCategory(null)
                R.id.chipMath -> viewModel.filterCoursesByCategory("Mathematics")
                R.id.chipScience -> viewModel.filterCoursesByCategory("Science")
                R.id.chipLanguage -> viewModel.filterCoursesByCategory("Languages")
                R.id.chipProgramming -> viewModel.filterCoursesByCategory("Programming")
            }
        }
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
            popularCoursesAdapter.submitList(state.popularCourses)
            allCoursesAdapter.submitList(state.allCourses)
        }
    }

    private fun loadCoursesData() {
        viewModel.loadCourses()
    }

    private fun navigateToTutorSearch(subject: String) {
        val intent = Intent(requireContext(), TutorSearchActivity::class.java).apply {
            putExtra("SEARCH_QUERY", subject)
        }
        startActivity(intent)
    }

    companion object {
        fun newInstance() = CoursesFragment()
    }
}