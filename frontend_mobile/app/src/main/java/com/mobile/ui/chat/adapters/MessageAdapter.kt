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
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying messages in a RecyclerView
 */
class MessageAdapter(
    private val currentUserId: Long
) : ListAdapter<NetworkUtils.Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
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
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)

        fun bind(message: NetworkUtils.Message) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val profileImage: CircleImageView = itemView.findViewById(R.id.profileImage)

        fun bind(message: NetworkUtils.Message) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)
            
            // Set profile image (in a real app, this would be loaded from a URL)
            profileImage.setImageResource(R.drawable.default_profile)
        }
    }
    
    private fun formatTime(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(timestamp)
            
            val outputFormat = if (isToday(date)) {
                SimpleDateFormat("h:mm a", Locale.getDefault())
            } else {
                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            }
            
            date?.let { outputFormat.format(it) } ?: timestamp
        } catch (e: Exception) {
            // If parsing fails, just return the raw timestamp
            timestamp
        }
    }
    
    private fun isToday(date: Date?): Boolean {
        if (date == null) return false
        
        val calendar1 = java.util.Calendar.getInstance()
        calendar1.time = date
        
        val calendar2 = java.util.Calendar.getInstance()
        
        return calendar1.get(java.util.Calendar.YEAR) == calendar2.get(java.util.Calendar.YEAR) &&
               calendar1.get(java.util.Calendar.DAY_OF_YEAR) == calendar2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<NetworkUtils.Message>() {
        override fun areItemsTheSame(oldItem: NetworkUtils.Message, newItem: NetworkUtils.Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NetworkUtils.Message, newItem: NetworkUtils.Message): Boolean {
            return oldItem == newItem
        }
    }
} 