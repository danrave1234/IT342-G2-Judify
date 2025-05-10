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
                val url = URL("$BACKEND_URL/api/tutoring-sessions/updateStatus/$sessionId")
                Log.d(TAG, "Accepting session - making API request to URL: $url")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000 // 15 seconds timeout
                connection.readTimeout = 15000 // 15 seconds read timeout

                // Explicitly connect to make sure we can trace network errors
                connection.connect()
                
                Log.d(TAG, "Connected to server to accept session $sessionId")
                
                // Write the updated status to the output stream
                val outputStream = connection.outputStream
                outputStream.write("\"APPROVED\"".toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                Log.d(TAG, "Accept session response code: $responseCode")

                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Accept session raw response: $response")
                    
                    try {
                        val jsonObject = JSONObject(response)
                        
                        // Parse the session details from the response
                        val session = NetworkUtils.TutoringSession(
                            id = jsonObject.optLong("sessionId", jsonObject.optLong("id")),
                            tutorId = jsonObject.optLong("tutorId", jsonObject.optLong("userId")),
                            studentId = jsonObject.optString("studentId"),
                            startTime = jsonObject.optString("startTime"),
                            endTime = jsonObject.optString("endTime"),
                            status = jsonObject.optString("status"),
                            subject = jsonObject.optString("subject"),
                            sessionType = jsonObject.optString("sessionType", "Online"),
                            notes = if (jsonObject.has("notes") && !jsonObject.isNull("notes")) jsonObject.getString("notes") else null,
                            tutorName = jsonObject.optString("tutorName", ""),
                            studentName = jsonObject.optString("studentName", ""),
                            conversationId = if (jsonObject.has("conversationId") && !jsonObject.isNull("conversationId")) jsonObject.getLong("conversationId") else null,
                            price = if (jsonObject.has("price") && !jsonObject.isNull("price")) jsonObject.getDouble("price") else null,
                            latitude = if (jsonObject.has("latitude") && !jsonObject.isNull("latitude")) jsonObject.getDouble("latitude") else null,
                            longitude = if (jsonObject.has("longitude") && !jsonObject.isNull("longitude")) jsonObject.getDouble("longitude") else null,
                            locationName = jsonObject.optString("locationName", null),
                            locationData = jsonObject.optString("locationData", null)
                        )
                        
                        Log.d(TAG, "Session accepted successfully: ID=${session.id}, status=${session.status}")
                        
                        // Also update session status in NetworkUtils to ensure consistency
                        try {
                            NetworkUtils.updateSessionStatus(session.id, session.status)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating session status cache: ${e.message}")
                            // Continue anyway since we already have the correct data
                        }
                        
                        Result.success(session)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing accept session response: ${e.message}", e)
                        Result.failure(e)
                    }
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e(TAG, "Error accepting session, HTTP $responseCode: $errorBody")
                    Result.failure(Exception("HTTP Error: $responseCode - $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception accepting session: ${e.message}", e)
                // Log detailed exception information for network errors
                if (e is java.net.SocketTimeoutException) {
                    Log.e(TAG, "Network timeout when accepting session")
                } else if (e is java.net.UnknownHostException) {
                    Log.e(TAG, "Unknown host - check network connection")
                } else if (e is java.io.IOException) {
                    Log.e(TAG, "IO Exception: ${e.message}")
                }
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
                val url = URL("$BACKEND_URL/api/tutoring-sessions/updateStatus/$sessionId")
                Log.d(TAG, "Rejecting session - making API request to URL: $url")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000 // 15 seconds timeout
                connection.readTimeout = 15000 // 15 seconds read timeout

                // Explicitly connect to make sure we can trace network errors
                connection.connect()
                
                Log.d(TAG, "Connected to server to reject session $sessionId")
                
                // Write the updated status to the output stream
                val outputStream = connection.outputStream
                outputStream.write("\"CANCELLED\"".toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                Log.d(TAG, "Reject session response code: $responseCode")

                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Reject session raw response: $response")
                    
                    try {
                        val jsonObject = JSONObject(response)
                        
                        // Parse the session details from the response
                        val session = NetworkUtils.TutoringSession(
                            id = jsonObject.optLong("sessionId", jsonObject.optLong("id")),
                            tutorId = jsonObject.optLong("tutorId", jsonObject.optLong("userId")),
                            studentId = jsonObject.optString("studentId"),
                            startTime = jsonObject.optString("startTime"),
                            endTime = jsonObject.optString("endTime"),
                            status = jsonObject.optString("status"),
                            subject = jsonObject.optString("subject"),
                            sessionType = jsonObject.optString("sessionType", "Online"),
                            notes = if (jsonObject.has("notes") && !jsonObject.isNull("notes")) jsonObject.getString("notes") else null,
                            tutorName = jsonObject.optString("tutorName", ""),
                            studentName = jsonObject.optString("studentName", ""),
                            conversationId = if (jsonObject.has("conversationId") && !jsonObject.isNull("conversationId")) jsonObject.getLong("conversationId") else null,
                            price = if (jsonObject.has("price") && !jsonObject.isNull("price")) jsonObject.getDouble("price") else null,
                            latitude = if (jsonObject.has("latitude") && !jsonObject.isNull("latitude")) jsonObject.getDouble("latitude") else null,
                            longitude = if (jsonObject.has("longitude") && !jsonObject.isNull("longitude")) jsonObject.getDouble("longitude") else null,
                            locationName = jsonObject.optString("locationName", null),
                            locationData = jsonObject.optString("locationData", null)
                        )
                        
                        Log.d(TAG, "Session rejected successfully: ID=${session.id}, status=${session.status}")
                        
                        // Also update session status in NetworkUtils to ensure consistency
                        try {
                            NetworkUtils.updateSessionStatus(session.id, session.status)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating session status cache: ${e.message}")
                            // Continue anyway since we already have the correct data
                        }
                        
                        Result.success(session)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing reject session response: ${e.message}", e)
                        Result.failure(e)
                    }
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e(TAG, "Error rejecting session, HTTP $responseCode: $errorBody")
                    Result.failure(Exception("HTTP Error: $responseCode - $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception rejecting session: ${e.message}", e)
                // Log detailed exception information for network errors
                if (e is java.net.SocketTimeoutException) {
                    Log.e(TAG, "Network timeout when rejecting session")
                } else if (e is java.net.UnknownHostException) {
                    Log.e(TAG, "Unknown host - check network connection")
                } else if (e is java.io.IOException) {
                    Log.e(TAG, "IO Exception: ${e.message}")
                }
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
     * @param tutorId Tutor profile ID (not user ID)
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
                
                // First, get the userId associated with the tutorId
                val tutorUserIdResult = NetworkUtils.getUserIdFromTutorId(tutorId)
                
                // Create a mutable copy of notes that we can modify
                var finalNotes = notes
                
                // Variable to store the ID we'll actually use for session creation
                var finalTutorId = tutorId
                
                if (tutorUserIdResult.isFailure) {
                    Log.e(TAG, "Failed to get userId from tutorId: ${tutorUserIdResult.exceptionOrNull()?.message}")
                    // We'll try to make another direct call to get the tutor data
                    val tutorResult = NetworkUtils.findTutorById(tutorId)
                    
                    if (tutorResult.isSuccess) {
                        val tutor = tutorResult.getOrNull()
                        if (tutor != null && tutor.userId != null) {
                            finalTutorId = tutor.userId
                            Log.d(TAG, "Successfully got userId ${finalTutorId} from direct tutor profile call")
                        } else {
                            Log.e(TAG, "No userId found in tutor profile data")
                            // Add to notes that we couldn't convert the ID
                            finalNotes = if (notes.isNotEmpty()) {
                                notes
                            } else {
                                ""
                            }
                        }
                    } else {
                        Log.d(TAG, "Proceeding with original tutorId: $tutorId")
                        // Keep original notes without adding tutorId information
                        finalNotes = notes
                    }
                } else {
                    val tutorUserId = tutorUserIdResult.getOrNull()
                    if (tutorUserId != null) {
                        Log.d(TAG, "Successfully converted tutorId=$tutorId to userId=$tutorUserId")
                        // Use the userId instead of the profile ID
                        finalTutorId = tutorUserId
                        // Keep the original notes without adding the tutorId reference
                        finalNotes = notes
                    }
                }

                // Parse location string if provided but no specific coordinates
                var parsedLatitude = latitude
                var parsedLongitude = longitude
                var parsedLocationName = locationName

                if (location.isNotEmpty() && sessionType == "In-Person" && 
                    (parsedLatitude == null || parsedLongitude == null || parsedLocationName.isNullOrEmpty())) {
                    // Try to extract location details from legacy format
                    try {
                        // Example format: "Lat: 12.345, Long: 67.890, Name: Location Name"
                        val latPattern = "Lat:\\s*([0-9.-]+)".toRegex()
                        val longPattern = "Long:\\s*([0-9.-]+)".toRegex()
                        val namePattern = "Name:\\s*(.+)$".toRegex()

                        val latMatch = latPattern.find(location)
                        val longMatch = longPattern.find(location)
                        val nameMatch = namePattern.find(location)

                        if (latMatch != null && parsedLatitude == null) {
                            parsedLatitude = latMatch.groupValues[1].toDoubleOrNull()
                        }

                        if (longMatch != null && parsedLongitude == null) {
                            parsedLongitude = longMatch.groupValues[1].toDoubleOrNull()
                        }

                        if (nameMatch != null && parsedLocationName.isNullOrEmpty()) {
                            parsedLocationName = nameMatch.groupValues[1].trim()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing location string: $location", e)
                    }
                }

                // Use NetworkUtils API for creating the tutoring session
                return@withContext NetworkUtils.createTutoringSession(
                    tutorId = finalTutorId, // Use the user ID if conversion was successful
                    studentId = studentIdLong,
                    startTime = startTime,
                    endTime = endTime,
                    subject = subject,
                    sessionType = sessionType,
                    notes = finalNotes,
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
