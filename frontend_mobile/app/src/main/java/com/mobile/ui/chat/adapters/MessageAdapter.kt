package com.mobile.ui.chat.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.model.Message
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying messages in a RecyclerView
 */
class MessageAdapter(
    private val currentUserId: Long,
    private val onSessionApprove: ((Long) -> Unit)? = null,
    private val onSessionReject: ((Long) -> Unit)? = null
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    // Context property
    private var currentContext: Context? = null

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val VIEW_TYPE_SESSION_DETAILS_SENT = 3
        private const val VIEW_TYPE_SESSION_DETAILS_RECEIVED = 4
        private const val VIEW_TYPE_SESSION_ACTION_SENT = 5
        private const val VIEW_TYPE_SESSION_ACTION_RECEIVED = 6
        private const val VIEW_TYPE_TUTOR_SESSION_APPROVAL = 7
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        currentContext = recyclerView.context
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        currentContext = null
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)

        // Add debug logging for all messages
        android.util.Log.d("MessageAdapter", "Message ID: ${message.id}, Type: ${message.messageType}, SessionID: ${message.sessionId}, Content: ${message.content.take(50)}...")

        // Check if this is a session details message
        if (message.messageType == Message.MessageType.SESSION_DETAILS) {
            android.util.Log.d("MessageAdapter", "Found SESSION_DETAILS message: ${message.id}")

            // First try to get context from currentContext
            val context = currentContext ?: return if (message.senderId == currentUserId) {
                VIEW_TYPE_SESSION_DETAILS_SENT
            } else {
                VIEW_TYPE_SESSION_DETAILS_RECEIVED
            }

            try {
                val userRole = com.mobile.utils.PreferenceUtils.getUserRole(context)
                android.util.Log.d("MessageAdapter", "Message type: ${message.messageType}, User role: $userRole")

                // If user is a tutor, always show the tutor approval view for session details messages
                // regardless of who sent the message
                if (userRole == "TUTOR") {
                    android.util.Log.d("MessageAdapter", "Using TUTOR_SESSION_APPROVAL view type")
                    return VIEW_TYPE_TUTOR_SESSION_APPROVAL
                }
            } catch (e: Exception) {
                android.util.Log.e("MessageAdapter", "Error getting user role: ${e.message}", e)
            }
        }

        return when (message.messageType) {
            Message.MessageType.SESSION_DETAILS -> {
                if (message.senderId == currentUserId) {
                    VIEW_TYPE_SESSION_DETAILS_SENT
                } else {
                    VIEW_TYPE_SESSION_DETAILS_RECEIVED
                }
            }
            Message.MessageType.SESSION_ACTION -> {
                // Check if this is a session approval message
                if (message.content.contains("approved") || message.content.contains("scheduled") || 
                    message.content.contains("accepted")) {
                    // For approval messages, always show as received if the current user is a student
                    // and as sent if the current user is a tutor
                    val context = currentContext
                    if (context != null) {
                        try {
                            val userRole = com.mobile.utils.PreferenceUtils.getUserRole(context)
                            android.util.Log.d("MessageAdapter", "Session action message: ${message.content}, User role: $userRole")

                            if (userRole == "TUTOR") {
                                // If user is a tutor, show approval messages as sent
                                return VIEW_TYPE_SESSION_ACTION_SENT
                            } else {
                                // If user is a student, show approval messages as received
                                return VIEW_TYPE_SESSION_ACTION_RECEIVED
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MessageAdapter", "Error getting user role: ${e.message}", e)
                        }
                    }
                }

                // For other session action messages, use the default logic
                if (message.senderId == currentUserId) {
                    VIEW_TYPE_SESSION_ACTION_SENT
                } else {
                    VIEW_TYPE_SESSION_ACTION_RECEIVED
                }
            }
            else -> {
                if (message.senderId == currentUserId) {
                    VIEW_TYPE_SENT
                } else {
                    VIEW_TYPE_RECEIVED
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Make sure we have a valid context
        currentContext = parent.context

        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            VIEW_TYPE_SESSION_DETAILS_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_session_details_sent, parent, false)
                SessionDetailsSentViewHolder(view)
            }
            VIEW_TYPE_SESSION_DETAILS_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_session_details_received, parent, false)
                SessionDetailsReceivedViewHolder(view)
            }
            VIEW_TYPE_SESSION_ACTION_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_session_action_sent, parent, false)
                SessionActionSentViewHolder(view)
            }
            VIEW_TYPE_SESSION_ACTION_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_session_action_received, parent, false)
                SessionActionReceivedViewHolder(view)
            }
            VIEW_TYPE_TUTOR_SESSION_APPROVAL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_tutor_session_approval, parent, false)
                TutorSessionApprovalViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is SessionDetailsSentViewHolder -> holder.bind(message)
            is SessionDetailsReceivedViewHolder -> holder.bind(message)
            is SessionActionSentViewHolder -> holder.bind(message)
            is SessionActionReceivedViewHolder -> holder.bind(message)
            is TutorSessionApprovalViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)

        fun bind(message: Message) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)

        fun bind(message: Message) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)
        }
    }

    inner class SessionDetailsSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)

        fun bind(message: Message) {
            // Parse the message content to extract session details
            val content = message.content
            
            // Filter out any "Tutor Profile ID" information that might be in the message
            val filteredContent = content.replace(Regex("Tutor Profile ID:.*?(\\n|$)"), "")
                                        .trim()

            // Check if this is a face-to-face session with location
            if (filteredContent.contains("Session Type: Face to Face", ignoreCase = true) || 
                filteredContent.contains("Session Type: In-Person", ignoreCase = true)) {

                // Try to extract location information
                val locationPattern = "location:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE)
                val locationMatch = locationPattern.find(filteredContent)

                if (locationMatch != null) {
                    // Extract the location
                    val location = locationMatch.groupValues[1].trim()

                    // Create a modified content with the location highlighted
                    val modifiedContent = if (!filteredContent.contains("Meeting Location:", ignoreCase = true)) {
                        // Add the location information if it's not already in the formatted message
                        val meetingLocationText = "\nMeeting Location: $location"
                        filteredContent + meetingLocationText
                    } else {
                        filteredContent
                    }

                    messageText.text = modifiedContent
                } else {
                    messageText.text = filteredContent
                }
            } else {
                messageText.text = filteredContent
            }

            timeText.text = formatTime(message.timestamp)
        }
    }

    inner class SessionDetailsReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val tutorActionButtonsLayout: LinearLayout = itemView.findViewById(R.id.tutorActionButtonsLayout)
        private val approveButton: Button = itemView.findViewById(R.id.approveButton)
        private val rejectButton: Button = itemView.findViewById(R.id.rejectButton)

        fun bind(message: Message) {
            // Parse the message content to extract session details
            val content = message.content
            
            // Filter out any "Tutor Profile ID" information that might be in the message
            val filteredContent = content.replace(Regex("Tutor Profile ID:.*?(\\n|$)"), "")
                                        .trim()

            // Check if this is a face-to-face session with location
            if (filteredContent.contains("Session Type: Face to Face", ignoreCase = true) || 
                filteredContent.contains("Session Type: In-Person", ignoreCase = true)) {

                // Try to extract location information
                val locationPattern = "location:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE)
                val locationMatch = locationPattern.find(filteredContent)

                if (locationMatch != null) {
                    // Extract the location
                    val location = locationMatch.groupValues[1].trim()

                    // Create a modified content with the location highlighted
                    val modifiedContent = if (!filteredContent.contains("Meeting Location:", ignoreCase = true)) {
                        // Add the location information if it's not already in the formatted message
                        val meetingLocationText = "\nMeeting Location: $location"
                        filteredContent + meetingLocationText
                    } else {
                        filteredContent
                    }

                    messageText.text = modifiedContent
                } else {
                    messageText.text = filteredContent
                }
            } else {
                messageText.text = filteredContent
            }

            timeText.text = formatTime(message.timestamp)

            // Get user role for logging only
            val userRole = com.mobile.utils.PreferenceUtils.getUserRole(itemView.context)

            // Log visibility state for debugging
            android.util.Log.d("MessageAdapter", "User role: $userRole")
            android.util.Log.d("MessageAdapter", "Message: ${message.content}, sessionId: ${message.sessionId}")

            // Check if the session has already been approved or rejected based on message content
            val isApproved = filteredContent.contains("Status: APPROVED", ignoreCase = true) ||
                            filteredContent.contains("has been approved", ignoreCase = true)
            
            val isCancelled = filteredContent.contains("Status: CANCELLED", ignoreCase = true) ||
                             filteredContent.contains("Status: REJECTED", ignoreCase = true) ||
                             filteredContent.contains("has been cancelled", ignoreCase = true) ||
                             filteredContent.contains("has been rejected", ignoreCase = true)

            // Hide buttons if session is already approved or cancelled/rejected
            if (isApproved || isCancelled) {
                android.util.Log.d("MessageAdapter", "Session already ${if (isApproved) "approved" else "cancelled/rejected"}, hiding buttons")
                tutorActionButtonsLayout.visibility = View.GONE
            } else {
                // Always make buttons visible for pending sessions
                android.util.Log.d("MessageAdapter", "Session is pending, showing tutor action buttons")
                tutorActionButtonsLayout.visibility = View.VISIBLE
                
                // Update button text for consistency
                approveButton.text = "ACCEPT"
                rejectButton.text = "DECLINE"

                // Set up buttons with sessionId
                message.sessionId?.let { sessionId ->
                    android.util.Log.d("MessageAdapter", "Setting up button click listeners for sessionId: $sessionId")

                    approveButton.setOnClickListener {
                        android.util.Log.d("MessageAdapter", "Approve button clicked for sessionId: $sessionId")
                        onSessionApprove?.invoke(sessionId)
                    }

                    rejectButton.setOnClickListener {
                        android.util.Log.d("MessageAdapter", "Reject button clicked for sessionId: $sessionId")
                        onSessionReject?.invoke(sessionId)
                    }
                } ?: run {
                    android.util.Log.d("MessageAdapter", "No sessionId available for this message")
                }
            }
        }
    }

    inner class TutorSessionApprovalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sessionDetailsText: TextView = itemView.findViewById(R.id.sessionDetailsText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val approveButton: Button = itemView.findViewById(R.id.approveButton)
        private val rejectButton: Button = itemView.findViewById(R.id.rejectButton)

        fun bind(message: Message) {
            // Parse the message content to extract session details
            val content = message.content

            // Improved regex to filter out any mention of tutor profile ID, with any case and any characters in between
            val filteredContent = content.replace(Regex("(?i)tutor\\s*profile\\s*i[d|:]+.*?(\\n|$)"), "")
                                        .trim()
            
            // More aggressive cleanup - remove empty lines that might be left after filtering
            val cleanedContent = filteredContent.replace(Regex("\\n\\s*\\n"), "\n")
                                              .trim()

            // Log the message details for debugging
            android.util.Log.d("MessageAdapter", "Binding tutor session approval message: ${message.id}")
            android.util.Log.d("MessageAdapter", "Original message content: $content")
            android.util.Log.d("MessageAdapter", "Filtered message content: $cleanedContent")
            android.util.Log.d("MessageAdapter", "Message sessionId property: ${message.sessionId}")

            // IMPORTANT: Check if sessionId is correct from the backend database
            // Try to extract the session ID directly from the message content
            var sessionId = message.sessionId
            var foundNewId = false
            
            // First check for "Session ID: X" pattern
            val sessionIdPattern = "Session ID:\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE)
            val sessionIdMatch = sessionIdPattern.find(content)
            
            if (sessionIdMatch != null) {
                val extractedId = sessionIdMatch.groupValues[1].toLongOrNull()
                if (extractedId != null && extractedId > 0) {
                    sessionId = extractedId
                    foundNewId = true
                    android.util.Log.d("MessageAdapter", "Found Session ID in content: $sessionId")
                }
            }
            
            // If that fails, check for "Session #X" pattern
            if (!foundNewId) {
                val sessionHashPattern = "[Ss]ession\\s*#(\\d+)".toRegex()
                val sessionHashMatch = sessionHashPattern.find(content)
                
                if (sessionHashMatch != null) {
                    val extractedId = sessionHashMatch.groupValues[1].toLongOrNull()
                    if (extractedId != null && extractedId > 0) {
                        sessionId = extractedId
                        foundNewId = true
                        android.util.Log.d("MessageAdapter", "Found Session # in content: $sessionId")
                    }
                }
            }
            
            // If that fails, look for any numeric ID after "ID:" 
            if (!foundNewId) {
                val idPattern = "ID:\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE)
                val idMatch = idPattern.find(content)
                
                if (idMatch != null) {
                    val extractedId = idMatch.groupValues[1].toLongOrNull()
                    if (extractedId != null && extractedId > 0) {
                        sessionId = extractedId
                        foundNewId = true
                        android.util.Log.d("MessageAdapter", "Found ID: in content: $sessionId")
                    }
                }
            }
            
            // If all else fails, try to find any number after "session" 
            if (!foundNewId) {
                val generalSessionPattern = "[Ss]ession.*?(\\d+)".toRegex()
                val generalMatch = generalSessionPattern.find(content)
                
                if (generalMatch != null) {
                    val extractedId = generalMatch.groupValues[1].toLongOrNull()
                    if (extractedId != null && extractedId > 0) {
                        sessionId = extractedId
                        foundNewId = true
                        android.util.Log.d("MessageAdapter", "Found general session number in content: $sessionId")
                    }
                }
            }
            
            // If we still couldn't find a session ID, try looking for the ID from the subject
            if (!foundNewId) {
                val subjectPattern = "Subject:.*?\\b(\\d+)\\b".toRegex()
                val subjectMatch = subjectPattern.find(content)
                
                if (subjectMatch != null) {
                    val extractedId = subjectMatch.groupValues[1].toLongOrNull()
                    if (extractedId != null && extractedId > 0) {
                        sessionId = extractedId
                        foundNewId = true
                        android.util.Log.d("MessageAdapter", "Found possible ID in subject: $sessionId")
                    }
                }
            }
            
            // Check if this is a face-to-face session with location
            if (cleanedContent.contains("Session Type: Face to Face", ignoreCase = true) || 
                cleanedContent.contains("Session Type: In-Person", ignoreCase = true)) {

                // Try to extract location information
                val locationPattern = "location:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE)
                val locationMatch = locationPattern.find(cleanedContent)

                if (locationMatch != null) {
                    // Extract the location
                    val location = locationMatch.groupValues[1].trim()

                    // Create a modified content with the location highlighted
                    val modifiedContent = if (!cleanedContent.contains("Meeting Location:", ignoreCase = true)) {
                        // Add the location information if it's not already in the formatted message
                        val meetingLocationText = "\nMeeting Location: $location"
                        cleanedContent + meetingLocationText
                    } else {
                        cleanedContent
                    }

                    sessionDetailsText.text = modifiedContent
                } else {
                    sessionDetailsText.text = cleanedContent
                }
            } else {
                sessionDetailsText.text = cleanedContent
            }

            timeText.text = formatTime(message.timestamp)

            // Check if the session has already been approved or rejected based on message content
            val isApproved = cleanedContent.contains("Status: APPROVED", ignoreCase = true) ||
                            cleanedContent.contains("has been approved", ignoreCase = true)
            
            val isRejected = cleanedContent.contains("Status: CANCELLED", ignoreCase = true) ||
                            cleanedContent.contains("Status: REJECTED", ignoreCase = true) ||
                            cleanedContent.contains("has been cancelled", ignoreCase = true) ||
                            cleanedContent.contains("has been rejected", ignoreCase = true)

            // Check session status from message content
            if (isApproved || isRejected) {
                // Session already approved or rejected, hide the buttons
                android.util.Log.d("MessageAdapter", "Session already ${if (isApproved) "approved" else "rejected"}, hiding action buttons")
                approveButton.visibility = View.GONE
                rejectButton.visibility = View.GONE
            } else {
                // Log that we're showing the tutor approval buttons
                android.util.Log.d("MessageAdapter", "Showing approve/reject buttons for sessionId: $sessionId")

                // Make sure buttons are visible
                approveButton.visibility = View.VISIBLE
                rejectButton.visibility = View.VISIBLE

                // Change button text to be clearer
                approveButton.text = "ACCEPT"
                rejectButton.text = "DECLINE"

                // Ensure the buttons have proper styling
                try {
                    approveButton.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Green
                    rejectButton.setBackgroundColor(android.graphics.Color.parseColor("#F44336")) // Red
                } catch (e: Exception) {
                    android.util.Log.e("MessageAdapter", "Error setting button colors: ${e.message}")
                }
            }

            // Set up buttons with sessionId
            val finalSessionId = sessionId
            if (finalSessionId != null && finalSessionId > 0) {
                android.util.Log.d("MessageAdapter", "Setting click listeners with sessionId: $finalSessionId")
                
                approveButton.setOnClickListener {
                    android.util.Log.d("MessageAdapter", "Approve button clicked with sessionId: $finalSessionId")
                    onSessionApprove?.invoke(finalSessionId)
                }

                rejectButton.setOnClickListener {
                    android.util.Log.d("MessageAdapter", "Reject button clicked with sessionId: $finalSessionId")
                    onSessionReject?.invoke(finalSessionId)
                }
            } else {
                android.util.Log.e("MessageAdapter", "No valid sessionId found for message: ${message.id}")
                
                // Disable buttons if no valid session ID
                approveButton.isEnabled = false
                rejectButton.isEnabled = false
                
                // Add click listeners that show error toast
                approveButton.setOnClickListener {
                    android.util.Log.e("MessageAdapter", "Approve button clicked but no sessionId available")
                    // Try to get context to show a toast
                    val context = itemView.context
                    if (context != null) {
                        android.widget.Toast.makeText(context, "Error: Invalid session ID", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                
                rejectButton.setOnClickListener {
                    android.util.Log.e("MessageAdapter", "Reject button clicked but no sessionId available")
                    // Try to get context to show a toast
                    val context = itemView.context
                    if (context != null) {
                        android.widget.Toast.makeText(context, "Error: Invalid session ID", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    inner class SessionActionSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)

        fun bind(message: Message) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)
        }
    }

    inner class SessionActionReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)

        fun bind(message: Message) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val messageDate = Date(timestamp)
        val calendar = Calendar.getInstance()

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        calendar.time = messageDate

        // Create formatters with the device's time zone to ensure correct local time display
        val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateTimeFormatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

        return if (calendar.timeInMillis >= today.timeInMillis) {
            // Message is from today, show only time
            timeFormatter.format(messageDate)
        } else {
            // Message is from a different day, show date and time
            dateTimeFormatter.format(messageDate)
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.content == newItem.content && 
                   oldItem.timestamp == newItem.timestamp &&
                   oldItem.senderId == newItem.senderId &&
                   oldItem.readStatus == newItem.readStatus &&
                   oldItem.messageType == newItem.messageType
        }
    }
}
