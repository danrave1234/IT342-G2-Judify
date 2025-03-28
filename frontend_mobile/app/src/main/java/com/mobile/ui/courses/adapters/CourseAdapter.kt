package com.mobile.ui.courses.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.ui.courses.models.Course
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter for displaying courses in a RecyclerView
 */
class CourseAdapter(
    private val onCourseClick: (Course) -> Unit,
    private val isTutor: Boolean = false,
    private val onEditCourse: ((Course) -> Unit)? = null,
    private val onDeleteCourse: ((Course) -> Unit)? = null
) : ListAdapter<Course, RecyclerView.ViewHolder>(CourseDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_LEARNER = 0
        private const val VIEW_TYPE_TUTOR = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (isTutor) VIEW_TYPE_TUTOR else VIEW_TYPE_LEARNER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TUTOR -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_tutor_course, parent, false)
                TutorCourseViewHolder(view, onCourseClick, onEditCourse, onDeleteCourse)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_course, parent, false)
                LearnerCourseViewHolder(view, onCourseClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val course = getItem(position)
        when (holder) {
            is TutorCourseViewHolder -> holder.bind(course)
            is LearnerCourseViewHolder -> holder.bind(course)
        }
    }

    /**
     * ViewHolder for course items viewed by learners
     */
    class LearnerCourseViewHolder(
        itemView: View,
        private val onCourseClick: (Course) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val courseIcon: ImageView = itemView.findViewById(R.id.courseIcon)
        private val courseTitle: TextView = itemView.findViewById(R.id.courseTitle)
        private val courseSubtitle: TextView = itemView.findViewById(R.id.courseSubtitle)
        private val courseDescription: TextView = itemView.findViewById(R.id.courseDescription)
        private val tutorCount: TextView = itemView.findViewById(R.id.tutorCount)
        private val averageRating: TextView = itemView.findViewById(R.id.averageRating)
        private val averagePrice: TextView = itemView.findViewById(R.id.averagePrice)
        private val viewTutorsButton: Button = itemView.findViewById(R.id.viewTutorsButton)

        fun bind(course: Course) {
            // Set course data to views
            courseTitle.text = course.title
            courseSubtitle.text = course.subtitle
            courseDescription.text = course.description
            tutorCount.text = "${course.tutorCount} Tutors"
            averageRating.text = course.averageRating.toString()

            // Format price as currency
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            averagePrice.text = "${currencyFormat.format(course.averagePrice)}/hr"

            // Set course icon if available
            course.imageResId?.let {
                courseIcon.setImageResource(it)
            }

            // Set click listeners
            viewTutorsButton.setOnClickListener {
                onCourseClick(course)
            }

            itemView.setOnClickListener {
                onCourseClick(course)
            }
        }
    }

    /**
     * ViewHolder for course items viewed by tutors
     */
    class TutorCourseViewHolder(
        itemView: View,
        private val onCourseClick: (Course) -> Unit,
        private val onEditCourse: ((Course) -> Unit)?,
        private val onDeleteCourse: ((Course) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val courseIcon: ImageView = itemView.findViewById(R.id.courseIcon)
        private val courseTitle: TextView = itemView.findViewById(R.id.courseTitle)
        private val courseSubtitle: TextView = itemView.findViewById(R.id.courseSubtitle)
        private val courseDescription: TextView = itemView.findViewById(R.id.courseDescription)
        private val courseCategory: TextView = itemView.findViewById(R.id.courseCategory)
        private val coursePrice: TextView = itemView.findViewById(R.id.coursePrice)
        private val courseMenuButton: ImageButton = itemView.findViewById(R.id.courseMenuButton)
        private val editCourseButton: Button = itemView.findViewById(R.id.editCourseButton)
        private val deleteCourseButton: Button = itemView.findViewById(R.id.deleteCourseButton)

        fun bind(course: Course) {
            // Set course data to views
            courseTitle.text = course.title
            courseSubtitle.text = course.subtitle
            courseDescription.text = course.description
            courseCategory.text = course.category

            // Format price as currency
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
            coursePrice.text = "${currencyFormat.format(course.averagePrice)}/hr"

            // Set course icon if available
            course.imageResId?.let {
                courseIcon.setImageResource(it)
            }

            // Set click listeners
            editCourseButton.setOnClickListener {
                onEditCourse?.invoke(course)
            }

            deleteCourseButton.setOnClickListener {
                onDeleteCourse?.invoke(course)
            }

            courseMenuButton.setOnClickListener {
                // Show popup menu with edit/delete options
                // This is an alternative to the buttons below
            }

            itemView.setOnClickListener {
                onCourseClick(course)
            }
        }
    }

    /**
     * DiffUtil callback for efficient updates
     */
    class CourseDiffCallback : DiffUtil.ItemCallback<Course>() {
        override fun areItemsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Course, newItem: Course): Boolean {
            return oldItem == newItem
        }
    }
}
