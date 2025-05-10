package com.mobile.service

import android.content.Context
import android.util.Log
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager class for handling notifications
 */
class NotificationManager {
    companion object {
        private const val TAG = "NotificationManager"
        private const val PREF_LAST_NOTIFICATION_CHECK = "last_notification_check"
        private const val PREF_CACHED_NOTIFICATIONS = "cached_notifications"

        // Base URL for API calls
        private const val BASE_URL = "https://api.judify.com"

        /**
         * Get notifications for a user
         * @param userId The user ID
         * @return Result containing a list of notifications or an error
         */
        suspend fun getNotificationsForUser(userId: Long): Result<List<NetworkUtils.Notification>> {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Fetching notifications for user with ID: $userId")

                    // Make an API call to the backend to get notifications
                    val url = URL("$BASE_URL/notifications/user/$userId")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val responseCode = connection.responseCode
                    if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        val jsonArray = JSONArray(response.toString())
                        val notifications = mutableListOf<NetworkUtils.Notification>()

                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            notifications.add(
                                NetworkUtils.Notification(
                                    id = jsonObject.getLong("id"),
                                    userId = jsonObject.getLong("userId"),
                                    type = jsonObject.getString("type"),
                                    content = jsonObject.getString("content"),
                                    timestamp = jsonObject.getString("timestamp"),
                                    isRead = jsonObject.getBoolean("isRead")
                                )
                            )
                        }

                        Result.success(notifications)
                    } else {
                        val error = "Failed to fetch notifications: HTTP ${connection.responseCode}"
                        Log.e(TAG, error)
                        Result.failure(Exception(error))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception fetching notifications: ${e.message}", e)
                    Result.failure(e)
                }
            }
        }

        /**
         * Mark a notification as read
         * @param notificationId The notification ID
         * @return Result containing the updated notification or an error
         */
        suspend fun markNotificationAsRead(notificationId: Long): Result<NetworkUtils.Notification> {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Marking notification as read: $notificationId")

                    // Make an API call to the backend to mark the notification as read
                    val url = URL("$BASE_URL/notifications/$notificationId/read")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "PUT"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val responseCode = connection.responseCode
                    if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        val jsonObject = JSONObject(response.toString())
                        val notification = NetworkUtils.Notification(
                            id = jsonObject.getLong("id"),
                            userId = jsonObject.getLong("userId"),
                            type = jsonObject.getString("type"),
                            content = jsonObject.getString("content"),
                            timestamp = jsonObject.getString("timestamp"),
                            isRead = true
                        )

                        Result.success(notification)
                    } else {
                        val error = "Failed to mark notification as read: HTTP ${connection.responseCode}"
                        Log.e(TAG, error)
                        Result.failure(Exception(error))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception marking notification as read: ${e.message}", e)
                    Result.failure(e)
                }
            }
        }

        /**
         * Mark all notifications as read for a user
         * @param userId The user ID
         * @return Result containing Unit or an error
         */
        suspend fun markAllNotificationsAsRead(userId: Long): Result<Unit> {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "Marking all notifications as read for user: $userId")

                    // Make an API call to the backend to mark all notifications as read
                    val url = URL("$BASE_URL/notifications/user/$userId/read-all")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "PUT"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val responseCode = connection.responseCode
                    if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        Result.success(Unit)
                    } else {
                        val error = "Failed to mark all notifications as read: HTTP ${connection.responseCode}"
                        Log.e(TAG, error)
                        Result.failure(Exception(error))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception marking all notifications as read: ${e.message}", e)
                    Result.failure(e)
                }
            }
        }

        /**
         * Cache notifications locally
         * @param context The context
         * @param notifications The notifications to cache
         */
        fun cacheNotifications(context: Context, notifications: List<NetworkUtils.Notification>) {
            val jsonArray = JSONArray()
            notifications.forEach { notification ->
                val jsonObject = JSONObject().apply {
                    put("id", notification.id)
                    put("userId", notification.userId)
                    put("type", notification.type)
                    put("content", notification.content)
                    put("timestamp", notification.timestamp)
                    put("isRead", notification.isRead)
                }
                jsonArray.put(jsonObject)
            }

            PreferenceUtils.saveLong(context, PREF_LAST_NOTIFICATION_CHECK, System.currentTimeMillis())
            PreferenceUtils.saveString(context, PREF_CACHED_NOTIFICATIONS, jsonArray.toString())
        }

        /**
         * Get cached notifications
         * @param context The context
         * @return List of cached notifications
         */
        fun getCachedNotifications(context: Context): List<NetworkUtils.Notification> {
            val cachedJson = PreferenceUtils.getString(context, PREF_CACHED_NOTIFICATIONS, "[]")
            val notifications = mutableListOf<NetworkUtils.Notification>()

            try {
                val jsonArray = JSONArray(cachedJson as String)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    notifications.add(
                        NetworkUtils.Notification(
                            id = jsonObject.getLong("id"),
                            userId = jsonObject.getLong("userId"),
                            type = jsonObject.getString("type"),
                            content = jsonObject.getString("content"),
                            timestamp = jsonObject.getString("timestamp"),
                            isRead = jsonObject.getBoolean("isRead")
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception parsing cached notifications: ${e.message}", e)
            }

            return notifications
        }

    }
}
