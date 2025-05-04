package com.mobile.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.utils.NetworkUtils
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: List<NetworkUtils.Notification>,
    private val onNotificationClick: (NetworkUtils.Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.bind(notification)
    }

    override fun getItemCount(): Int = notifications.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val notificationIcon: ImageView = itemView.findViewById(R.id.notificationIcon)
        private val notificationTypeText: TextView = itemView.findViewById(R.id.notificationTypeText)
        private val notificationContentText: TextView = itemView.findViewById(R.id.notificationContentText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)

        fun bind(notification: NetworkUtils.Notification) {
            // Set notification type
            val typeText = when (notification.type) {
                "SESSION_REMINDER" -> "Session Reminder"
                "NEW_MESSAGE" -> "New Message"
                "PAYMENT_CONFIRMATION" -> "Payment Confirmation"
                "BOOKING_CONFIRMATION" -> "Booking Confirmation"
                "BOOKING_CANCELLATION" -> "Booking Cancellation"
                "REVIEW_RECEIVED" -> "Review Received"
                else -> notification.type
            }
            notificationTypeText.text = typeText

            // Set notification content
            notificationContentText.text = notification.content

            // Set notification time
            try {
                val date = dateFormat.parse(notification.timestamp)
                timeText.text = date?.let { displayFormat.format(it) } ?: notification.timestamp
            } catch (e: Exception) {
                timeText.text = notification.timestamp
            }

            // Set notification icon based on type
            val iconResource = when (notification.type) {
                "SESSION_REMINDER" -> R.drawable.ic_calendar
                "NEW_MESSAGE" -> R.drawable.ic_message
                "PAYMENT_CONFIRMATION" -> R.drawable.ic_check
                "BOOKING_CONFIRMATION" -> R.drawable.ic_check
                "BOOKING_CANCELLATION" -> R.drawable.ic_notification
                "REVIEW_RECEIVED" -> R.drawable.ic_person
                else -> R.drawable.ic_notification
            }
            notificationIcon.setImageResource(iconResource)

            // Show/hide unread indicator
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

            // Set click listener
            itemView.setOnClickListener {
                onNotificationClick(notification)
            }
        }
    }
}
