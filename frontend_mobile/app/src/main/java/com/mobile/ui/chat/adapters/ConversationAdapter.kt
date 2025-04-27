package com.mobile.ui.chat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying conversations in a RecyclerView
 */
class ConversationAdapter(
    private val onConversationClicked: (NetworkUtils.Conversation) -> Unit
) : ListAdapter<NetworkUtils.Conversation, ConversationAdapter.ConversationViewHolder>(ConversationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = getItem(position)
        holder.bind(conversation)
    }

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.nameText)
        private val lastMessageText: TextView = itemView.findViewById(R.id.lastMessageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onConversationClicked(getItem(position))
                }
            }
        }

        fun bind(conversation: NetworkUtils.Conversation) {
            // Get the current user ID from preferences
            val currentUserId = itemView.context?.let { PreferenceUtils.getUserId(it) } ?: -1L
            val userRole = itemView.context?.let { PreferenceUtils.getUserRole(it) } ?: "LEARNER"

            // Determine which user name to display based on the user's role
            val otherUserName = when (userRole) {
                "TUTOR" -> {
                    // If user is a tutor, show student's name
                    conversation.studentName.ifEmpty { "Unknown Student" }
                }
                "LEARNER" -> {
                    // If user is a student, show tutor's name
                    conversation.tutorName.ifEmpty { "Unknown Tutor" }
                }
                else -> {
                    // Fallback to the old logic if role is unknown
                    if (conversation.studentId == currentUserId) {
                        conversation.tutorName.ifEmpty { "Unknown Tutor" }
                    } else if (conversation.tutorId == currentUserId) {
                        conversation.studentName.ifEmpty { "Unknown Student" }
                    } else {
                        "Conversation ${conversation.id}"
                    }
                }
            }

            // Set the name text to the other user's name
            nameText.text = otherUserName

            // Set last message preview
            lastMessageText.text = conversation.lastMessage ?: "Tap to view messages"

            // Format and set time
            conversation.lastMessageTime?.let { timeString ->
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = inputFormat.parse(timeString)

                    val outputFormat = if (isToday(date)) {
                        SimpleDateFormat("h:mm a", Locale.getDefault())
                    } else {
                        SimpleDateFormat("MMM d", Locale.getDefault())
                    }

                    timeText.text = date?.let { outputFormat.format(it) } ?: ""
                } catch (e: Exception) {
                    timeText.text = timeString
                }
            } ?: run {
                val createdAtFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                try {
                    val date = createdAtFormat.parse(conversation.createdAt)
                    val outputFormat = if (isToday(date)) {
                        SimpleDateFormat("h:mm a", Locale.getDefault())
                    } else {
                        SimpleDateFormat("MMM d", Locale.getDefault())
                    }
                    timeText.text = date?.let { outputFormat.format(it) } ?: ""
                } catch (e: Exception) {
                    timeText.text = conversation.createdAt
                }
            }

            // Show unread indicator if there are unread messages
            unreadIndicator.visibility = if (conversation.unreadCount > 0) View.VISIBLE else View.GONE
        }

        private fun isToday(date: Date?): Boolean {
            if (date == null) return false

            val calendar1 = Calendar.getInstance()
            calendar1.time = date

            val calendar2 = Calendar.getInstance()

            return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                   calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
        }
    }

    class ConversationDiffCallback : DiffUtil.ItemCallback<NetworkUtils.Conversation>() {
        override fun areItemsTheSame(oldItem: NetworkUtils.Conversation, newItem: NetworkUtils.Conversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NetworkUtils.Conversation, newItem: NetworkUtils.Conversation): Boolean {
            return oldItem == newItem
        }
    }
}
