package com.mobile.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for session-related operations
 */
object SessionUtils {

    private const val TAG = "SessionUtils"

    // Use the same URLs as NetworkUtils
    private const val PRODUCTION_BACKEND_URL = "https://judify-795422705086.asia-east1.run.app"
    private const val DEVELOPMENT_BACKEND_URL = "http://192.168.86.107:8080" // Match NetworkUtils URL
    private const val IS_DEVELOPMENT_MODE = true
    private val BACKEND_URL = if (IS_DEVELOPMENT_MODE) DEVELOPMENT_BACKEND_URL else PRODUCTION_BACKEND_URL

    // Constants for session time checking
    private const val SESSION_UPCOMING_THRESHOLD_MINUTES = 15 // Notify 15 minutes before session starts

    /**
     * Get session details by conversation ID
     * @param conversationId The ID of the conversation
     * @return Result containing the session details or an error
     */
    suspend fun getSessionByConversationId(conversationId: Long): Result<NetworkUtils.TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BACKEND_URL/api/tutoring-sessions/findByConversation/$conversationId")
                Log.d(TAG, "Fetching session details from URL: $url")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                NetworkUtils.processResponse(connection) { response ->
                    val jsonObject = JSONObject(response)

                    NetworkUtils.TutoringSession(
                        id = jsonObject.getLong("sessionId"),
                        tutorId = jsonObject.getLong("tutorId"),
                        studentId = jsonObject.getString("studentId"),
                        startTime = jsonObject.getString("startTime"),
                        endTime = jsonObject.getString("endTime"),
                        status = jsonObject.getString("status"),
                        subject = jsonObject.getString("subject"),
                        sessionType = jsonObject.optString("sessionType", "Online"),
                        notes = if (jsonObject.has("notes") && !jsonObject.isNull("notes")) jsonObject.getString("notes") else null,
                        tutorName = jsonObject.optString("tutorName", ""),
                        studentName = jsonObject.optString("studentName", ""),
                        conversationId = if (jsonObject.has("conversationId") && !jsonObject.isNull("conversationId")) jsonObject.getLong("conversationId") else null,
                        price = if (jsonObject.has("price") && !jsonObject.isNull("price")) jsonObject.getDouble("price") else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting session by conversation ID: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Accept a session
     * @param sessionId The ID of the session to accept
     * @return Result containing the updated session details or an error
     */
    suspend fun acceptSession(sessionId: Long): Result<NetworkUtils.TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BACKEND_URL/api/tutoring-sessions/acceptSession/$sessionId")
                Log.d(TAG, "Accepting session from URL: $url")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                NetworkUtils.processResponse(connection) { response ->
                    val jsonObject = JSONObject(response)

                    NetworkUtils.TutoringSession(
                        id = jsonObject.getLong("sessionId"),
                        tutorId = jsonObject.getLong("tutorId"),
                        studentId = jsonObject.getString("studentId"),
                        startTime = jsonObject.getString("startTime"),
                        endTime = jsonObject.getString("endTime"),
                        status = jsonObject.getString("status"),
                        subject = jsonObject.getString("subject"),
                        sessionType = jsonObject.optString("sessionType", "Online"),
                        notes = if (jsonObject.has("notes") && !jsonObject.isNull("notes")) jsonObject.getString("notes") else null,
                        tutorName = jsonObject.optString("tutorName", ""),
                        studentName = jsonObject.optString("studentName", ""),
                        conversationId = if (jsonObject.has("conversationId") && !jsonObject.isNull("conversationId")) jsonObject.getLong("conversationId") else null,
                        price = if (jsonObject.has("price") && !jsonObject.isNull("price")) jsonObject.getDouble("price") else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting session: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Reject a session
     * @param sessionId The ID of the session to reject
     * @return Result containing the updated session details or an error
     */
    suspend fun rejectSession(sessionId: Long): Result<NetworkUtils.TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BACKEND_URL/api/tutoring-sessions/rejectSession/$sessionId")
                Log.d(TAG, "Rejecting session from URL: $url")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                NetworkUtils.processResponse(connection) { response ->
                    val jsonObject = JSONObject(response)

                    NetworkUtils.TutoringSession(
                        id = jsonObject.getLong("sessionId"),
                        tutorId = jsonObject.getLong("tutorId"),
                        studentId = jsonObject.getString("studentId"),
                        startTime = jsonObject.getString("startTime"),
                        endTime = jsonObject.getString("endTime"),
                        status = jsonObject.getString("status"),
                        subject = jsonObject.getString("subject"),
                        sessionType = jsonObject.optString("sessionType", "Online"),
                        notes = if (jsonObject.has("notes") && !jsonObject.isNull("notes")) jsonObject.getString("notes") else null,
                        tutorName = jsonObject.optString("tutorName", ""),
                        studentName = jsonObject.optString("studentName", ""),
                        conversationId = if (jsonObject.has("conversationId") && !jsonObject.isNull("conversationId")) jsonObject.getLong("conversationId") else null,
                        price = if (jsonObject.has("price") && !jsonObject.isNull("price")) jsonObject.getDouble("price") else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting session: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Format a date string for display
     * @param dateString The date string in ISO format
     * @return Formatted date string
     */
    fun formatDateForDisplay(dateString: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString) ?: return dateString
            return outputFormat.format(date)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: ${e.message}", e)
            return dateString
        }
    }

    /**
     * Format a time string for display
     * @param startTimeString The start time string in ISO format
     * @param endTimeString The end time string in ISO format
     * @return Formatted time range string
     */
    fun formatTimeForDisplay(startTimeString: String, endTimeString: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

            val startDate = inputFormat.parse(startTimeString) ?: return "$startTimeString - $endTimeString"
            val endDate = inputFormat.parse(endTimeString) ?: return "$startTimeString - $endTimeString"

            val formattedStartTime = outputFormat.format(startDate)
            val formattedEndTime = outputFormat.format(endDate)

            return "$formattedStartTime - $formattedEndTime"
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting time: ${e.message}", e)
            return "$startTimeString - $endTimeString"
        }
    }

    /**
     * Create a new tutoring session
     * @param studentId Student ID
     * @param tutorId Tutor ID
     * @param startTime Start time of the session (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param endTime End time of the session (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param subject Subject for the session
     * @param sessionType Type of session (e.g., "Online", "In-Person")
     * @param location Optional location for the session (legacy format)
     * @param latitude Optional latitude for in-person sessions
     * @param longitude Optional longitude for in-person sessions
     * @param locationName Optional location name for in-person sessions
     * @param notes Additional notes for the session
     * @return Result containing the created TutoringSession
     */
    suspend fun createSession(
        studentId: String,
        tutorId: Long,
        startTime: String,
        endTime: String,
        subject: String,
        sessionType: String,
        location: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        notes: String
    ): Result<NetworkUtils.TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating session: student=$studentId, tutor=$tutorId, subject=$subject")

                // Convert studentId to Long
                val studentIdLong = studentId.toLongOrNull() ?: -1L
                if (studentIdLong == -1L) {
                    throw IllegalArgumentException("Invalid student ID: $studentId")
                }

                // Parse location string if provided but no specific coordinates
                var parsedLatitude = latitude
                var parsedLongitude = longitude
                var parsedLocationName = locationName

                // If we have a legacy location string but no specific coordinates, try to parse it
                if (location.isNotEmpty() && sessionType == "In-Person" && latitude == null && longitude == null) {
                    // Try to parse the location string (expected format: "Lat: X.X, Long: Y.Y, Name: Z")
                    try {
                        val parts = location.split(",")
                        if (parts.size >= 2) {
                            // Extract latitude
                            val latPart = parts[0].trim()
                            if (latPart.startsWith("Lat:")) {
                                parsedLatitude = latPart.substring(4).trim().toDoubleOrNull()
                            }

                            // Extract longitude
                            val longPart = parts[1].trim()
                            if (longPart.startsWith("Long:")) {
                                parsedLongitude = longPart.substring(5).trim().toDoubleOrNull()
                            }

                            // Extract name if available
                            if (parts.size >= 3) {
                                val namePart = parts[2].trim()
                                if (namePart.startsWith("Name:")) {
                                    parsedLocationName = namePart.substring(5).trim()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing location string: ${e.message}", e)
                        // If parsing fails, just use the original string as locationName
                        parsedLocationName = location
                    }
                }

                // Use NetworkUtils API instead of creating our own connection
                return@withContext NetworkUtils.createTutoringSession(
                    tutorId = tutorId,
                    studentId = studentIdLong,
                    startTime = startTime,
                    endTime = endTime,
                    subject = subject,
                    sessionType = sessionType,
                    notes = notes,
                    latitude = parsedLatitude,
                    longitude = parsedLongitude,
                    locationName = parsedLocationName,
                    locationData = if (location.isNotEmpty() && sessionType == "In-Person") {
                        location
                    } else {
                        null
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating session: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }
    }

    /**
     * Check if a session is about to start
     * @param session The session to check
     * @return true if the session is about to start (within the threshold), false otherwise
     */
    fun isSessionAboutToStart(session: NetworkUtils.TutoringSession): Boolean {
        try {
            // Only check scheduled sessions
            if (session.status.uppercase() != "SCHEDULED") {
                return false
            }

            // Parse the start time
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val startDate = inputFormat.parse(session.startTime) ?: return false

            // Get current time
            val currentTime = Calendar.getInstance().time

            // Calculate time difference in minutes
            val diffInMillis = startDate.time - currentTime.time
            val diffInMinutes = diffInMillis / (1000 * 60)

            // Check if the session is about to start (within threshold)
            return diffInMinutes in 0..SESSION_UPCOMING_THRESHOLD_MINUTES
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if session is about to start: ${e.message}", e)
            return false
        }
    }

    /**
     * Check if a session has started
     * @param session The session to check
     * @return true if the session has started, false otherwise
     */
    fun hasSessionStarted(session: NetworkUtils.TutoringSession): Boolean {
        try {
            // Only check scheduled sessions
            if (session.status.uppercase() != "SCHEDULED") {
                return false
            }

            // Parse the start time
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val startDate = inputFormat.parse(session.startTime) ?: return false

            // Get current time
            val currentTime = Calendar.getInstance().time

            // Check if current time is after or equal to start time
            return currentTime.time >= startDate.time
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if session has started: ${e.message}", e)
            return false
        }
    }
}
