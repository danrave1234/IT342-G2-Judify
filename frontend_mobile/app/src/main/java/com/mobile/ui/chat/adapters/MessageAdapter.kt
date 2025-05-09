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

            // Check if this is a face-to-face session with location
            if (content.contains("Session Type: Face to Face", ignoreCase = true) || 
                content.contains("Session Type: In-Person", ignoreCase = true)) {

                // Try to extract location information
                val locationPattern = "location:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE)
                val locationMatch = locationPattern.find(content)

                if (locationMatch != null) {
                    // Extract the location
                    val location = locationMatch.groupValues[1].trim()

                    // Create a modified content with the location highlighted
                    val modifiedContent = if (!content.contains("Meeting Location:", ignoreCase = true)) {
                        // Add the location information if it's not already in the formatted message
                        val meetingLocationText = "\nMeeting Location: $location"
                        content + meetingLocationText
                    } else {
                        content
                    }

                    messageText.text = modifiedContent
                } else {
                    messageText.text = content
                }
            } else {
                messageText.text = content
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

            // Check if this is a face-to-face session with location
            if (content.contains("Session Type: Face to Face", ignoreCase = true) || 
                content.contains("Session Type: In-Person", ignoreCase = true)) {

                // Try to extract location information
                val locationPattern = "location:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE)
                val locationMatch = locationPattern.find(content)

                if (locationMatch != null) {
                    // Extract the location
                    val location = locationMatch.groupValues[1].trim()

                    // Create a modified content with the location highlighted
                    val modifiedContent = if (!content.contains("Meeting Location:", ignoreCase = true)) {
                        // Add the location information if it's not already in the formatted message
                        val meetingLocationText = "\nMeeting Location: $location"
                        content + meetingLocationText
                    } else {
                        content
                    }

                    messageText.text = modifiedContent
                } else {
                    messageText.text = content
                }
            } else {
                messageText.text = content
            }

            timeText.text = formatTime(message.timestamp)

            // Get user role for logging only
            val userRole = com.mobile.utils.PreferenceUtils.getUserRole(itemView.context)

            // Log visibility state for debugging
            android.util.Log.d("MessageAdapter", "User role: $userRole")
            android.util.Log.d("MessageAdapter", "Message: ${message.content}, sessionId: ${message.sessionId}")

            // Always make buttons visible regardless of role
            android.util.Log.d("MessageAdapter", "Always showing tutor action buttons")
            tutorActionButtonsLayout.visibility = View.VISIBLE

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

    inner class TutorSessionApprovalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sessionDetailsText: TextView = itemView.findViewById(R.id.sessionDetailsText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val approveButton: Button = itemView.findViewById(R.id.approveButton)
        private val rejectButton: Button = itemView.findViewById(R.id.rejectButton)

        fun bind(message: Message) {
            // Parse the message content to extract session details
            val content = message.content

            // Check if this is a face-to-face session with location
            if (content.contains("Session Type: Face to Face", ignoreCase = true) || 
                content.contains("Session Type: In-Person", ignoreCase = true)) {

                // Try to extract location information
                val locationPattern = "location:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE)
                val locationMatch = locationPattern.find(content)

                if (locationMatch != null) {
                    // Extract the location
                    val location = locationMatch.groupValues[1].trim()

                    // Create a modified content with the location highlighted
                    val modifiedContent = if (!content.contains("Meeting Location:", ignoreCase = true)) {
                        // Add the location information if it's not already in the formatted message
                        val meetingLocationText = "\nMeeting Location: $location"
                        content + meetingLocationText
                    } else {
                        content
                    }

                    sessionDetailsText.text = modifiedContent
                } else {
                    sessionDetailsText.text = content
                }
            } else {
                sessionDetailsText.text = content
            }

            timeText.text = formatTime(message.timestamp)

            // Log that we're showing the tutor approval buttons
            android.util.Log.d("MessageAdapter", "Binding tutor session approval view for message: ${message.content}")
            android.util.Log.d("MessageAdapter", "Session ID: ${message.sessionId}")

            // Get the current context
            val context = itemView.context

            // Check if the user is a tutor
            val userRole = com.mobile.utils.PreferenceUtils.getUserRole(context)
            android.util.Log.d("MessageAdapter", "User role: $userRole")

            // Make sure buttons are visible
            approveButton.visibility = View.VISIBLE
            rejectButton.visibility = View.VISIBLE

            // Make buttons more noticeable
            approveButton.textSize = 18f
            rejectButton.textSize = 18f

            // Set button colors
            approveButton.setBackgroundColor(context.resources.getColor(R.color.success, null))
            rejectButton.setBackgroundColor(context.resources.getColor(R.color.error, null))

            // Add padding to buttons
            val paddingDp = 16
            val density = context.resources.displayMetrics.density
            val paddingPx = (paddingDp * density).toInt()
            approveButton.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            rejectButton.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            // Add margin between buttons
            val layoutParams = approveButton.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            layoutParams?.setMargins(0, 0, 0, paddingPx)
            approveButton.layoutParams = layoutParams

            // Set up buttons with sessionId
            message.sessionId?.let { sessionId ->
                approveButton.setOnClickListener {
                    android.util.Log.d("MessageAdapter", "TutorApproval: Approve button clicked for sessionId: $sessionId")
                    onSessionApprove?.invoke(sessionId)
                }

                rejectButton.setOnClickListener {
                    android.util.Log.d("MessageAdapter", "TutorApproval: Reject button clicked for sessionId: $sessionId")
                    onSessionReject?.invoke(sessionId)
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
