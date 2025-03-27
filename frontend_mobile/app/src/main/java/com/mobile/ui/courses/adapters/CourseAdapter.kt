package com.mobile.ui.courses.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
class CourseAdapter(private val onCourseClick: (Course) -> Unit) : 
    ListAdapter<Course, CourseAdapter.CourseViewHolder>(CourseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view, onCourseClick)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for course items
     */
    class CourseViewHolder(
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