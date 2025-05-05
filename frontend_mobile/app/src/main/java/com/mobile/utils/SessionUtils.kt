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

    /**
     * Get session details by conversation ID
     * @param conversationId The ID of the conversation
     * @return Result containing the session details or an error
     */
    suspend fun getSessionByConversationId(conversationId: Long): Result<NetworkUtils.TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://judify-backend.onrender.com/api/tutoring-sessions/findByConversation/$conversationId")
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
                        conversationId = if (jsonObject.has("conversationId") && !jsonObject.isNull("conversationId")) jsonObject.getLong("conversationId") else null
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
                val url = URL("https://judify-backend.onrender.com/api/tutoring-sessions/acceptSession/$sessionId")
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
                        conversationId = if (jsonObject.has("conversationId") && !jsonObject.isNull("conversationId")) jsonObject.getLong("conversationId") else null
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
                val url = URL("https://judify-backend.onrender.com/api/tutoring-sessions/rejectSession/$sessionId")
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
                        conversationId = if (jsonObject.has("conversationId") && !jsonObject.isNull("conversationId")) jsonObject.getLong("conversationId") else null
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
     * @param location Optional location for the session
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

                // Use hardcoded URL
                val url = URL("https://judify-backend.onrender.com/api/tutoring-sessions/createSession")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                // Create the request body
                val jsonObject = JSONObject().apply {
                    put("tutorId", tutorId)
                    put("studentId", studentIdLong)
                    put("startTime", startTime)
                    put("endTime", endTime)
                    put("status", "PENDING") // Status is now PENDING until tutor accepts
                    put("subject", subject)
                    put("sessionType", sessionType)
                    notes.let { put("notes", it) }
                    if (location.isNotEmpty() && sessionType == "In-Person") {
                        put("locationData", location)
                    }
                }

                // Write the request body
                val outputStream = connection.outputStream
                outputStream.write(jsonObject.toString().toByteArray())
                outputStream.close()

                // Get the response
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val reader = connection.inputStream.bufferedReader()
                    val response = reader.readText()
                    reader.close()

                    // Parse the response
                    val responseJson = JSONObject(response)

                    val session = NetworkUtils.TutoringSession(
                        id = responseJson.optLong("sessionId", responseJson.optLong("id")),
                        tutorId = responseJson.optLong("tutorId"),
                        studentId = responseJson.optString("studentId"),
                        startTime = responseJson.optString("startTime"),
                        endTime = responseJson.optString("endTime"),
                        status = responseJson.optString("status"),
                        subject = responseJson.optString("subject"),
                        sessionType = responseJson.optString("sessionType"),
                        notes = responseJson.optString("notes", ""),
                        tutorName = responseJson.optString("tutorName", ""),
                        studentName = responseJson.optString("studentName", ""),
                        conversationId = responseJson.optLong("conversationId")
                    )

                    return@withContext Result.success(session)
                } else {
                    val errorStream = connection.errorStream
                    val errorResponse = errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    errorStream?.close()

                    Log.e(TAG, "Error creating session: $responseCode - $errorResponse")
                    return@withContext Result.failure(Exception("Error creating session: $responseCode - $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating session: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }
    }
}
