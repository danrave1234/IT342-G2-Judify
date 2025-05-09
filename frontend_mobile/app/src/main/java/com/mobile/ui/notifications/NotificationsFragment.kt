package com.mobile.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mobile.R
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import com.mobile.service.NotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mobile.utils.UiUtils

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var errorStateLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var retryButton: MaterialButton
    private lateinit var markAllAsReadButton: FloatingActionButton
    private lateinit var adapter: NotificationAdapter
    private var notifications = mutableListOf<NetworkUtils.Notification>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.notificationsRecyclerView)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        errorStateLayout = view.findViewById(R.id.errorStateLayout)
        progressBar = view.findViewById(R.id.progressBar)
        retryButton = view.findViewById(R.id.retryButton)
        markAllAsReadButton = view.findViewById(R.id.markAllAsReadButton)

        // Set up RecyclerView
        adapter = NotificationAdapter(notifications) { notification ->
            // Handle notification click
            markNotificationAsRead(notification.id)
            // Navigate based on notification type
            handleNotificationClick(notification)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Set up retry button
        retryButton.setOnClickListener {
            loadNotifications()
        }

        // Set up mark all as read button
        markAllAsReadButton.setOnClickListener {
            markAllNotificationsAsRead()
        }

        // Load notifications
        loadNotifications()
    }

    private fun loadNotifications() {
        showLoading()

        val userId = PreferenceUtils.getUserId(requireContext()) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                NotificationManager.getNotificationsForUser(userId)
            }

            if (result.isSuccess) {
                val fetchedNotifications = result.getOrNull() ?: emptyList()
                notifications.clear()
                notifications.addAll(fetchedNotifications)
                adapter.notifyDataSetChanged()

                // Cache notifications for offline access
                NotificationManager.cacheNotifications(requireContext(), fetchedNotifications)

                if (notifications.isEmpty()) {
                    showEmptyState()
                } else {
                    showContent()
                }
            } else {
                // Try to load cached notifications if network request fails
                val cachedNotifications = NotificationManager.getCachedNotifications(requireContext())
                if (cachedNotifications.isNotEmpty()) {
                    notifications.clear()
                    notifications.addAll(cachedNotifications)
                    adapter.notifyDataSetChanged()
                    showContent()
                    UiUtils.showInfoSnackbar(requireView(), "Showing cached notifications")
                } else {
                    showErrorState()
                    UiUtils.showErrorSnackbar(requireView(), "Failed to load notifications")
                }
            }
        }
    }

    private fun markNotificationAsRead(notificationId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                NotificationManager.markNotificationAsRead(notificationId)
            }

            if (result.isSuccess) {
                // Update the notification in the list
                val index = notifications.indexOfFirst { it.id == notificationId }
                if (index != -1) {
                    notifications[index] = notifications[index].copy(isRead = true)
                    adapter.notifyItemChanged(index)
                }
            } else {
                UiUtils.showErrorSnackbar(requireView(), "Failed to mark notification as read")
            }
        }
    }

    private fun markAllNotificationsAsRead() {
        val userId = PreferenceUtils.getUserId(requireContext()) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                NotificationManager.markAllNotificationsAsRead(userId)
            }

            if (result.isSuccess) {
                // Update all notifications in the list
                for (i in notifications.indices) {
                    notifications[i] = notifications[i].copy(isRead = true)
                }
                adapter.notifyDataSetChanged()
                UiUtils.showSuccessSnackbar(requireView(), "All notifications marked as read")
            } else {
                UiUtils.showErrorSnackbar(requireView(), "Failed to mark all notifications as read")
            }
        }
    }

    private fun handleNotificationClick(notification: NetworkUtils.Notification) {
        // Handle different notification types with appropriate messages
        when (notification.type) {
            "SESSION_REMINDER" -> {
                // Show session reminder details
                UiUtils.showSnackbar(requireView(), "Session Reminder: ${notification.content}")

                // Log the action
                Log.d("NotificationsFragment", "Clicked on session reminder notification: ${notification.id}")
            }
            "NEW_MESSAGE" -> {
                // Show message notification details
                UiUtils.showSnackbar(requireView(), "New Message: ${notification.content}")

                // Log the action
                Log.d("NotificationsFragment", "Clicked on message notification: ${notification.id}")
            }
            "PAYMENT_CONFIRMATION" -> {
                // Show payment confirmation details
                UiUtils.showSnackbar(requireView(), "Payment Confirmation: ${notification.content}")

                // Log the action
                Log.d("NotificationsFragment", "Clicked on payment notification: ${notification.id}")
            }
            "BOOKING_CONFIRMATION" -> {
                // Show booking confirmation details
                UiUtils.showSnackbar(requireView(), "Booking Confirmation: ${notification.content}")

                // Log the action
                Log.d("NotificationsFragment", "Clicked on booking confirmation notification: ${notification.id}")
            }
            "BOOKING_CANCELLATION" -> {
                // Show booking cancellation details
                UiUtils.showSnackbar(requireView(), "Booking Cancellation: ${notification.content}")

                // Log the action
                Log.d("NotificationsFragment", "Clicked on booking cancellation notification: ${notification.id}")
            }
            "REVIEW_RECEIVED" -> {
                // Show review details
                UiUtils.showSnackbar(requireView(), "Review Received: ${notification.content}")

                // Log the action
                Log.d("NotificationsFragment", "Clicked on review notification: ${notification.id}")
            }
            else -> {
                // Handle unknown notification types
                UiUtils.showSnackbar(requireView(), "Notification: ${notification.content}")

                // Log the action
                Log.d("NotificationsFragment", "Clicked on unknown notification type: ${notification.type}")
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        errorStateLayout.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
        errorStateLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        errorStateLayout.visibility = View.GONE
    }

    private fun showErrorState() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        errorStateLayout.visibility = View.VISIBLE
    }
}
