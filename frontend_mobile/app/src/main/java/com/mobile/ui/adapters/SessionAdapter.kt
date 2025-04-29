package com.mobile.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.utils.NetworkUtils.TutoringSession
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying session items in a RecyclerView
 */
class SessionAdapter : ListAdapter<TutoringSession, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = getItem(position)
        holder.bind(session)
    }

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val subject: TextView = view.findViewById(R.id.sessionSubject)
        private val date: TextView = view.findViewById(R.id.sessionDate)
        private val time: TextView = view.findViewById(R.id.sessionTime)
        private val status: TextView = view.findViewById(R.id.sessionStatus)
        private val tutorName: TextView = view.findViewById(R.id.tutorName)
        
        // Date formatters
        private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        private val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        private val outputTimeFormat = SimpleDateFormat("h:mm a", Locale.US)

        fun bind(session: TutoringSession) {
            // Set the subject
            subject.text = session.subject
            
            // Set the tutor name
            tutorName.text = session.tutorName
            
            // Format and set the date
            try {
                val startDate = inputDateFormat.parse(session.startTime)
                if (startDate != null) {
                    date.text = outputDateFormat.format(startDate)
                    time.text = "${outputTimeFormat.format(startDate)} - ${
                        outputTimeFormat.format(inputDateFormat.parse(session.endTime)!!)
                    }"
                } else {
                    date.text = "Unknown date"
                    time.text = "Unknown time"
                }
            } catch (e: Exception) {
                date.text = "Unknown date"
                time.text = "Unknown time"
            }
            
            // Set the status with appropriate styling
            status.text = session.status.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            
            // Set status background color based on status
            when (session.status.lowercase(Locale.getDefault())) {
                "scheduled" -> {
                    status.setBackgroundResource(R.drawable.status_background_scheduled)
                }
                "completed" -> {
                    status.setBackgroundResource(R.drawable.status_background_completed)
                }
                "cancelled" -> {
                    status.setBackgroundResource(R.drawable.status_background_cancelled)
                }
                "in progress" -> {
                    status.setBackgroundResource(R.drawable.status_background_in_progress)
                }
                else -> {
                    status.setBackgroundResource(R.drawable.status_background)
                }
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<TutoringSession>() {
        override fun areItemsTheSame(oldItem: TutoringSession, newItem: TutoringSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TutoringSession, newItem: TutoringSession): Boolean {
            return oldItem == newItem
        }
    }
} 