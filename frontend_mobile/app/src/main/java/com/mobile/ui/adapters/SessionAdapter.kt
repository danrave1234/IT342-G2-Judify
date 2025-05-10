package com.mobile.ui.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mobile.R
import com.mobile.utils.NetworkUtils.TutoringSession
import com.mobile.utils.PreferenceUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Adapter for displaying session items in a RecyclerView
 */
class SessionAdapter : ListAdapter<TutoringSession, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    private var onVideoCallStarted: ((TutoringSession) -> Unit)? = null
    private var onSessionStarted: ((TutoringSession) -> Unit)? = null
    
    // List to store sessions
    private val sessionsList = mutableListOf<TutoringSession>()

    // Set listeners for actions
    fun setOnVideoCallStartedListener(listener: (TutoringSession) -> Unit) {
        onVideoCallStarted = listener
    }

    fun setOnSessionStartedListener(listener: (TutoringSession) -> Unit) {
        onSessionStarted = listener
    }
    
    /**
     * Update the sessions list and refresh the view
     * @param sessions New list of sessions to display
     */
    fun updateSessions(sessions: List<TutoringSession>) {
        // Clear existing sessions
        sessionsList.clear()
        // Add all new sessions
        sessionsList.addAll(sessions)
        // Submit the list to the adapter
        submitList(sessionsList.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view, onVideoCallStarted, onSessionStarted)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = getItem(position)
        holder.bind(session)
    }

    class SessionViewHolder(
        view: View, 
        private val onVideoCallStarted: ((TutoringSession) -> Unit)?,
        private val onSessionStarted: ((TutoringSession) -> Unit)?
    ) : RecyclerView.ViewHolder(view) {
        private val subject: TextView = view.findViewById(R.id.sessionSubject)
        private val date: TextView = view.findViewById(R.id.sessionDate)
        private val time: TextView = view.findViewById(R.id.sessionTime)
        private val status: TextView = view.findViewById(R.id.sessionStatus)
        private val tutorName: TextView = view.findViewById(R.id.tutorName)
        private val sessionType: TextView = view.findViewById(R.id.sessionType)
        
        // Action buttons
        private val sessionActionsContainer: View = view.findViewById(R.id.sessionActionsContainer)
        private val btnStartVideoCall: MaterialButton = view.findViewById(R.id.btnStartVideoCall)
        private val btnViewMap: MaterialButton = view.findViewById(R.id.btnViewMap)
        private val btnStartSession: MaterialButton = view.findViewById(R.id.btnStartSession)

        // Date formatters for different possible server formats
        private val isoInputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            // Don't set timezone - treat server time as local time
        }
        private val sqlInputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            // Don't set timezone - treat server time as local time
        }
        private val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
            // Don't change timezone for display
        }
        private val outputTimeFormat = SimpleDateFormat("h:mm a", Locale.US).apply {
            // Don't change timezone for display
        }

        fun bind(session: TutoringSession) {
            // Set the subject
            subject.text = session.subject

            // Set the session type
            sessionType.text = session.sessionType

            // Set the tutor name
            tutorName.text = session.tutorName

            // Format and set the date
            try {
                // Try to parse with ISO format first, then SQL format if that fails
                var startDate = try {
                    isoInputDateFormat.parse(session.startTime)
                } catch (e: Exception) {
                    sqlInputDateFormat.parse(session.startTime)
                }

                var endDate = try {
                    isoInputDateFormat.parse(session.endTime)
                } catch (e: Exception) {
                    sqlInputDateFormat.parse(session.endTime)
                }

                if (startDate != null && endDate != null) {
                    date.text = outputDateFormat.format(startDate)
                    time.text = "${outputTimeFormat.format(startDate)} - ${
                        outputTimeFormat.format(endDate)
                    }"
                    
                    // Check if session is today and within time range to enable action buttons
                    setupActionButtons(session, startDate, endDate)
                } else {
                    date.text = "Unknown date"
                    time.text = "Unknown time"
                    
                    // Hide action buttons if date parsing failed
                    sessionActionsContainer.visibility = View.GONE
                }
            } catch (e: Exception) {
                date.text = "Unknown date"
                time.text = "Unknown time"
                
                // Hide action buttons if date parsing failed
                sessionActionsContainer.visibility = View.GONE
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
        
        private fun setupActionButtons(session: TutoringSession, startDate: Date, endDate: Date) {
            val context = itemView.context
            val userRole = PreferenceUtils.getUserRole(context) ?: "STUDENT"
            val currentTime = System.currentTimeMillis()
            
            // Check if the session is scheduled or in progress
            val isActiveSession = session.status.equals("scheduled", ignoreCase = true) || 
                                  session.status.equals("in progress", ignoreCase = true)
            
            // Check if the session is happening now (current time between start and end time)
            val isSessionTimeValid = currentTime >= startDate.time && currentTime <= endDate.time
            
            // For testing purposes - allow session to be started 15 minutes before
            val canStartEarly = currentTime >= (startDate.time - (15 * 60 * 1000))
            
            // Show action buttons only for active sessions near their time, and only for tutors
            if (isActiveSession && canStartEarly && userRole.equals("TUTOR", ignoreCase = true)) {
                sessionActionsContainer.visibility = View.VISIBLE
                
                // Show appropriate buttons based on session type
                if (session.sessionType.equals("Online", ignoreCase = true)) {
                    btnStartVideoCall.visibility = View.VISIBLE
                    btnViewMap.visibility = View.GONE
                } else if (session.sessionType.equals("In-Person", ignoreCase = true) || 
                           session.sessionType.equals("Face to Face", ignoreCase = true)) {
                    btnStartVideoCall.visibility = View.GONE
                    
                    // Show map button only if location data is available
                    if (session.latitude != null && session.longitude != null) {
                        btnViewMap.visibility = View.VISIBLE
                    } else if (!session.locationData.isNullOrBlank()) {
                        // Try to parse legacy location data
                        btnViewMap.visibility = View.VISIBLE
                    } else {
                        btnViewMap.visibility = View.GONE
                    }
                }
                
                // Always show the start session button for any active session
                btnStartSession.visibility = View.VISIBLE
                
                // Set up click listeners for the buttons
                setupButtonListeners(session, context)
            } else {
                // Hide all action buttons
                sessionActionsContainer.visibility = View.GONE
            }
        }
        
        private fun setupButtonListeners(session: TutoringSession, context: Context) {
            // Start video call button
            btnStartVideoCall.setOnClickListener {
                if (onVideoCallStarted != null) {
                    // Use the callback if provided
                    onVideoCallStarted?.invoke(session)
                } else {
                    // Default implementation: launch video call activity
                    try {
                        val intent = Intent(context, Class.forName("com.mobile.ui.videocall.VideoCallActivity"))
                        intent.putExtra("SESSION_ID", session.id)
                        intent.putExtra("IS_HOST", true)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Video call feature not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            // View map button
            btnViewMap.setOnClickListener {
                // Try to get coordinates from the session
                var latitude = session.latitude
                var longitude = session.longitude
                
                // If not available, try to parse from locationData
                if (latitude == null || longitude == null) {
                    try {
                        // Try to extract location details from legacy format
                        val latPattern = "Lat:\\s*([0-9.-]+)".toRegex()
                        val longPattern = "Long:\\s*([0-9.-]+)".toRegex()
                        
                        val locationData = session.locationData ?: ""
                        val latMatch = latPattern.find(locationData)
                        val longMatch = longPattern.find(locationData)
                        
                        if (latMatch != null && longMatch != null) {
                            latitude = latMatch.groupValues[1].toDoubleOrNull()
                            longitude = longMatch.groupValues[1].toDoubleOrNull()
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors
                    }
                }
                
                // Open in Google Maps if coordinates are available
                if (latitude != null && longitude != null) {
                    val locationName = session.locationName ?: "Session Location"
                    val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($locationName)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    
                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(mapIntent)
                    } else {
                        // Fallback to browser if Maps app is not available
                        val browserIntentUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                        val browserIntent = Intent(Intent.ACTION_VIEW, browserIntentUri)
                        context.startActivity(browserIntent)
                    }
                } else {
                    Toast.makeText(context, "Location information not available", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Start session button
            btnStartSession.setOnClickListener {
                // Use the callback if provided
                if (onSessionStarted != null) {
                    onSessionStarted?.invoke(session)
                } else {
                    // Default implementation: update session status to "in progress"
                    Toast.makeText(context, "Session started", Toast.LENGTH_SHORT).show()
                    
                    // Ideally we would call an API to update the session status here
                    // For now, we'll just show a message
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
