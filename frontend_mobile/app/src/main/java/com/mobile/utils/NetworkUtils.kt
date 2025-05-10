package com.mobile.utils

import android.util.Log
import com.mobile.model.CourseDTO
import com.mobile.model.TutorRegistration
import com.mobile.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*
import java.text.SimpleDateFormat
import java.time.LocalDate

/**
 * Utility class for handling network operations
 * This class provides methods to interact with the backend API
 * All methods are suspend functions that run on IO dispatcher
 * Each method includes detailed error logging
 */
object NetworkUtils {

    private const val TAG = "NetworkUtils"

    // Server configuration
    // Backend URL configuration with fallbacks
    // For production: Use the deployed Cloud Run URL
    // For development: Use the local development URL
    private const val PRODUCTION_BACKEND_URL = "https://judify-795422705086.asia-east1.run.app"
    private const val DEVELOPMENT_BACKEND_URL = "http://192.168.86.107:8080" // Android emulator uses 10.0.2.2 to access host's localhost

    // Set this to false for production, true for development
    private const val IS_DEVELOPMENT_MODE = true

    // Determine which URL to use based on the mode
    private val DEPLOYED_BACKEND_URL: String = 
        if (IS_DEVELOPMENT_MODE) DEVELOPMENT_BACKEND_URL else PRODUCTION_BACKEND_URL

    // Base URL for API requests
    private val BASE_URL: String = "$DEPLOYED_BACKEND_URL/api"

    /**
     * Authentication response data class
     */
    data class AuthResponse(
        val success: Boolean = true,
        val isAuthenticated: Boolean,
        val userId: Long? = null,
        val studentId: Long? = null,
        val tutorId: Long? = null,
        val username: String = "",
        val email: String = "",
        val firstName: String = "",
        val lastName: String = "",
        val role: String = "STUDENT",
        val token: String = ""
    )

    /**
     * Data class for Tutor Profile
     */
    data class TutorProfile(
        val id: Long,
        val userId: Long? = null,
        val name: String,
        val email: String = "",
        val bio: String = "",
        val rating: Float = 0.0f,
        val subjects: List<String> = emptyList(),
        val education: String = "",
        val hourlyRate: Double = 0.0,
        val yearsExperience: Int = 0,
        val latitude: Double? = null,
        val longitude: Double? = null
    )

    /**
     * Data class for Review
     */
    data class Review(
        val id: Long,
        val tutorId: Long,
        val studentId: Long,
        val rating: Int,
        val comment: String,
        val dateCreated: String
    )

    /**
     * Data class for Message
     */
    data class Message(
        val id: Long,
        val conversationId: Long,
        val senderId: Long,
        val content: String,
        val timestamp: String,
        val isRead: Boolean = false
    )

    /**
     * Data class for Notification
     */
    data class Notification(
        val id: Long,
        val userId: Long,
        val type: String,
        val content: String,
        val timestamp: String,
        val isRead: Boolean = false
    )

    /**
     * Data class for PaymentTransaction
     */
    data class PaymentTransaction(
        val id: Long,
        val payerId: Long,
        val payeeId: Long,
        val amount: Double,
        val status: String,
        val reference: String? = null,
        val timestamp: String
    )

    /**
     * Data class for TutorAvailability
     */
    data class TutorAvailability(
        val id: Long,
        val tutorId: Long,
        val dayOfWeek: String,
        val startTime: String,
        val endTime: String
    )

    /**
     * Data class for TutoringSession
     */
    data class TutoringSession(
        val id: Long,
        val tutorId: Long,
        val studentId: String, // Using the same field name as backend (studentId)
        val startTime: String,
        val endTime: String,
        val status: String,
        val subject: String,
        val sessionType: String,
        val notes: String?,
        val tutorName: String = "", // Added tutorName field with empty default
        val studentName: String = "", // Added studentName field with empty default
        val conversationId: Long? = null, // Add conversationId field with null default
        val price: Double? = null, // Add price field with null default
        val latitude: Double? = null, // Latitude for in-person sessions
        val longitude: Double? = null, // Longitude for in-person sessions
        val locationName: String? = null, // Location name for in-person sessions
        val locationData: String? = null // For backward compatibility
    )

    /**
     * Data class for Conversation
     */
    data class Conversation(
        val id: Long,
        val studentId: Long,
        val tutorId: Long,
        val studentName: String = "",
        val tutorName: String = "",
        val lastMessage: String? = null,
        val lastMessageTime: String? = null,
        val unreadCount: Int = 0,
        val createdAt: String = "",
        val updatedAt: String = ""
    ) : android.os.Parcelable {
        constructor(parcel: android.os.Parcel) : this(
            parcel.readLong(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString(),
            parcel.readString(),
            parcel.readInt(),
            parcel.readString() ?: "",
            parcel.readString() ?: ""
        )

        override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
            parcel.writeLong(id)
            parcel.writeLong(studentId)
            parcel.writeLong(tutorId)
            parcel.writeString(studentName)
            parcel.writeString(tutorName)
            parcel.writeString(lastMessage)
            parcel.writeString(lastMessageTime)
            parcel.writeInt(unreadCount)
            parcel.writeString(createdAt)
            parcel.writeString(updatedAt)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : android.os.Parcelable.Creator<Conversation> {
            override fun createFromParcel(parcel: android.os.Parcel): Conversation {
                return Conversation(parcel)
            }

            override fun newArray(size: Int): Array<Conversation?> {
                return arrayOfNulls(size)
            }
        }
    }

    /**
     * Authenticate user with the API
     * @param email User's email
     * @param password User's password (plain text)
     * @return AuthResponse containing authentication result and user details
     */
    suspend fun authenticateUser(email: String, password: String): Result<AuthResponse> {
        Log.d(TAG, "Attempting to authenticate user: $email")
        return withContext(Dispatchers.IO) {
            try {
                // Create URL with query parameters
                val params = mapOf(
                    "email" to email,
                    "password" to password
                )
                val url = createUrlWithParams("$BASE_URL/users/authenticate", params)
                Log.d(TAG, "Making auth request to: $url")
                val connection = createPostConnection(url)

                return@withContext handleResponse(connection) { response ->
                    Log.d(TAG, "Received auth response: $response")
                    val json = parseJsonResponse(response)

                    // Check if authenticated
                    val isAuthenticated = json.optBoolean("authenticated", json.optBoolean("isAuthenticated", false))

                    // If not authenticated, return a response with default values
                    if (!isAuthenticated) {
                        return@handleResponse AuthResponse(
                            isAuthenticated = false,
                            userId = null,
                            studentId = null,
                            tutorId = null,
                            username = "",
                            email = "",
                            firstName = "",
                            lastName = "",
                            role = "STUDENT",
                            token = ""
                        )
                    }

                    // If authenticated, get all the user details
                    // Use the role directly from the backend without mapping
                    val role = json.optString("role", "")

                    // Get studentId or tutorId based on role
                    val studentId = if (role == "STUDENT") json.optLong("studentId", 0).takeIf { it > 0 } else null
                    val tutorId = if (role == "TUTOR") json.optLong("tutorId", 0).takeIf { it > 0 } else null

                    AuthResponse(
                        isAuthenticated = true,
                        userId = json.optLong("userId"),
                        studentId = studentId,
                        tutorId = tutorId,
                        username = json.optString("username", ""),
                        email = json.optString("email", ""),
                        firstName = json.optString("firstName", ""),
                        lastName = json.optString("lastName", ""),
                        role = role,
                        token = json.optString("token", "")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Authentication failed: ${e.message}", e)
                handleNetworkError(e, "authenticating user")
            }
        }
    }

    /**
     * Register a new user with the API
     * @param user User object containing registration details
     * @return User object returned from the server
     */
    suspend fun registerUser(user: User): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/users/addUser")
                val connection = createPostConnection(url)

                // Use the role directly without mapping
                // The backend expects STUDENT, not LEARNER
                val role = user.roles

                // Log a warning about sending plain passwords
                Log.w(TAG, "Warning: Sending plain password to server. Consider implementing proper password hashing on the client side.")

                val jsonObject = JSONObject().apply {
                    put("username", user.email) // Using email as username
                    put("email", user.email)
                    // Note: passwordHash is actually the plain password - server will hash it
                    put("password", user.passwordHash) 
                    put("firstName", user.firstName)
                    put("lastName", user.lastName)
                    put("role", role) // Use the role directly
                    user.profilePicture?.let { put("profilePicture", it) }
                    user.contactDetails?.let { put("contactDetails", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    // Use the role directly from the backend without mapping
                    val role = json.optString("role", "")

                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = json.optString("password", json.optString("passwordHash", "")),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = role
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "registering user")
            }
        }
    }

    /**
     * Register a new tutor with the API
     * @param tutorRegistration TutorRegistration object containing registration details
     * @return Result indicating success or failure
     */
    suspend fun registerTutor(tutorRegistration: TutorRegistration): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutors/register")
                val connection = createPostConnection(url)

                // Create JSON object with both user and tutor profile information
                val jsonObject = JSONObject().apply {
                    // User information
                    put("username", tutorRegistration.username)
                    put("email", tutorRegistration.email)
                    put("password", tutorRegistration.password)
                    put("firstName", tutorRegistration.firstName)
                    put("lastName", tutorRegistration.lastName)
                    put("role", "TUTOR")
                    tutorRegistration.contactDetails?.let { put("contactDetails", it) }

                    // Tutor profile information
                    put("bio", tutorRegistration.bio)
                    put("expertise", tutorRegistration.expertise)
                    put("hourlyRate", tutorRegistration.hourlyRate)

                    // Add subjects as a JSON array
                    val subjectsArray = JSONArray()
                    tutorRegistration.subjects.forEach { subjectsArray.put(it) }
                    put("subjects", subjectsArray)

                    // Add location if available
                    tutorRegistration.latitude?.let { put("latitude", it) }
                    tutorRegistration.longitude?.let { put("longitude", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    json.optBoolean("success", true)
                }
            } catch (e: Exception) {
                handleNetworkError(e, "registering tutor")
            }
        }
    }

    /**
     * Extension function to safely get a float value from a JSONObject
     */
    private fun JSONObject.optFloat(name: String, fallback: Float): Float {
        return try {
            // First try to get it as a double and convert to float
            val value = this.optDouble(name, fallback.toDouble())
            value.toFloat()
        } catch (e: Exception) {
            fallback
        }
    }

    /**
     * Get tutor profile by subject ID
     */
    suspend fun getTutorProfileBySubjectId(subjectId: Long): Result<TutorProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/subjects/$subjectId/tutor")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    // Log the raw JSON response for debugging
                    Log.d(TAG, "Tutor Profile from Subject JSON response: $json")

                    // Map the fields from the backend TutorProfileDTO to our TutorProfile model
                    val id = json.optLong("profileId", 0L)

                    // First try to extract a full name by combining firstName and lastName
                    var displayName = ""
                    Log.d(TAG, "Attempting to extract tutor name for profile ID: $id")

                    // Check if the response has a user object that contains name fields
                    if (json.has("user")) {
                        try {
                            val userObj = json.getJSONObject("user")
                            val firstName = userObj.optString("firstName", "")
                            val lastName = userObj.optString("lastName", "")

                            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                displayName = "$firstName $lastName".trim()
                                Log.d(TAG, "Found name in user object: $displayName")
                            } else {
                                Log.d(TAG, "User object exists but doesn't have name fields")
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Error extracting name from user object: ${e.message}")
                        }
                    } else {
                        Log.d(TAG, "No user object found in the response")
                    }

                    // If still no name, try direct fields
                    if (displayName.isEmpty()) {
                        // Try to get the username first (which might be set to a display name)
                        val username = json.optString("username", "")
                        if (username.isNotEmpty() && !username.contains("@")) {
                            displayName = username
                        } 

                        // If we couldn't get a valid name, try to construct one from first and last name directly
                        if (displayName.isEmpty()) {
                            val firstName = json.optString("firstName", "")
                            val lastName = json.optString("lastName", "")

                            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                displayName = "$firstName $lastName".trim()
                            }
                        }
                    }

                    // If all else fails, try to get a name from any available field
                    if (displayName.isEmpty()) {
                        // Try username first
                        val username = json.optString("username", "")
                        if (username.isNotEmpty() && !username.contains("@")) {
                            displayName = username
                        } else {
                            // Try name field
                            val name = json.optString("name", "")
                            if (name.isNotEmpty() && !name.contains("@")) {
                                displayName = name
                            } else {
                                // Try fullName field
                                val fullName = json.optString("fullName", "")
                                if (fullName.isNotEmpty() && !fullName.contains("@")) {
                                    displayName = fullName
                                } else {
                                    // Last resort, use a generic name
                                    displayName = "Tutor #$id"
                                }
                            }
                        }
                    }

                    // Clean up display name - make sure it doesn't contain email 
                    if (displayName.contains("@")) {
                        // Try to extract a name from the email
                        val emailParts = displayName.split("@")
                        if (emailParts.isNotEmpty()) {
                            val nameFromEmail = emailParts[0].replace(".", " ").split(" ")
                                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                            displayName = nameFromEmail
                        } else {
                            displayName = "Tutor #$id"
                        }
                    }

                    // Get email
                    var email = ""
                    if (json.has("user")) {
                        try {
                            val userObj = json.getJSONObject("user")
                            email = userObj.optString("email", "")
                        } catch (e: Exception) {
                            Log.d(TAG, "Error extracting email from user object: ${e.message}")
                        }
                    }
                    if (email.isEmpty()) {
                        email = json.optString("email", "")
                    }

                    // Get bio
                    val bio = json.optString("bio", "No bio available")

                    // Get rating
                    val rating = json.optFloat("rating", 0f)

                    // Get hourly rate
                    val hourlyRate = json.optDouble("hourlyRate", 0.0)

                    // Get subjects
                    val subjects = mutableListOf<String>()
                    if (json.has("subjects")) {
                        try {
                            val subjectsArray = json.getJSONArray("subjects")
                            for (i in 0 until subjectsArray.length()) {
                                subjects.add(subjectsArray.getString(i))
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Error extracting subjects: ${e.message}")
                        }
                    }

                    TutorProfile(
                        id = id,
                        name = displayName,
                        email = email,
                        bio = bio,
                        rating = rating,
                        subjects = subjects,
                        hourlyRate = hourlyRate
                    )
                }
            } catch (e: Exception) {
                return@withContext handleNetworkError(e, "getting tutor profile by subject ID")
            }
        }
    }

    /**
     * Get tutor profile by ID
     */
    suspend fun getTutorProfile(tutorId: Long): Result<TutorProfile> {
        Log.d(TAG, "Getting tutor profile for tutorId: $tutorId")
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutors/findById/$tutorId")
                val connection = createGetConnection(url)
                
                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    
                    // Parse tutor details from response
                    val tutorName = "${json.optString("firstName", "")} ${json.optString("lastName", "")}".trim()
                    val tutorEmail = json.optString("email", "")
                    val tutorBio = json.optString("bio", "")
                    val tutorRating = json.optDouble("rating", 0.0).toFloat()
                    val hourlyRate = json.optDouble("hourlyRate", 0.0)
                    val yearsExperience = json.optInt("yearsExperience", 0)
                    
                    // Parse subjects if available
                    val subjectsArray = json.optJSONArray("subjects")
                    val subjects = mutableListOf<String>()
                    if (subjectsArray != null) {
                        for (i in 0 until subjectsArray.length()) {
                            val subjectObj = subjectsArray.optJSONObject(i)
                            val subjectName = subjectObj?.optString("name", "")
                            if (!subjectName.isNullOrEmpty()) {
                                subjects.add(subjectName)
                            }
                        }
                    }
                    
                    // Create and return TutorProfile object
                    TutorProfile(
                        id = tutorId,
                        userId = json.optLong("userId", 0L),
                        name = tutorName,
                        email = tutorEmail,
                        bio = tutorBio,
                        rating = tutorRating,
                        subjects = subjects,
                        education = json.optString("education", ""),
                        hourlyRate = hourlyRate,
                        yearsExperience = yearsExperience,
                        latitude = json.optDouble("latitude", 0.0).takeIf { it != 0.0 },
                        longitude = json.optDouble("longitude", 0.0).takeIf { it != 0.0 }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting tutor profile: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get a student's profile details by userId
     * @param userId The user ID of the student
     * @return TutorProfile object with basic user information (reusing the same class for consistency)
     */
    suspend fun getStudentProfile(userId: Long): Result<TutorProfile> {
        Log.d(TAG, "Getting student profile for userId: $userId")
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/users/findById/$userId")
                val connection = createGetConnection(url)
                
                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    
                    // Parse student details from response
                    val studentName = "${json.optString("firstName", "")} ${json.optString("lastName", "")}".trim()
                    val studentEmail = json.optString("email", "")
                    
                    // Create and return TutorProfile object (reusing for now)
                    TutorProfile(
                        id = userId,
                        userId = userId,
                        name = studentName,
                        email = studentEmail,
                        bio = json.optString("bio", ""),
                        rating = 0.0f,
                        subjects = emptyList(),
                        education = "",
                        hourlyRate = 0.0,
                        yearsExperience = 0
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting student profile: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Debug method to verify API connectivity
     * This is a utility method for development only
     */
    suspend fun verifyApiConnection(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {

                // Try to connect to the root API endpoint
                val url = URL("$BASE_URL/ping")
                val connection = createGetConnection(url)
                connection.connectTimeout = 5000 // 5 seconds timeout
                connection.readTimeout = 5000

                var tutorConn: HttpURLConnection? = null
                return@withContext try {
                    val responseCode = connection.responseCode
                    Log.d(TAG, "API connection test response code: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Result.success("API connection successful: $responseCode")
                    } else {
                        // Try another endpoint if ping fails
                        val tutorUrl = URL("$BASE_URL/tutors/getAllProfiles")
                        tutorConn = createGetConnection(tutorUrl)
                        tutorConn.connectTimeout = 5000
                        tutorConn.readTimeout = 5000

                        val tutorResponseCode = tutorConn.responseCode
                        Log.d(TAG, "Tutor API test response code: $tutorResponseCode")

                        if (tutorResponseCode == HttpURLConnection.HTTP_OK) {
                            Result.success("Tutor API connection successful: $tutorResponseCode")
                        } else {
                            Result.failure(Exception("Failed to connect to API. Response: $tutorResponseCode"))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "API connection test failed: ${e.message}", e)
                    Result.failure(Exception("API connection test failed: ${e.message}"))
                } finally {
                    connection.disconnect()
                    tutorConn?.disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in verifyApiConnection: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Find conversations for a user (deprecated - uses the old path format)
     * @param userId ID of the user
     * @return Result<List<Conversation>> containing the user's conversations
     */
    suspend fun findConversationsByUser(userId: Long): Result<List<Conversation>> {
        // Delegate to the new method for backward compatibility
        return getConversationsForUser(userId)
    }

    /**
     * Alternative method to fetch conversations (deprecated - uses the old path format)
     * @param userId ID of the user
     * @return Result<List<Conversation>> containing the user's conversations
     */
    suspend fun findConversationsAlternative(userId: Long): Result<List<Conversation>> {
        // Delegate to the new method for backward compatibility
        return getConversationsForUser(userId)
    }

    /**
     * Get conversations for a user - fixed version that handles the backend path variable correctly
     * @param userId ID of the user
     * @return Result<List<Conversation>> containing the user's conversations
     */
    suspend fun getConversationsForUser(userId: Long): Result<List<Conversation>> {
        return withContext(Dispatchers.IO) {
            try {
                // Try multiple approaches, starting with the most standards-compliant

                // Try approach #1: Direct GET with user ID in path
                try {
                    val url = URL(createApiUrl("conversations/findByUser/$userId"))
                    Log.d(TAG, "Creating GET connection to URL: $url")
                    Log.d(TAG, "Using deployed backend URL: $DEPLOYED_BACKEND_URL")
                    val connection = createGetConnection(url)

                    // Add headers to indicate this is our userId (must be set before connecting)
                    connection.setRequestProperty("X-User-ID", userId.toString())

                    // Now we can connect and check the response
                    connection.connect()

                    // Try GET approach
                    val responseCode = connection.responseCode
                    Log.d(TAG, "Response code: $responseCode")

                    if (responseCode in 200..299) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        return@withContext parseConversationsResponse(response)
                    } else {
                        // Read error response for logging
                        val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                        Log.d(TAG, "Response: $errorResponse")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "First GET approach failed: ${e.message}")
                }

                // Try approach #2: GET with 'participant' in the URL
                try {
                    val url = URL(createApiUrl("conversations/findByUser/participant/$userId"))
                    Log.d(TAG, "Creating GET connection to URL: $url")
                    val connection = createGetConnection(url)
                    connection.connect()

                    val responseCode = connection.responseCode
                    Log.d(TAG, "Response code: $responseCode")

                    if (responseCode in 200..299) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        return@withContext parseConversationsResponse(response)
                    } else {
                        val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                        Log.d(TAG, "Response: $errorResponse")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Second GET approach failed: ${e.message}")
                }

                // Try approach #3: POST approaches
                Log.d(TAG, "GET request failed with error, trying POST approach")
                val postResult = tryPostConversationsByUserId(userId)
                if (postResult.isSuccess) {
                    return@withContext postResult
                }

                // Try approach #4: Alternative endpoint pattern
                Log.d(TAG, "POST approaches failed, trying alternative endpoint pattern")
                val alternativeResult = tryAlternativeEndpoint(userId)
                if (alternativeResult.isSuccess) {
                    return@withContext alternativeResult
                }

                // If all approaches fail, use mock data as fallback
                Log.e(TAG, "All API approaches failed, using mock data as fallback")
                return@withContext Result.success(createMockConversations(userId))
            } catch (e: Exception) {
                Log.e(TAG, "Exception in conversations API: ${e.message}", e)
                return@withContext handleNetworkError(e, "finding conversations for user")
            }
        }
    }

    private fun createMockConversations(userId: Long): List<Conversation> {
        // Create mock conversations with the current user as student and different users as tutors
        val otherUser1 = 2L
        val otherUser2 = 3L

        // Use negative IDs for mock conversations to avoid conflicts with real conversation IDs
        return listOf(
            Conversation(
                id = -1L,
                studentId = userId,
                tutorId = otherUser1,
                studentName = "Current User",
                tutorName = "John Doe",
                lastMessage = "Hello, how are you?",
                lastMessageTime = "2024-03-20T14:30:00",
                unreadCount = 1,
                createdAt = "2024-03-20T10:00:00"
            ),
            Conversation(
                id = -2L,
                studentId = userId,
                tutorId = otherUser2,
                studentName = "Current User",
                tutorName = "Jane Smith",
                lastMessage = "When is our next session?",
                lastMessageTime = "2024-03-19T09:15:00",
                unreadCount = 0,
                createdAt = "2024-03-15T16:45:00"
            )
        )
    }

    private suspend fun tryGetConversations(userId: Long): Result<List<Conversation>> {
        return try {
            val url = URL(createApiUrl("conversations/findByUser/$userId"))
            val connection = createGetConnection(url)

            // Set headers before connecting
            connection.setRequestProperty("X-User-ID", userId.toString())
            connection.connect()

            val result = handleApiResponse(connection)
            if (result.isSuccess) {
                parseConversationsResponse(result.getOrThrow())
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun tryPostConversationsByUserId(userId: Long): Result<List<Conversation>> {
        return try {
            // Try different approaches to see what works

            // Approach 1: Send a POST to find by userId directly
            val url1 = URL(createApiUrl("conversations/findByUserId"))
            val connection1 = createPostConnection(url1)
            val jsonObject1 = JSONObject().apply {
                put("userId", userId)
            }

            try {
                val result1 = handleApiResponse(connection1, jsonObject1.toString())
                if (result1.isSuccess) {
                    return parseConversationsResponse(result1.getOrThrow())
                }
            } catch (e: Exception) {
                Log.d(TAG, "First approach failed: ${e.message}")
            }

            // Approach 2: Send a POST to findByUser/{userId} with participant in body 
            val url2 = URL(createApiUrl("conversations/findByUser/$userId"))
            val connection2 = createPostConnection(url2)
            val jsonObject2 = JSONObject().apply {
                put("participant", JSONObject().apply {
                    put("userId", userId)
                })
            }

            try {
                val result2 = handleApiResponse(connection2, jsonObject2.toString())
                if (result2.isSuccess) {
                    return parseConversationsResponse(result2.getOrThrow())
                }
            } catch (e: Exception) {
                Log.d(TAG, "Second approach failed: ${e.message}")
            }

            // Fallback to mock data if both approaches fail
            Log.e(TAG, "Both API approaches failed, using mock data")
            return Result.success(createMockConversations(userId))
        } catch (e: Exception) {
            Log.e(TAG, "Error in POST approaches: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseConversationsResponse(response: String): Result<List<Conversation>> {
        return try {
            val jsonArray = parseJsonArrayResponse(response, "conversations")
            val conversations = mutableListOf<Conversation>()

            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)

                // Ensure we have a valid conversation ID from the backend
                val conversationId = json.optLong("conversationId", -1L)
                if (conversationId == -1L) {
                    Log.w(TAG, "Conversation missing ID, skipping: $json")
                    continue
                }

                // Extract IDs - map from backend's user1/user2 to student/tutor if needed
                val studentId = json.optLong("studentId", -1L)
                val tutorId = json.optLong("tutorId", -1L)

                // Handle the case where backend uses user1/user2 instead of student/tutor
                val finalStudentId = if (studentId != -1L) studentId else json.optLong("user1Id")
                val finalTutorId = if (tutorId != -1L) tutorId else json.optLong("user2Id")

                // Extract names - prioritize specific names, fallback to user1/user2 names
                var studentName = json.optString("studentName", "")
                var tutorName = json.optString("tutorName", "")

                // If student/tutor names are empty, try user1/user2 names
                if (studentName.isEmpty()) {
                    studentName = json.optString("user1Name", "Unknown Student")
                }

                if (tutorName.isEmpty()) {
                    tutorName = json.optString("user2Name", "Unknown Tutor")
                }

                Log.d(TAG, "Parsed conversation: id=$conversationId, studentId=$finalStudentId, tutorId=$finalTutorId")
                Log.d(TAG, "Names: studentName=$studentName, tutorName=$tutorName")

                conversations.add(
                    Conversation(
                        id = conversationId,
                        studentId = finalStudentId,
                        tutorId = finalTutorId,
                        studentName = studentName,
                        tutorName = tutorName,
                        lastMessage = json.optString("lastMessage", ""),
                        lastMessageTime = json.optString("lastMessageTime", ""),
                        unreadCount = json.optInt("unreadCount", 0),
                        createdAt = json.optString("createdAt", ""),
                        updatedAt = json.optString("updatedAt", "")
                    )
                )
            }
            Result.success(conversations)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing conversations: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun handleApiResponse(connection: HttpURLConnection, requestBody: String? = null): Result<String> {
        return try {
            if (requestBody != null) {
                val output = connection.outputStream
                output.write(requestBody.toByteArray())
                output.close()
            } else {
                // For GET requests, explicitly connect if not done yet
                connection.connect()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                Result.success(responseBody)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Result.failure(Exception("HTTP Error: $responseCode - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Create a new conversation
     * @param participantIds List of user IDs who are participants in the conversation
     * @return Result<Conversation> containing the created conversation
     */
    suspend fun createConversation(participantIds: List<Long>): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate input
                if (participantIds.size < 2) {
                    Log.e(TAG, "Failed to create conversation: At least 2 participants are required")
                    return@withContext Result.failure(IllegalArgumentException("At least 2 participants are required"))
                }

                val studentId = participantIds[0]
                val tutorId = participantIds[1]

                // Ensure participants are different users
                if (studentId == tutorId) {
                    Log.e(TAG, "Failed to create conversation: Cannot create conversation with same user (ID: $studentId)")
                    return@withContext Result.failure(IllegalArgumentException("Cannot create conversation with the same user"))
                }

                // Log the participant IDs
                Log.d(TAG, "Creating conversation with studentId=$studentId, tutorId=$tutorId")

                // Use the correct endpoint path as defined in the backend ConversationController
                val url = URL(createApiUrl("conversations/createConversation"))
                Log.d(TAG, "Making API request to URL: $url")
                val connection = createPostConnection(url)

                // Create request body with studentId and tutorId
                val jsonObject = JSONObject().apply {
                    put("studentId", studentId)
                    put("tutorId", tutorId)
                }

                val requestBody = jsonObject.toString()
                Log.d(TAG, "Request payload: $requestBody")

                return@withContext handleResponse(connection, requestBody) { response ->
                    Log.d(TAG, "Received conversation creation response: $response")
                    val json = parseJsonResponse(response)

                    // Log the parsed JSON for debugging
                    Log.d(TAG, "Parsed JSON response: ${json.toString(2)}")

                    val conversationId = json.optLong("conversationId", -1L)
                    val responseStudentId = json.optLong("studentId")
                    val responseTutorId = json.optLong("tutorId")

                    Log.d(TAG, "Created conversation: id=$conversationId, studentId=$responseStudentId, tutorId=$responseTutorId")

                    // Verify that both users are in the response - backend might swap the order
                    val responseSameUsers = ((responseStudentId == studentId && responseTutorId == tutorId) || 
                                            (responseStudentId == tutorId && responseTutorId == studentId))

                    if (!responseSameUsers) {
                        Log.e(TAG, "Error: Conversation response user IDs don't match request IDs. " +
                                 "Request: [$studentId, $tutorId], Response: [$responseStudentId, $responseTutorId]")
                    }

                    // Verify we don't have same user ID for both participants - that would be an error
                    if (responseStudentId == responseTutorId) {
                        Log.e(TAG, "Backend error: Same user ID for both participants: $responseStudentId")
                        // Override the response with correct IDs if backend sends incorrect data
                        return@handleResponse Conversation(
                            id = conversationId,
                            studentId = studentId,
                            tutorId = tutorId,
                            createdAt = json.optString("createdAt", ""),
                            updatedAt = json.optString("updatedAt", "")
                        )
                    }

                    Conversation(
                        id = conversationId,
                        studentId = responseStudentId,
                        tutorId = responseTutorId,
                        studentName = json.optString("studentName", ""),
                        tutorName = json.optString("tutorName", ""),
                        createdAt = json.optString("createdAt", ""),
                        updatedAt = json.optString("updatedAt", "")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create conversation", e)
                handleNetworkError(e, "creating conversation")
            }
        }
    }

    /**
     * Create a new conversation with a tutor
     * @param studentUserId The user ID of the student
     * @param tutorUserId The user ID of the tutor (not the tutor profile ID)
     * @param sessionId Optional session ID to associate with the conversation
     * @return Result<Conversation> containing the created conversation
     */
    suspend fun createConversationWithTutor(studentUserId: Long, tutorUserId: Long, sessionId: Long? = null): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                // Validation
                if (studentUserId <= 0) {
                    Log.e(TAG, "Failed to create conversation: Invalid student user ID: $studentUserId")
                    return@withContext Result.failure(IllegalArgumentException("Invalid student user ID"))
                }

                if (tutorUserId <= 0) {
                    Log.e(TAG, "Failed to create conversation: Invalid tutor user ID: $tutorUserId")
                    return@withContext Result.failure(IllegalArgumentException("Invalid tutor user ID"))
                }

                // Log the IDs
                Log.d(TAG, "Creating conversation between studentUserId=$studentUserId and tutorUserId=$tutorUserId")

                // Use the endpoint for creating conversations between users
                // Add sessionId as a query parameter if provided
                val baseUrl = "conversations/createWithTutor/$studentUserId/$tutorUserId"
                val urlString = if (sessionId != null) {
                    createApiUrl("$baseUrl?sessionId=$sessionId")
                } else {
                    createApiUrl(baseUrl)
                }

                val url = URL(urlString)
                Log.d(TAG, "Making API request to URL: $url")
                val connection = createPostConnection(url)

                return@withContext handleResponse(connection, "") { response ->
                    Log.d(TAG, "Received conversation creation response: $response")
                    val json = parseJsonResponse(response)

                    // Log the parsed JSON for debugging
                    Log.d(TAG, "Parsed JSON response: ${json.toString(2)}")

                    val conversationId = json.optLong("conversationId", -1L)
                    val responseStudentId = json.optLong("user1Id") // user1Id is studentId
                    val responseTutorId = json.optLong("user2Id")   // user2Id is tutorId

                    // Extract names properly - user1Name is student, user2Name is tutor
                    val studentName = json.optString("user1Name", "")
                    val tutorName = json.optString("user2Name", "")

                    Log.d(TAG, "Created conversation: id=$conversationId, studentId=$responseStudentId, tutorId=$responseTutorId")
                    Log.d(TAG, "Names: studentName=$studentName, tutorName=$tutorName")

                    Conversation(
                        id = conversationId,
                        studentId = responseStudentId,
                        tutorId = responseTutorId,
                        studentName = studentName,
                        tutorName = tutorName,
                        createdAt = json.optString("createdAt", ""),
                        updatedAt = json.optString("updatedAt", "")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create conversation with tutor", e)
                handleNetworkError(e, "creating conversation with tutor")
            }
        }
    }

    /**
     * Delete a conversation
     * @param conversationId ID of the conversation to delete
     * @return Result<Unit> indicating success or failure
     */
    suspend fun deleteConversation(conversationId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Use the correct endpoint path as defined in the backend ConversationController
                val url = URL(createApiUrl("conversations/deleteConversation/$conversationId"))
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting conversation")
            }
        }
    }

    /**
     * Get messages for a conversation
     * @param conversationId ID of the conversation
     * @param page Page number (0-based)
     * @param size Page size
     * @return Result<List<Message>> containing the messages
     */
    suspend fun getMessages(conversationId: Long, page: Int = 0, size: Int = 20): Result<List<Message>> {
        return withContext(Dispatchers.IO) {
            try {
                // Build the URL with query parameters
                val params = mapOf(
                    "page" to page.toString(),
                    "size" to size.toString()
                )
                val url = createUrlWithParams(
                    "$BASE_URL/messages/findByConversationPaginated/$conversationId", 
                    params
                )
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonObject = parseJsonResponse(response)
                    val jsonArray = jsonObject.getJSONArray("content")
                    val messages = mutableListOf<Message>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)

                        // Format the createdAt date properly
                        val createdAt = json.optString("createdAt")
                        val timestamp = if (createdAt.isNotEmpty()) {
                            try {
                                if (createdAt.contains("T")) {
                                    // Parse ISO format timestamp with UTC time zone
                                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    }
                                    val dateObj = isoFormat.parse(createdAt.substring(0, 19))
                                    if (dateObj != null) {
                                        // Format with UTC time zone to ensure consistency
                                        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        }
                                        outputFormat.format(dateObj)
                                    } else {
                                        createdAt
                                    }
                                } else {
                                    val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
                                    val dateObj = dateFormat.parse(createdAt)
                                    if (dateObj != null) {
                                        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                        outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        outputFormat.format(dateObj)
                                    } else {
                                        // Fallback if parsing fails
                                        createdAt
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing timestamp: $createdAt", e)
                                createdAt
                            }
                        } else {
                            val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            outputFormat.format(Date())
                        }

                        messages.add(
                            Message(
                                id = json.optLong("messageId"),
                                conversationId = json.optLong("conversationId"),
                                senderId = json.optLong("senderId"),
                                content = json.optString("content"),
                                timestamp = timestamp,
                                isRead = json.optBoolean("isRead", false)
                            )
                        )
                    }
                    messages
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting messages for conversation")
            }
        }
    }

    /**
     * Send a message in a conversation
     * @param conversationId ID of the conversation
     * @param senderId ID of the message sender
     * @param receiverId ID of the message receiver
     * @param content Message content
     * @return Result<Message> containing the sent message
     */
    suspend fun sendMessage(conversationId: Long, senderId: Long, receiverId: Long, content: String): Result<Message> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/messages/sendMessage")
                val connection = createPostConnection(url)

                // Create request body with field names matching the backend's MessageDTO class
                val jsonObject = JSONObject().apply {
                    put("conversationId", conversationId)
                    put("senderId", senderId)
                    put("receiverId", receiverId)
                    put("content", content)
                    put("isRead", false)
                }

                Log.d(TAG, "Making POST request to $url")
                Log.d(TAG, "Request headers: ${connection.requestProperties}")
                Log.d(TAG, "Sending request body: $jsonObject")

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    // Format the createdAt date properly
                    val createdAt = json.optString("createdAt")
                    val timestamp = if (createdAt.isNotEmpty()) {
                        try {
                            if (createdAt.contains("T")) {
                                // Parse ISO format timestamp with UTC time zone
                                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }
                                val dateObj = isoFormat.parse(createdAt.substring(0, 19))
                                if (dateObj != null) {
                                    // Format with UTC time zone to ensure consistency
                                    val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    }
                                    outputFormat.format(dateObj)
                                } else {
                                    createdAt
                                }
                            } else {
                                val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
                                val dateObj = dateFormat.parse(createdAt)
                                if (dateObj != null) {
                                    val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                    outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    outputFormat.format(dateObj)
                                } else {
                                    // Fallback if parsing fails
                                    createdAt
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing timestamp: $createdAt", e)
                            createdAt
                        }
                    } else {
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        outputFormat.format(Date())
                    }

                    // Extract message ID properly - could be either messageId or id in the response
                    val id = json.optLong("messageId", json.optLong("id", 0))
                    val isReadValue = json.optBoolean("isRead", false)

                    // The receiverId is sent to the backend but not stored in the Message object
                    // since the current Message data class doesn't have a receiverId field
                    Message(
                        id = id,
                        conversationId = json.optLong("conversationId"),
                        senderId = json.optLong("senderId"),
                        content = json.optString("content"),
                        timestamp = timestamp,
                        isRead = isReadValue
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "sending message")
            }
        }
    }

    /**
     * Mark a message as read
     * @param messageId ID of the message to mark as read
     * @return Result<Message> containing the updated message
     */
    suspend fun markMessageAsRead(messageId: Long): Result<Message> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/messages/markAsRead/$messageId")
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)

                    // Format the createdAt date properly
                    val createdAt = json.optString("createdAt")
                    val timestamp = if (createdAt.isNotEmpty()) {
                        try {
                            if (createdAt.contains("T")) {
                                // Parse ISO format timestamp with UTC time zone
                                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }
                                val dateObj = isoFormat.parse(createdAt.substring(0, 19))
                                if (dateObj != null) {
                                    // Format with UTC time zone to ensure consistency
                                    val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    }
                                    outputFormat.format(dateObj)
                                } else {
                                    createdAt
                                }
                            } else {
                                val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
                                val dateObj = dateFormat.parse(createdAt)
                                if (dateObj != null) {
                                    val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                    outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    outputFormat.format(dateObj)
                                } else {
                                    // Fallback if parsing fails
                                    createdAt
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing timestamp: $createdAt", e)
                            createdAt
                        }
                    } else {
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        outputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        outputFormat.format(Date())
                    }

                    Message(
                        id = json.optLong("messageId"),
                        conversationId = json.optLong("conversationId"),
                        senderId = json.optLong("senderId"),
                        content = json.optString("content"),
                        timestamp = timestamp,
                        isRead = true
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "marking message as read")
            }
        }
    }

    /**
     * Mark all messages in a conversation as read
     * @param conversationId ID of the conversation
     * @param userId ID of the user whose messages should be marked as read
     * @return Result<Unit> indicating success or failure
     */
    suspend fun markAllMessagesAsRead(conversationId: Long, userId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/messages/markAllAsRead/$conversationId?userId=$userId")
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "marking all messages as read")
            }
        }
    }

    /**
     * Data class for TutorSubject
     */
    data class TutorSubject(
        val id: Long,
        val tutorProfileId: Long,
        val subject: String,
        val createdAt: String
    )

    /**
     * Get subjects by tutor profile ID
     * @param tutorProfileId ID of the tutor profile
     * @return Result<List<TutorSubject>> containing all subjects for the tutor
     */
    suspend fun getSubjectsByTutorProfileId(tutorProfileId: Long): Result<List<TutorSubject>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/subjects/by-tutor/$tutorProfileId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val subjects = mutableListOf<TutorSubject>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        subjects.add(
                            TutorSubject(
                                id = json.getLong("id"),
                                tutorProfileId = json.getLong("tutorProfileId"),
                                subject = json.getString("subject"),
                                createdAt = json.optString("createdAt", "")
                            )
                        )
                    }
                    subjects
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting subjects by tutor profile ID")
            }
        }
    }

    /**
     * Search subjects
     * @param query Search query
     * @return Result<List<TutorSubject>> containing matching subjects
     */
    suspend fun searchSubjects(query: String): Result<List<TutorSubject>> {
        return withContext(Dispatchers.IO) {
            try {
                val params = mapOf("query" to query)
                val url = createUrlWithParams("$BASE_URL/subjects/search", params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val subjects = mutableListOf<TutorSubject>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        subjects.add(
                            TutorSubject(
                                id = json.getLong("id"),
                                tutorProfileId = json.getLong("tutorProfileId"),
                                subject = json.getString("subject"),
                                createdAt = json.optString("createdAt", "")
                            )
                        )
                    }
                    subjects
                }
            } catch (e: Exception) {
                handleNetworkError(e, "searching subjects")
            }
        }
    }

    /**
     * Add a subject
     * @param tutorProfileId ID of the tutor profile
     * @param subject Subject name
     * @return Result<TutorSubject> containing the added subject
     */
    suspend fun addSubject(tutorProfileId: Long, subject: String): Result<TutorSubject> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/subjects/add")
                val connection = createPostConnection(url)

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("tutorProfileId", tutorProfileId)
                    put("subject", subject)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    TutorSubject(
                        id = json.getLong("id"),
                        tutorProfileId = json.getLong("tutorProfileId"),
                        subject = json.getString("subject"),
                        createdAt = json.optString("createdAt", "")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "adding subject")
            }
        }
    }

    /**
     * Add multiple subjects for a tutor
     * @param tutorProfileId ID of the tutor profile
     * @param subjects List of subject names
     * @return Result<List<TutorSubject>> containing the added subjects
     */
    suspend fun addSubjectsForTutor(tutorProfileId: Long, subjects: List<String>): Result<List<TutorSubject>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/subjects/add-multiple/$tutorProfileId")
                val connection = createPostConnection(url)

                // Create request body with JSON array of subjects
                val jsonArray = JSONArray()
                subjects.forEach { jsonArray.put(it) }

                return@withContext handleResponse(connection, jsonArray.toString()) { response ->
                    val responseJsonArray = parseJsonArrayResponse(response)
                    val addedSubjects = mutableListOf<TutorSubject>()

                    for (i in 0 until responseJsonArray.length()) {
                        val json = responseJsonArray.getJSONObject(i)
                        addedSubjects.add(
                            TutorSubject(
                                id = json.getLong("id"),
                                tutorProfileId = json.getLong("tutorProfileId"),
                                subject = json.getString("subject"),
                                createdAt = json.optString("createdAt", "")
                            )
                        )
                    }
                    addedSubjects
                }
            } catch (e: Exception) {
                handleNetworkError(e, "adding multiple subjects")
            }
        }
    }

    /**
     * Delete a subject
     * @param subjectId ID of the subject to delete
     * @return Result<Unit> indicating success or failure
     */
    suspend fun deleteSubject(subjectId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/subjects/delete/$subjectId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting subject")
            }
        }
    }

    /**
     * Delete all subjects for a tutor
     * @param tutorProfileId ID of the tutor profile
     * @return Result<Unit> indicating success or failure
     */
    suspend fun deleteAllSubjectsForTutor(tutorProfileId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/subjects/delete-all/$tutorProfileId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting all subjects for tutor")
            }
        }
    }

    /**
     * Update a subject
     * @param subjectId ID of the subject to update
     * @param subject New subject name
     * @return Result<TutorSubject> containing the updated subject
     */
    suspend fun updateSubject(subjectId: Long, subject: String): Result<TutorSubject> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/subjects/update/$subjectId")
                val connection = createPutConnection(url)

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("subject", subject)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    TutorSubject(
                        id = json.getLong("id"),
                        tutorProfileId = json.getLong("tutorProfileId"),
                        subject = json.getString("subject"),
                        createdAt = json.optString("createdAt", "")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "updating subject")
            }
        }
    }

    /**
     * Find a user by email
     * @param email Email of the user to find
     * @return Result<User> containing the user if found
     */
    suspend fun findUserByEmail(email: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val params = mapOf("email" to email)
                val url = createUrlWithParams("$BASE_URL/users/find-by-email", params)
                val connection = createGetConnection(url)

                handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)

                    // Use the role directly from the backend without mapping
                    val role = json.optString("role", "")

                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = json.optString("password", json.optString("passwordHash", "")),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = role
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding user by email")
            }
        }
    }

    /**
     * Update a user's profile information
     * @param user User object containing updated profile information
     * @return Result<User> containing the updated user profile
     */
    suspend fun updateUser(user: User): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/users/updateUser/${user.userId}") // Correct path
                val connection = createPutConnection(url)

                // Use the role directly without mapping
                val role = user.roles

                // Create request body with updated user information
                val jsonObject = JSONObject().apply {
                    put("userId", user.userId)
                    put("username", user.username) // Using email as username
                    put("email", user.email)
                    // Don't include password when updating user info
                    // Only include it when specifically changing password
                    put("firstName", user.firstName)
                    put("lastName", user.lastName)
                    put("role", role)
                    user.profilePicture?.let { put("profilePicture", it) }
                    user.contactDetails?.let { put("contactDetails", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    // Use the role directly from the backend without mapping
                    val role = json.optString("role", "")

                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = "", // Don't store password hash in response
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = role
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user: ${e.message}", e)
                handleNetworkError(e, "updating user profile")
            }
        }
    }

    /**
     * Find a tutor profile by user ID
     * @param userId The user ID of the tutor
     * @return Result containing the tutor profile if found
     */
    suspend fun findTutorByUserId(userId: Long): Result<TutorProfile?> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutors/findByUserId/$userId")
                val connection = createGetConnection(url)

                handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)

                    if (json.has("profileId") || json.has("id")) {
                        val subjectsJson = json.optJSONArray("subjects")
                        val subjects = mutableListOf<String>()

                        if (subjectsJson != null) {
                            for (i in 0 until subjectsJson.length()) {
                                subjects.add(subjectsJson.getString(i))
                            }
                        }

                        // Extract tutor name with fallback mechanisms for backward compatibility
                        var displayName = json.optString("name", "")

                        // If name is empty, try to extract from user object or other fields
                        if (displayName.isEmpty() && json.has("user")) {
                            try {
                                val userObj = json.getJSONObject("user")
                                val firstName = userObj.optString("firstName", "")
                                val lastName = userObj.optString("lastName", "")

                                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                    displayName = "$firstName $lastName".trim()
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "Error extracting name from user object: ${e.message}")
                            }
                        }

                        // Use direct firstName/lastName fields if still empty
                        if (displayName.isEmpty()) {
                            val firstName = json.optString("firstName", "")
                            val lastName = json.optString("lastName", "")

                            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                displayName = "$firstName $lastName".trim()
                            } else {
                                // Use username as last resort
                                displayName = json.optString("username", "Tutor #${json.optLong("id")}")
                            }
                        }

                        // Clean up display name - don't use email as a name
                        if (displayName.contains("@")) {
                            displayName = "Tutor #${json.optLong("id")}"
                        }

                        // Use profileId if available, otherwise fall back to id
                        val profileId = if (json.has("profileId")) json.optLong("profileId") else json.optLong("id")

                        TutorProfile(
                            id = profileId,
                            userId = json.optLong("userId"),
                            name = displayName,
                            email = json.optString("email", ""),
                            bio = json.optString("bio", ""),
                            subjects = subjects,
                            education = json.optString("education", ""),
                            rating = json.optDouble("rating", 0.0).toFloat(),
                            hourlyRate = json.optDouble("hourlyRate", 0.0),
                            yearsExperience = json.optInt("yearsExperience", 0)
                        )
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding tutor by user ID: ${e.message}", e)
                handleNetworkError(e, "finding tutor by user ID")
            }
        }
    }

    /**
     * Find a tutor by ID
     * @param tutorId The ID of the tutor to find
     * @return Result containing the tutor profile or error
     */
    suspend fun findTutorById(tutorId: Long): Result<TutorProfile?> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutors/findById/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)

                    if (json.has("id")) {
                        val subjectsJson = json.optJSONArray("subjects")
                        val subjects = mutableListOf<String>()

                        if (subjectsJson != null) {
                            for (i in 0 until subjectsJson.length()) {
                                subjects.add(subjectsJson.getString(i))
                            }
                        }

                        // Extract tutor name with fallback mechanisms for backward compatibility
                        var displayName = json.optString("name", "")

                        // If name is empty, try to extract from user object or other fields
                        if (displayName.isEmpty() && json.has("user")) {
                            try {
                                val userObj = json.getJSONObject("user")
                                val firstName = userObj.optString("firstName", "")
                                val lastName = userObj.optString("lastName", "")

                                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                    displayName = "$firstName $lastName".trim()
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "Error extracting name from user object: ${e.message}")
                            }
                        }

                        // Use direct firstName/lastName fields if still empty
                        if (displayName.isEmpty()) {
                            val firstName = json.optString("firstName", "")
                            val lastName = json.optString("lastName", "")

                            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                displayName = "$firstName $lastName".trim()
                            } else {
                                // Use username as last resort
                                displayName = json.optString("username", "Tutor #${json.optLong("id")}")
                            }
                        }

                        // Clean up display name - don't use email as a name
                        if (displayName.contains("@")) {
                            displayName = "Tutor #${json.optLong("id")}"
                        }

                        TutorProfile(
                            id = json.optLong("id"),
                            userId = json.optLong("userId"),
                            name = displayName,
                            email = json.optString("email", ""),
                            bio = json.optString("bio", ""),
                            subjects = subjects,
                            education = json.optString("education", ""),
                            rating = json.optDouble("rating", 0.0).toFloat(),
                            hourlyRate = json.optDouble("hourlyRate", 0.0),
                            yearsExperience = json.optInt("yearsExperience", 0)
                        )
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding tutor by ID: ${e.message}", e)
                handleNetworkError(e, "finding tutor by ID")
            }
        }
    }

    /**
     * Gets the user ID associated with a tutor profile ID
     * This is important because many API endpoints (like createTutoringSession) expect a userId 
     * but our app often works with tutorId (profile ID) which is different 
     * @param tutorId The tutor profile ID (not user ID)
     * @return Result containing the userId if found
     */
    suspend fun getUserIdFromTutorId(tutorId: Long): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting userId for tutorId: $tutorId")
                
                // Use the findTutorById method to get the full tutor profile
                val tutorResult = findTutorById(tutorId)
                
                return@withContext tutorResult.fold(
                    onSuccess = { tutorProfile ->
                        val userId = tutorProfile?.userId
                        if (userId != null && userId > 0) {
                            Log.d(TAG, "Successfully found userId: $userId for tutorId: $tutorId")
                            Result.success(userId)
                        } else {
                            Log.e(TAG, "No userId found for tutorId: $tutorId")
                            Result.failure(Exception("No user ID found for tutor ID: $tutorId"))
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error getting userId for tutorId: $tutorId - ${error.message}")
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting userId for tutorId: $tutorId - ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Update a tutor's location
     * @param tutorId ID of the tutor profile to update
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Result<Unit> indicating success or failure
     */
    suspend fun updateTutorLocation(tutorId: Long, latitude: Double, longitude: Double): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Create URL with query parameters instead of using request body
                val urlWithParams = URL("$BASE_URL/tutors/updateLocation/$tutorId?latitude=$latitude&longitude=$longitude")
                val connection = createPutConnection(urlWithParams)

                return@withContext handleResponse(connection, null) { _ -> Unit }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating tutor location: ${e.message}", e)
                handleNetworkError(e, "updating tutor location")
            }
        }
    }

    /**
     * Get reviews for a tutor
     * @param tutorId ID of the tutor to get reviews for
     * @return Result<List<Review>> containing the tutor's reviews
     */
    suspend fun getTutorReviews(tutorId: Long): Result<List<Review>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/reviews/tutor/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val reviews = mutableListOf<Review>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)

                        reviews.add(
                            Review(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                studentId = json.getLong("studentId"),
                                rating = json.getInt("rating"),
                                comment = json.getString("comment"),
                                dateCreated = json.getString("dateCreated")
                            )
                        )
                    }
                    reviews
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting tutor reviews: ${e.message}", e)
                handleNetworkError(e, "getting tutor reviews")
            }
        }
    }

    /**
     * Create a new review for a tutor
     * @param review Review object containing review details
     * @return Result<Review> containing the created review
     */
    suspend fun createReview(review: Review): Result<Review> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/reviews/create")
                val connection = createPostConnection(url)

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("tutorId", review.tutorId)
                    put("studentId", review.studentId)
                    put("rating", review.rating)
                    put("comment", review.comment)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    Review(
                        id = json.getLong("id"),
                        tutorId = json.getLong("tutorId"),
                        studentId = json.getLong("studentId"),
                        rating = json.getInt("rating"),
                        comment = json.getString("comment"),
                        dateCreated = json.getString("dateCreated")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating review: ${e.message}", e)
                handleNetworkError(e, "creating review")
            }
        }
    }

    /**
     * Find tutors by expertise or subject
     * @param query Search query for expertise or subject area
     * @return Result<List<TutorProfile>> containing matching tutors
     */
    suspend fun findTutorsByExpertise(query: String): Result<List<TutorProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = URL("$BASE_URL/tutors/search?query=$encodedQuery")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val tutors = mutableListOf<TutorProfile>()

                    for (i in 0 until jsonArray.length()) {
                        val tutorJson = jsonArray.getJSONObject(i)

                        // Extract tutor name with fallback mechanisms
                        var displayName = tutorJson.optString("name", "")

                        // Try user object if name is empty
                        if (displayName.isEmpty() && tutorJson.has("user")) {
                            try {
                                val userObj = tutorJson.getJSONObject("user")
                                val firstName = userObj.optString("firstName", "")
                                val lastName = userObj.optString("lastName", "")

                                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                    displayName = "$firstName $lastName".trim()
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "Error extracting name from user object: ${e.message}")
                            }
                        }

                        // Try direct fields if still empty
                        if (displayName.isEmpty()) {
                            val firstName = tutorJson.optString("firstName", "")
                            val lastName = tutorJson.optString("lastName", "")

                            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                displayName = "$firstName $lastName".trim()
                            } else {
                                // Use username as last resort
                                displayName = tutorJson.optString("username", 
                                    "Tutor #${tutorJson.optLong("id")}")
                            }
                        }

                        // Clean up display name
                        if (displayName.contains("@")) {
                            displayName = "Tutor #${tutorJson.optLong("id")}"
                        }

                        // Get subjects
                        val subjects = mutableListOf<String>()
                        if (tutorJson.has("subjects")) {
                            try {
                                val subjectsArray = tutorJson.getJSONArray("subjects")
                                for (j in 0 until subjectsArray.length()) {
                                    subjects.add(subjectsArray.getString(j))
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "Error extracting subjects: ${e.message}")
                            }
                        }

                        // If no subjects found, try expertise
                        if (subjects.isEmpty()) {
                            val expertise = tutorJson.optString("expertise", "")
                            if (expertise.isNotEmpty()) {
                                subjects.addAll(expertise.split(",").map { it.trim() })
                            }
                        }

                        // Get email
                        var email = ""
                        if (tutorJson.has("user")) {
                            try {
                                val userObj = tutorJson.getJSONObject("user")
                                email = userObj.optString("email", "")
                            } catch (e: Exception) {
                                Log.d(TAG, "Error extracting email: ${e.message}")
                            }
                        }
                        if (email.isEmpty()) {
                            email = tutorJson.optString("email", "")
                        }

                        tutors.add(
                            TutorProfile(
                                id = tutorJson.optLong("id"),
                                userId = tutorJson.optLong("userId", 0).let { 
                                    if (it > 0) it else null 
                                },
                                name = displayName,
                                email = email,
                                bio = tutorJson.optString("bio", ""),
                                rating = tutorJson.optDouble("rating", 0.0).toFloat(),
                                subjects = subjects,
                                education = tutorJson.optString("education", ""),
                                hourlyRate = tutorJson.optDouble("hourlyRate", 0.0),
                                yearsExperience = tutorJson.optInt("yearsExperience", 0)
                            )
                        )
                    }

                    tutors
                }
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException -> Log.e(TAG, "Connection timed out. Server may be slow or unresponsive")
                    else -> Log.e(TAG, "Unexpected error type: ${e.javaClass.simpleName}")
                }

                handleNetworkError(e, "finding tutors by expertise")
            }
        }
    }

    /**
     * Get all tutor profiles
     * @return Result<List<TutorProfile>> containing all tutor profiles
     */
    suspend fun getAllTutorProfiles(): Result<List<TutorProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                // Call the tutors API endpoint to get all tutors
                val url = URL("$BASE_URL/tutors")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val tutors = mutableListOf<TutorProfile>()

                    for (i in 0 until jsonArray.length()) {
                        val tutorJson = jsonArray.getJSONObject(i)

                        // Parse subjects array
                        val subjectsArray = tutorJson.optJSONArray("subjects")
                        val subjects = mutableListOf<String>()
                        if (subjectsArray != null) {
                            for (j in 0 until subjectsArray.length()) {
                                subjects.add(subjectsArray.getString(j))
                            }
                        }

                        val firstName = tutorJson.optString("firstName", "")
                        val lastName = tutorJson.optString("lastName", "")
                        val fullName = if (firstName.isNotBlank() && lastName.isNotBlank()) {
                            "$firstName $lastName"
                        } else {
                            tutorJson.optString("username", "Unknown")
                        }

                        // Extract latitude and longitude if available
                        val latitude = if (tutorJson.has("latitude")) tutorJson.optDouble("latitude") else null
                        val longitude = if (tutorJson.has("longitude")) tutorJson.optDouble("longitude") else null

                        tutors.add(
                            TutorProfile(
                                id = tutorJson.getLong("profileId"),
                                userId = tutorJson.optLong("userId"),
                                name = fullName,
                                email = tutorJson.optString("username", ""),
                                bio = tutorJson.optString("bio", ""),
                                rating = tutorJson.optDouble("rating", 0.0).toFloat(),
                                subjects = subjects,
                                education = tutorJson.optString("education", ""),
                                hourlyRate = tutorJson.optDouble("hourlyRate", 0.0),
                                yearsExperience = tutorJson.optInt("yearsExperience", 0),
                                latitude = latitude,
                                longitude = longitude
                            )
                        )
                    }

                    tutors
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting all tutor profiles: ${e.message}", e)

                when (e) {
                    is ConnectException -> Log.e(TAG, "Connection error - unable to connect to server")
                    is SocketTimeoutException -> Log.e(TAG, "Connection timed out. Server may be slow or unresponsive")
                    else -> Log.e(TAG, "Unexpected error type: ${e.javaClass.simpleName}")
                }

                return@withContext handleNetworkError(e, "getting all tutor profiles")
            }
        }
    }

    /**
     * Get random tutors
     * @param limit Maximum number of tutors to return (default is 10)
     * @return Result<List<TutorProfile>> containing random tutors
     */
    suspend fun getRandomTutors(limit: Int = 10): Result<List<TutorProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                // Call the random tutors API endpoint
                val url = URL("$BASE_URL/tutors/random?limit=$limit")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val tutors = mutableListOf<TutorProfile>()

                    for (i in 0 until jsonArray.length()) {
                        val tutorJson = jsonArray.getJSONObject(i)

                        // Parse subjects array
                        val subjectsArray = tutorJson.optJSONArray("subjects")
                        val subjects = mutableListOf<String>()
                        if (subjectsArray != null) {
                            for (j in 0 until subjectsArray.length()) {
                                subjects.add(subjectsArray.getString(j))
                            }
                        }

                        val firstName = tutorJson.optString("firstName", "")
                        val lastName = tutorJson.optString("lastName", "")
                        val fullName = if (firstName.isNotBlank() && lastName.isNotBlank()) {
                            "$firstName $lastName"
                        } else {
                            tutorJson.optString("username", "Unknown")
                        }

                        tutors.add(
                            TutorProfile(
                                id = tutorJson.getLong("profileId"),
                                name = fullName,
                                email = tutorJson.optString("username", ""),
                                bio = tutorJson.optString("bio", ""),
                                rating = tutorJson.optDouble("rating", 0.0).toFloat(),
                                subjects = subjects,
                                hourlyRate = tutorJson.optDouble("hourlyRate", 0.0)
                            )
                        )
                    }

                    tutors
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting random tutors: ${e.message}", e)

                when (e) {
                    is ConnectException -> Log.e(TAG, "Connection error - unable to connect to server")
                    is SocketTimeoutException -> Log.e(TAG, "Connection timed out. Server may be slow or unresponsive")
                    else -> Log.e(TAG, "Unexpected error type: ${e.javaClass.simpleName}")
                }

                return@withContext handleNetworkError(e, "getting random tutors")
            }
        }
    }

    /**
     * Get subject by ID - using the CourseDTO model
     */
    suspend fun getSubjectById(subjectId: Long): CourseDTO? {
        return withContext(Dispatchers.IO) {
            try {
                // The backend doesn't have a direct /api/subjects/{id} endpoint
                // Try first to search for the subject
                val params = mapOf("query" to subjectId.toString())
                val url = createUrlWithParams("$BASE_URL/subjects/search", params)
                val connection = createGetConnection(url)

                try {
                    val result = handleResponse(connection) { response ->
                        val jsonArray = parseJsonArrayResponse(response)

                        // If we found any subjects, try to find one with matching ID
                        for (i in 0 until jsonArray.length()) {
                            val json = jsonArray.getJSONObject(i)
                            val id = json.optLong("id")
                            if (id == subjectId) {
                                return@handleResponse CourseDTO(
                                    id = id,
                                    title = json.optString("subject", "Unknown Subject"),
                                    subtitle = "Course by tutor #" + json.optLong("tutorProfileId"),
                                    description = "Subject offered by tutor",
                                    tutorId = json.optLong("tutorProfileId"),
                                    tutorName = "Tutor #" + json.optLong("tutorProfileId"), // Use default name instead of null
                                    category = "Subject",
                                    price = 0.0, // We don't have the hourly rate in this response
                                    createdAt = parseDate(json.optString("createdAt"))
                                )
                            }
                        }

                        null // No matching subject found
                    }

                    if (result.isSuccess && result.getOrNull() != null) {
                        return@withContext result.getOrNull()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching for subject by ID: ${e.message}", e)
                    // Continue to fallback methods
                }

                // Create a default subject if the API call fails
                // Note: Course API has been removed, so we're providing a fallback
                Log.d(TAG, "Creating fallback subject for ID: $subjectId")
                return@withContext CourseDTO(
                    id = subjectId,
                    title = "Course #$subjectId",
                    subtitle = "Course information",
                    description = "Course information unavailable",
                    tutorId = null,
                    tutorName = null,
                    category = "General",
                    price = 0.0,
                    createdAt = Date()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting subject by ID: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Helper function to parse date strings
     */
    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            format.parse(dateString)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            null
        }
    }

    /**
     * Create a GET connection to the specified URL
     * @param url URL to connect to
     * @return HttpURLConnection
     */
    private fun createGetConnection(url: URL): HttpURLConnection {
        // Log the full URL to help diagnose connection issues
        Log.d(TAG, "Creating GET connection to URL: ${url.toString()}")
        val serverUrl = DEPLOYED_BACKEND_URL
        Log.d(TAG, "Using deployed backend URL: $serverUrl")

        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 15000 // 15 seconds timeout
            connection.readTimeout = 15000 // 15 seconds read timeout
            connection.doInput = true
            // Don't call connect() here - let the caller do it when ready

            return connection
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create connection to $url: ${e.message}", e)
            throw e
        }
    }

    /**
     * Public accessor for creating a GET connection
     * @param url URL to connect to
     * @return HttpURLConnection
     */
    fun getConnection(url: URL): HttpURLConnection {
        return createGetConnection(url)
    }

    /**
     * Helper method to create an HttpURLConnection for POST requests
     * @param url The URL to connect to
     * @return The configured HttpURLConnection
     */
    private fun createPostConnection(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        return connection
    }

    /**
     * Public accessor for creating a POST connection
     * @param url URL to connect to
     * @return HttpURLConnection
     */
    fun postConnection(url: URL): HttpURLConnection {
        return createPostConnection(url)
    }

    /**
     * Helper method to create an HttpURLConnection for PUT requests
     * @param url The URL to connect to
     * @return The configured HttpURLConnection
     */
    private fun createPutConnection(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "PUT"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        return connection
    }

    /**
     * Public accessor for creating a PUT connection
     * @param url URL to connect to
     * @return HttpURLConnection
     */
    fun putConnection(url: URL): HttpURLConnection {
        return createPutConnection(url)
    }

    /**
     * Helper method to create an HttpURLConnection for DELETE requests
     * @param url The URL to connect to
     * @return The configured HttpURLConnection
     */
    private fun createDeleteConnection(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        return connection
    }

    /**
     * Public accessor for creating a DELETE connection
     * @param url URL to connect to
     * @return HttpURLConnection
     */
    fun deleteConnection(url: URL): HttpURLConnection {
        return createDeleteConnection(url)
    }

    /**
     * Helper method to create a URL with query parameters
     * @param baseUrl The base URL without query parameters
     * @param params Map of query parameters
     * @return URL object with query parameters
     */
    private fun createUrlWithParams(baseUrl: String, params: Map<String, String>): URL {
        val sb = StringBuilder(baseUrl)
        if (params.isNotEmpty()) {
            sb.append("?")
            params.entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    sb.append("&")
                }
                sb.append(URLEncoder.encode(entry.key, "UTF-8"))
                sb.append("=")
                sb.append(URLEncoder.encode(entry.value, "UTF-8"))
            }
        }
        val urlString = sb.toString()
        Log.d(TAG, "Created URL with params: $urlString")
        return URL(urlString)
    }

    /**
     * Helper method to handle HTTP responses
     * @param connection The HttpURLConnection to read from
     * @param requestBody Optional request body for POST/PUT requests
     * @param handler Lambda to process the response string
     * @return Result containing the processed response or an error
     */
    private inline fun <T> handleResponse(
        connection: HttpURLConnection,
        requestBody: String? = null,
        crossinline handler: (String) -> T
    ): Result<T> {
        var responseCode: Int
        try {
            // Log connection details
            Log.d(TAG, "Making ${connection.requestMethod} request to ${connection.url}")

            // Log headers for debugging
            val requestProperties = connection.requestProperties
            Log.d(TAG, "Request headers: $requestProperties")

            // Write request body if provided
            if (requestBody != null) {
                Log.d(TAG, "Sending request body: $requestBody")
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestBody)
                writer.flush()
                writer.close()
            } else {
                // For GET requests, explicitly connect if no request body is provided
                connection.connect()
            }

            // Get response code
            responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            // Read response
            val reader = if (responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream))
            } else {
                Log.e(TAG, "Error response code: $responseCode")
                BufferedReader(InputStreamReader(connection.errorStream ?: return Result.failure(
                    Exception("HTTP Error: $responseCode")
                )))
            }

            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val responseStr = response.toString()
            Log.d(TAG, "Response body: $responseStr")

            // If response code is not successful, return failure
            if (responseCode !in 200..299) {
                Log.e(TAG, "HTTP Error: $responseCode - $responseStr")
                return Result.failure(Exception("HTTP Error: $responseCode - $responseStr"))
            }

            // Process response with handler
            try {
                val result = handler(responseStr)
                Log.d(TAG, "Processed response successfully: $result")
                return Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing response: ${e.message}", e)
                return Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleResponse: ${e.message}", e)
            return Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Public accessor for handling HTTP responses
     * @param connection The HttpURLConnection to read from
     * @param requestBody Optional request body for POST/PUT requests
     * @param handler Lambda to process the response string
     * @return Result containing the processed response or an error
     */
    fun <T> processResponse(
        connection: HttpURLConnection,
        requestBody: String? = null,
        handler: (String) -> T
    ): Result<T> {
        return handleResponse(connection, requestBody, handler)
    }

    /**
     * Helper method to handle network errors
     * @param e The exception that occurred
     * @param operation Description of the operation that failed
     * @return Result.failure with appropriate error message
     */
    private fun <T> handleNetworkError(e: Exception, operation: String): Result<T> {
        when (e) {
            is ConnectException -> {
                Log.e(TAG, "Connection error while $operation: ${e.message}")
            }
            is SocketTimeoutException -> {
                Log.e(TAG, "Connection timeout while $operation: ${e.message}")
            }
            else -> {
                Log.e(TAG, "Error while $operation: ${e.message}")
            }
        }
        return Result.failure(e)
    }

    /**
     * Helper method to parse a JSON response string into a JSONObject
     * @param response The JSON response string
     * @return JSONObject parsed from the response
     */
    private fun parseJsonResponse(response: String): JSONObject {
        return try {
            val jsonObject = JSONObject(response)
            Log.d(TAG, "Successfully parsed JSON response: ${jsonObject.toString(2)}")

            // Log all keys found in the JSON for debugging
            val keys = mutableListOf<String>()
            jsonObject.keys().forEach { keys.add(it) }
            Log.d(TAG, "JSON keys found: $keys")

            jsonObject
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON response: ${e.message}", e)
            Log.e(TAG, "Raw response content: $response")
            JSONObject()
        }
    }

    /**
     * Helper method to parse a JSON response string into a JSONArray
     * @param response The JSON response string
     * @param arrayKey Optional key to extract a specific array from the response
     * @return JSONArray parsed from the response
     */
    private fun parseJsonArrayResponse(response: String, arrayKey: String? = null): JSONArray {
        return try {
            // Check if response is an empty array
            if (response.trim() == "[]") {
                return JSONArray()
            }

            // Check if response is already a JSON array (starts with '[' and ends with ']')
            val trimmedResponse = response.trim()
            if (trimmedResponse.startsWith("[") && trimmedResponse.endsWith("]")) {
                return JSONArray(trimmedResponse)
            }

            if (arrayKey != null) {
                val json = JSONObject(response)
                json.getJSONArray(arrayKey)
            } else {
                JSONArray(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON array response: ${e.message}", e)
            JSONArray()
        }
    }

    /**
     * Get all tutoring sessions for a tutor
     * @param tutorId The ID of the tutor
     * @return List of tutoring sessions
     */
    suspend fun getTutorSessions(tutorId: String): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching sessions for tutor with ID: $tutorId")
                val url = URL("$BASE_URL/tutoring-sessions/findByUser/$tutorId")
                val connection = createGetConnection(url)

                val result = handleApiResponse(connection)
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    val jsonArray = parseJsonArrayResponse(response)
                    val sessions = mutableListOf<TutoringSession>()

                    // Debug: Print the entire response to check for session type
                    Log.d(TAG, "==== RAW SESSIONS RESPONSE ====")
                    Log.d(TAG, response)
                    Log.d(TAG, "==== END RAW RESPONSE ====")

                    for (i in 0 until jsonArray.length()) {
                        val sessionJson = jsonArray.getJSONObject(i)
                        
                        // Debug log for session type - enhanced version
                        val sessionId = sessionJson.optLong("sessionId", sessionJson.optLong("id"))
                        
                        // Extra debugging to check the raw JSON for session type
                        Log.d(TAG, "==== RAW SESSION JSON ====")
                        Log.d(TAG, sessionJson.toString(2))
                        Log.d(TAG, "==== END SESSION JSON ====")
                        
                        val sessionType = sessionJson.optString("sessionType")
                        Log.d(TAG, "Session $sessionId raw JSON sessionType: '$sessionType'")
                        
                        // Add debug log to check for any manipulation
                        val finalSessionType = sessionType // No transformation - use exactly as is
                        Log.d(TAG, "Session $sessionId final sessionType (unmodified): '$finalSessionType'")
                        
                        val session = TutoringSession(
                            id = sessionId,
                            tutorId = sessionJson.optLong("tutorId"),
                            studentId = sessionJson.optString("studentId", ""), // Using studentId from backend
                            startTime = sessionJson.optString("startTime"),
                            endTime = sessionJson.optString("endTime"),
                            status = sessionJson.optString("status"),
                            subject = sessionJson.optString("subject"),
                            sessionType = finalSessionType, // Use exactly what's in the JSON
                            notes = sessionJson.optString("notes", ""),
                            tutorName = sessionJson.optString("tutorName", ""),
                            studentName = sessionJson.optString("studentName", ""),
                            conversationId = sessionJson.optLong("conversationId")
                        )
                        
                        // Debug log after session creation
                        Log.d(TAG, "Created session object with type: '${session.sessionType}'")
                        
                        sessions.add(session)
                    }

                    Log.d(TAG, "Successfully fetched ${sessions.size} sessions for tutor $tutorId")
                    Result.success(sessions)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown error fetching tutor sessions"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching tutor sessions: ${e.message}", e)
                handleNetworkError(e, "fetching tutor sessions")
            }
        }
    }

    /**
     * Get upcoming tutoring sessions for a tutor (status: Scheduled or Confirmed)
     * @param tutorId The ID of the tutor
     * @return List of upcoming tutoring sessions
     */
    suspend fun getUpcomingTutorSessions(tutorId: String): Result<List<TutoringSession>> {
        return try {
            Log.d(TAG, "Fetching upcoming sessions for tutor with ID: $tutorId")
            val allSessionsResult = getTutorSessions(tutorId)

            if (allSessionsResult.isFailure) {
                return allSessionsResult
            }

            val allSessions = allSessionsResult.getOrNull() ?: emptyList()

            // Filter sessions with status "Scheduled" or "Confirmed"
            val upcomingSessions = allSessions.filter { 
                it.status.equals("Scheduled", ignoreCase = true) || 
                it.status.equals("Confirmed", ignoreCase = true) 
            }

            Log.d(TAG, "Filtered ${upcomingSessions.size} upcoming sessions from ${allSessions.size} total")
            Result.success(upcomingSessions)
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching upcoming tutor sessions: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Helper method to create a proper API URL, ensuring correct formatting
     * @param endpoint The API endpoint path
     * @return Properly formatted URL string
     */
    private fun createApiUrl(endpoint: String): String {
        // Remove any leading slash from the endpoint if present
        val cleanEndpoint = if (endpoint.startsWith("/")) endpoint.substring(1) else endpoint

        // Create a properly formatted URL
        return "$BASE_URL/$cleanEndpoint"
    }

    /**
     * Public accessor for creating API URLs
     * @param endpoint The API endpoint path
     * @return Properly formatted URL string
     */
    fun getApiUrl(endpoint: String): String {
        return createApiUrl(endpoint)
    }

    /**
     * Get all availability slots for a tutor
     * @param tutorId The ID of the tutor
     * @return List of tutor availability slots
     */
    suspend fun getTutorAvailability(tutorId: Long): Result<List<TutorAvailability>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutor-availability/findByUser/$tutorId")
                val connection = createGetConnection(url)

                handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val availabilitySlots = mutableListOf<TutorAvailability>()

                    for (i in 0 until jsonArray.length()) {
                        val slotJson = jsonArray.getJSONObject(i)
                        availabilitySlots.add(
                            TutorAvailability(
                                id = slotJson.optLong("availabilityId", slotJson.optLong("id")),
                                tutorId = slotJson.optLong("userId", slotJson.optLong("tutorId", 0)), // Try userId first, then tutorId as fallback
                                dayOfWeek = slotJson.optString("dayOfWeek"),
                                startTime = slotJson.optString("startTime"),
                                endTime = slotJson.optString("endTime")
                            )
                        )
                    }
                    availabilitySlots
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tutor availability: ${e.message}", e)
                handleNetworkError(e, "fetching tutor availability")
            }
        }
    }

    /**
     * Get all availability slots for a tutor on a specific day of the week
     * @param tutorId The ID of the tutor
     * @param dayOfWeek The day of the week, e.g., "MONDAY"
     * @return List of tutor availability slots for the specified day
     */
    suspend fun getTutorAvailabilityByDay(tutorId: Long, dayOfWeek: String): Result<List<TutorAvailability>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting availability for day of week: $dayOfWeek")
                val url = URL("$BASE_URL/tutor-availability/findByUserAndDay/$tutorId/$dayOfWeek")
                Log.d(TAG, "Creating GET connection to URL: $url")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val availabilitySlots = mutableListOf<TutorAvailability>()

                    for (i in 0 until jsonArray.length()) {
                        val slotJson = jsonArray.getJSONObject(i)
                        availabilitySlots.add(
                            TutorAvailability(
                                id = slotJson.optLong("availabilityId", slotJson.optLong("id")),
                                tutorId = slotJson.optLong("userId", slotJson.optLong("tutorId", 0)), // Try userId first, then tutorId as fallback
                                dayOfWeek = slotJson.optString("dayOfWeek"),
                                startTime = slotJson.optString("startTime"),
                                endTime = slotJson.optString("endTime")
                            )
                        )
                    }
                    availabilitySlots
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get availability data for day $dayOfWeek, returning empty list")
                // Return empty list instead of mock data
                return@withContext Result.success(emptyList())
            }
        }
    }

    /**
     * Create a new availability slot for a tutor
     * @param tutorId The ID of the tutor
     * @param dayOfWeek The day of the week (e.g., MONDAY, TUESDAY)
     * @param startTime The start time (e.g., "9:00 AM")
     * @param endTime The end time (e.g., "5:00 PM")
     * @return The created availability slot
     */
    suspend fun createTutorAvailability(
        tutorId: Long,
        dayOfWeek: String,
        startTime: String,
        endTime: String
    ): Result<TutorAvailability> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutor-availability/createAvailability")
                val connection = createPostConnection(url)

                // Create JSON payload
                val jsonObject = JSONObject()
                jsonObject.put("userId", tutorId)  // Using userId as expected by the backend
                jsonObject.put("dayOfWeek", dayOfWeek)
                jsonObject.put("startTime", startTime)
                jsonObject.put("endTime", endTime)

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutorAvailability(
                        id = json.optLong("availabilityId", json.optLong("id")),
                        tutorId = json.optLong("userId", json.optLong("tutorId", 0)), // Map userId to tutorId when parsing response
                        dayOfWeek = json.optString("dayOfWeek"),
                        startTime = json.optString("startTime"),
                        endTime = json.optString("endTime")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating tutor availability: ${e.message}", e)
                handleNetworkError(e, "creating tutor availability")
            }
        }
    }

    /**
     * Update an existing availability slot
     * @param id The ID of the availability slot
     * @param tutorId The ID of the tutor
     * @param dayOfWeek The day of the week (e.g., MONDAY, TUESDAY)
     * @param startTime The start time (e.g., "9:00 AM")
     * @param endTime The end time (e.g., "5:00 PM")
     * @return The updated availability slot
     */
    suspend fun updateTutorAvailability(
        id: Long,
        tutorId: Long,
        dayOfWeek: String,
        startTime: String,
        endTime: String
    ): Result<TutorAvailability> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutor-availability/updateAvailability/$id")
                val connection = createPutConnection(url)

                // Create JSON payload
                val jsonObject = JSONObject()
                jsonObject.put("availabilityId", id)
                jsonObject.put("userId", tutorId)  // Using userId as expected by the backend
                jsonObject.put("dayOfWeek", dayOfWeek)
                jsonObject.put("startTime", startTime)
                jsonObject.put("endTime", endTime)

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutorAvailability(
                        id = json.optLong("availabilityId", json.optLong("id")),
                        tutorId = json.optLong("userId", json.optLong("tutorId", 0)), // Map userId to tutorId when parsing response
                        dayOfWeek = json.optString("dayOfWeek"),
                        startTime = json.optString("startTime"),
                        endTime = json.optString("endTime")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating tutor availability: ${e.message}", e)
                handleNetworkError(e, "updating tutor availability")
            }
        }
    }

    /**
     * Delete an availability slot
     * @param id The ID of the availability slot
     * @return Success or failure
     */
    suspend fun deleteTutorAvailability(id: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutor-availability/deleteAvailability/$id")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting tutor availability: ${e.message}", e)
                handleNetworkError(e, "deleting tutor availability")
            }
        }
    }

    /**
     * Delete all availability slots for a tutor
     * @param tutorId The ID of the tutor
     * @return Success or failure
     */
    suspend fun deleteAllTutorAvailability(tutorId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutor-availability/deleteAllForTutor/$tutorId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all tutor availability: ${e.message}", e)
                handleNetworkError(e, "deleting all tutor availability")
            }
        }
    }

    /**
     * Create a new tutoring session
     * @param tutorId ID of the tutor user account (userId, not profile ID)
     * @param studentId Student ID
     * @param startTime Start time of the session
     * @param endTime End time of the session
     * @param subject Subject for the session
     * @param sessionType Type of session (e.g., "Online", "In-Person") 
     * @param notes Additional notes for the session
     * @param latitude Optional latitude for in-person sessions
     * @param longitude Optional longitude for in-person sessions
     * @param locationName Optional location name for in-person sessions
     * @param locationData Legacy location data string
     * @return Result containing the created TutoringSession
     */
    suspend fun createTutoringSession(
        tutorId: Long,
        studentId: Long,
        startTime: String,
        endTime: String,
        subject: String,
        sessionType: String,
        notes: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        locationData: String? = null
    ): Result<TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/tutoring-sessions/createSession")
                val connection = createPostConnection(url)

                // Create the request body
                val jsonObject = JSONObject().apply {
                    put("userId", tutorId)  // This is the userId of the tutor, not the profile ID
                    put("studentId", studentId) // Using consistent studentId terminology
                    put("startTime", startTime)
                    put("endTime", endTime)
                    put("status", "PENDING") // Status is now PENDING until tutor accepts
                    put("subject", subject)
                    put("sessionType", sessionType)
                    notes?.let { put("notes", it) }
                    // Add location fields for in-person sessions
                    if (sessionType == "In-Person") {
                        latitude?.let { put("latitude", it) }
                        longitude?.let { put("longitude", it) }
                        locationName?.let { put("locationName", it) }
                        // For backward compatibility
                        if (!locationData.isNullOrEmpty()) {
                            put("locationData", locationData)
                        }
                    }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    TutoringSession(
                        id = json.optLong("sessionId", json.optLong("id")),
                        tutorId = json.optLong("tutorId"),
                        studentId = json.optString("studentId"), // Using consistent studentId terminology
                        startTime = json.optString("startTime"),
                        endTime = json.optString("endTime"),
                        status = json.optString("status"),
                        subject = json.optString("subject"),
                        sessionType = json.optString("sessionType"),
                        notes = json.optString("notes", ""),
                        tutorName = json.optString("tutorName", ""),
                        conversationId = json.optLong("conversationId"),
                        price = if (json.has("price") && !json.isNull("price")) json.getDouble("price") else null,
                        latitude = if (json.has("latitude") && !json.isNull("latitude")) json.getDouble("latitude") else null,
                        longitude = if (json.has("longitude") && !json.isNull("longitude")) json.getDouble("longitude") else null,
                        locationName = json.optString("locationName"),
                        locationData = json.optString("locationData")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating tutoring session: ${e.message}", e)
                handleNetworkError(e, "creating tutoring session")
            }
        }
    }

    /**
     * Get all availability slots for a tutor on a specific date
     * @param tutorId The ID of the tutor
     * @param date The specific date in yyyy-MM-dd format
     * @return List of tutor availability slots for the specified date
     */
    suspend fun getTutorAvailabilityByDate(tutorId: Long, date: String): Result<List<TutorAvailability>> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert the date to a day of week
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                calendar.time = dateFormat.parse(date) ?: Date()

                val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "MONDAY"
                    Calendar.TUESDAY -> "TUESDAY"
                    Calendar.WEDNESDAY -> "WEDNESDAY"
                    Calendar.THURSDAY -> "THURSDAY"
                    Calendar.FRIDAY -> "FRIDAY"
                    Calendar.SATURDAY -> "SATURDAY"
                    Calendar.SUNDAY -> "SUNDAY"
                    else -> "MONDAY" // Default to Monday if parsing fails
                }

                Log.d(TAG, "Getting availability for day of week: $dayOfWeek from date: $date")

                // Get availability by day of week directly
                val dayResult = getTutorAvailabilityByDay(tutorId, dayOfWeek)
                if (dayResult.isSuccess) {
                    val availability = dayResult.getOrNull() ?: emptyList()
                    Log.d(TAG, "Successfully retrieved ${availability.size} availability slots for $dayOfWeek")
                    return@withContext Result.success(availability)
                } else {
                    // If API call failed, return empty list
                    Log.e(TAG, "Failed to get availability data for day $dayOfWeek, returning empty list")
                    return@withContext Result.success(emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tutor availability by date: ${e.message}", e)
                return@withContext Result.success(emptyList())
            }
        }
    }

    /**
     * Helper method to return fallback mock availability data for a specific day
     * Only used when all API calls fail
     */
    private fun getFallbackAvailabilityForDay(tutorId: Long, dayOfWeek: String): List<TutorAvailability> {
        // Log that we're using mock data as fallback
        Log.w(TAG, "Using mock availability data for $dayOfWeek as fallback")

        // Return empty list instead of mock data
        return emptyList()
    }

    /**
     * Try to fetch conversations using a different URL pattern that might be more compatible with the backend
     */
    private suspend fun tryAlternativeEndpoint(userId: Long): Result<List<Conversation>> {
        return try {
            // Try a completely different API endpoint that might exist
            val url = URL(createApiUrl("users/$userId/conversations"))
            Log.d(TAG, "Trying alternative URL: $url")
            val connection = createGetConnection(url)
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseConversationsResponse(response)
            } else {
                Result.failure(Exception("Alternative endpoint failed with code $responseCode"))
            }
        } catch (e: Exception) {
            Log.d(TAG, "Alternative endpoint approach failed: ${e.message}")
            Result.failure(e)
        }
    }


    /**
     * Find a user by ID
     * @param userId ID of the user to find
     * @return Result<User> containing the user if found
     */
    suspend fun findUserById(userId: Long): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/users/findById/$userId")
                val connection = createGetConnection(url)

                handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)

                    // Use the role directly from the backend without mapping
                    val role = json.optString("role", "")

                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = json.optString("password", json.optString("passwordHash", "")),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = role
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding user by ID")
            }
        }
    }

    /**
     * Get conversations for a tutor 
     * @param tutorId ID of the tutor
     * @return Result<List<Conversation>> containing the tutor's conversations
     */
    suspend fun getConversationsByTutorId(tutorId: Long): Result<List<Conversation>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(createApiUrl("conversations/findByTutor/$tutorId"))
                Log.d(TAG, "Creating GET connection to URL: $url")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val conversations = mutableListOf<Conversation>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)

                        // Extract IDs - map from backend's user1/user2 to student/tutor if needed
                        val studentId = json.optLong("studentId", -1L)
                        val tutorId = json.optLong("tutorId", -1L)

                        // Handle the case where backend uses user1/user2 instead of student/tutor
                        val finalStudentId = if (studentId != -1L) studentId else json.optLong("user1Id")
                        val finalTutorId = if (tutorId != -1L) tutorId else json.optLong("user2Id")

                        // Extract names - prioritize specific names, fallback to user1/user2 names
                        var studentName = json.optString("studentName", "")
                        var tutorName = json.optString("tutorName", "")

                        // If student/tutor names are empty, try user1/user2 names
                        if (studentName.isEmpty()) {
                            studentName = json.optString("user1Name", "Unknown Student")
                        }

                        if (tutorName.isEmpty()) {
                            tutorName = json.optString("user2Name", "Unknown Tutor")
                        }

                        Log.d(TAG, "Tutor conversation: id=${json.optLong("id")}, studentId=$finalStudentId, tutorId=$finalTutorId")
                        Log.d(TAG, "Names: studentName=$studentName, tutorName=$tutorName")

                        conversations.add(
                            Conversation(
                                id = json.optLong("id"),
                                studentId = finalStudentId,
                                tutorId = finalTutorId,
                                studentName = studentName,
                                tutorName = tutorName,
                                lastMessage = json.optString("lastMessage"),
                                lastMessageTime = json.optString("lastMessageTime"),
                                unreadCount = json.optInt("unreadCount", 0),
                                createdAt = json.optString("createdAt", ""),
                                updatedAt = json.optString("updatedAt", "")
                            )
                        )
                    }
                    conversations
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting conversations by tutor ID: ${e.message}", e)
                handleNetworkError(e, "getting conversations by tutor ID")
            }
        }
    }

    /**
     * Get conversations for a student
     * @param studentId ID of the student
     * @return Result<List<Conversation>> containing the student's conversations
     */
    suspend fun getConversationsByStudentId(studentId: Long): Result<List<Conversation>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(createApiUrl("conversations/findByStudent/$studentId"))
                Log.d(TAG, "Creating GET connection to URL: $url")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val conversations = mutableListOf<Conversation>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)

                        // Extract IDs - map from backend's user1/user2 to student/tutor if needed
                        val studentId = json.optLong("studentId", -1L)
                        val tutorId = json.optLong("tutorId", -1L)

                        // Handle the case where backend uses user1/user2 instead of student/tutor
                        val finalStudentId = if (studentId != -1L) studentId else json.optLong("user1Id")
                        val finalTutorId = if (tutorId != -1L) tutorId else json.optLong("user2Id")

                        // Extract names - prioritize specific names, fallback to user1/user2 names
                        var studentName = json.optString("studentName", "")
                        var tutorName = json.optString("tutorName", "")

                        // If student/tutor names are empty, try user1/user2 names
                        if (studentName.isEmpty()) {
                            studentName = json.optString("user1Name", "Unknown Student")
                        }

                        if (tutorName.isEmpty()) {
                            tutorName = json.optString("user2Name", "Unknown Tutor")
                        }

                        Log.d(TAG, "Student conversation: id=${json.optLong("id")}, studentId=$finalStudentId, tutorId=$finalTutorId")
                        Log.d(TAG, "Names: studentName=$studentName, tutorName=$tutorName")

                        conversations.add(
                            Conversation(
                                id = json.optLong("id"),
                                studentId = finalStudentId,
                                tutorId = finalTutorId,
                                studentName = studentName,
                                tutorName = tutorName,
                                lastMessage = json.optString("lastMessage"),
                                lastMessageTime = json.optString("lastMessageTime"),
                                unreadCount = json.optInt("unreadCount", 0),
                                createdAt = json.optString("createdAt", ""),
                                updatedAt = json.optString("updatedAt", "")
                            )
                        )
                    }
                    conversations
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting conversations by student ID: ${e.message}", e)
                handleNetworkError(e, "getting conversations by student ID")
            }
        }
    }

    /**
     * Update names for an existing conversation
     * @param conversationId ID of the conversation to update
     * @return Result<Conversation> containing the updated conversation with correct names
     */
    suspend fun updateConversationNames(conversationId: Long): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(createApiUrl("conversations/updateNames/$conversationId"))
                Log.d(TAG, "Creating PUT connection to URL: $url")
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection, "") { response ->
                    Log.d(TAG, "Received update conversation names response: $response")
                    val json = parseJsonResponse(response)

                    // Extract IDs - map from backend's user1/user2 to student/tutor if needed
                    val studentId = json.optLong("studentId", -1L)
                    val tutorId = json.optLong("tutorId", -1L)

                    // Handle the case where backend uses user1/user2 instead of student/tutor
                    val finalStudentId = if (studentId != -1L) studentId else json.optLong("user1Id")
                    val finalTutorId = if (tutorId != -1L) tutorId else json.optLong("user2Id")

                    // Extract names - prioritize specific names, fallback to user1/user2 names
                    var studentName = json.optString("studentName", "")
                    var tutorName = json.optString("tutorName", "")

                    // If student/tutor names are empty, try user1/user2 names
                    if (studentName.isEmpty()) {
                        studentName = json.optString("user1Name", "Unknown Student")
                    }

                    if (tutorName.isEmpty()) {
                        tutorName = json.optString("user2Name", "Unknown Tutor")
                    }

                    Log.d(TAG, "Updated conversation names: studentName=$studentName, tutorName=$tutorName")

                    Conversation(
                        id = conversationId,
                        studentId = finalStudentId,
                        tutorId = finalTutorId,
                        studentName = studentName,
                        tutorName = tutorName,
                        createdAt = json.optString("createdAt", ""),
                        updatedAt = json.optString("updatedAt", "")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update conversation names", e)
                handleNetworkError(e, "updating conversation names")
            }
        }
    }

    /**
     * Get a conversation by its ID
     * @param conversationId ID of the conversation
     * @return Result<Conversation> containing the conversation
     */
    suspend fun getConversation(conversationId: Long): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting conversation with ID: $conversationId")

                // The correct endpoint should be specifed - try these in sequence
                // The error suggests there's confusion with static resources
                val endpointOptions = listOf(
                    "conversations/getConversation/$conversationId",
                    "conversations/findById/$conversationId",
                    "conversations/get/$conversationId",
                    "conversations/conversation/$conversationId"
                )

                var lastException: Exception? = null

                // Try each endpoint option
                for (endpoint in endpointOptions) {
                    try {
                        val url = URL("$BASE_URL/$endpoint")
                        Log.d(TAG, "Trying URL: $url")

                        val connection = createGetConnection(url)
                        connection.connectTimeout = 10000 // 10 seconds
                        connection.readTimeout = 10000 // 10 seconds
                        connection.setRequestProperty("User-Agent", "Judify-Android-App")

                        // Connect and get response code
                        connection.connect()
                        val responseCode = connection.responseCode
                        Log.d(TAG, "Response code for $endpoint: $responseCode")

                        if (responseCode in 200..299) {
                            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                            Log.d(TAG, "Response body: $responseBody")

                            val json = parseJsonResponse(responseBody)

                            // Extract student and tutor IDs
                            val studentId = json.optLong("studentId", json.optLong("user1Id", 0))
                            val tutorId = json.optLong("tutorId", json.optLong("user2Id", 0))

                            // Extract names
                            val studentName = json.optString("studentName", json.optString("user1Name", ""))
                            val tutorName = json.optString("tutorName", json.optString("user2Name", ""))

                            Log.d(TAG, "Parsed conversation: studentId=$studentId, tutorId=$tutorId, studentName=$studentName, tutorName=$tutorName")

                            return@withContext Result.success(
                                Conversation(
                                    id = json.optLong("id"),
                                    studentId = studentId,
                                    tutorId = tutorId,
                                    studentName = studentName,
                                    tutorName = tutorName,
                                    lastMessage = json.optString("lastMessage"),
                                    lastMessageTime = json.optString("lastMessageTime"),
                                    unreadCount = json.optInt("unreadCount", 0),
                                    createdAt = json.optString("createdAt", ""),
                                    updatedAt = json.optString("updatedAt", "")
                                )
                            )
                        } else {
                            // Log error but continue trying other endpoints
                            val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details available"
                            Log.e(TAG, "Error response for $endpoint: $errorResponse")
                            lastException = Exception("HTTP error: $responseCode - $errorResponse")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error with $endpoint request: ${e.message}", e)
                        lastException = e
                    }
                }

                // If we get here, all endpoints failed
                return@withContext Result.failure(lastException ?: Exception("All conversation endpoints failed"))
            } catch (e: Exception) {
                Log.e(TAG, "Error getting conversation by ID: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Create a new tutoring session using Flow for easier handling in the ViewModel
     * This wraps the existing createTutoringSession method
     * 
     * @param studentId Student ID
     * @param tutorId Tutor ID
     * @param startTime Start time of the session (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param endTime End time of the session (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param subject Subject for the session
     * @param sessionType Type of session (e.g., "Online", "In-Person")
     * @param location Optional location for the session
     * @param notes Additional notes for the session
     * @return Flow with Result containing the created TutoringSession
     */
    fun createSession(
        studentId: String,
        tutorId: Long,
        startTime: String,
        endTime: String,
        subject: String,
        sessionType: String,
        location: String = "",
        notes: String
    ): Flow<Result<TutoringSession>> = flow {
        try {
            Log.d(TAG, "Creating session: student=$studentId, tutor=$tutorId, subject=$subject")

            // Convert studentId to Long
            val studentIdLong = studentId.toLongOrNull() ?: -1L
            if (studentIdLong == -1L) {
                throw IllegalArgumentException("Invalid student ID: $studentId")
            }

            // Create the session using the existing method
            val result = createTutoringSession(
                tutorId = tutorId,
                studentId = studentIdLong,
                startTime = startTime,
                endTime = endTime,
                subject = subject,
                sessionType = sessionType,
                notes = notes
            )

            // Emit the result
            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error in createSession flow: ${e.message}", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Checks if there are any approved sessions that overlap with the given time range for a specific tutor
     * @param tutorId The ID of the tutor
     * @param startTime The start time of the session in ISO format (yyyy-MM-dd'T'HH:mm:ss)
     * @param endTime The end time of the session in ISO format (yyyy-MM-dd'T'HH:mm:ss)
     * @return Result containing a Boolean indicating whether there is an overlap (true) or not (false)
     */
    suspend fun checkSessionOverlap(tutorId: Long, startTime: String, endTime: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = createApiUrl("/api/tutoring-sessions/checkOverlap")
                val params = mapOf(
                    "tutorId" to tutorId.toString(),
                    "startTime" to startTime,
                    "endTime" to endTime
                )
                val url = createUrlWithParams(baseUrl, params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    response.toBoolean()
                }
            } catch (e: Exception) {
                Log.e("NetworkUtils", "Error checking session overlap: ${e.message}", e)
                return@withContext Result.failure(e)
            }
        }
    }

    /**
     * Get all tutoring sessions for a student
     * @param studentId The ID of the student
     * @return List of tutoring sessions
     */
    suspend fun getStudentSessions(studentId: String): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching sessions for student with ID: $studentId")
                val url = URL("$BASE_URL/tutoring-sessions/findByUser/$studentId")
                val connection = createGetConnection(url)

                var result = handleApiResponse(connection)

                // If first endpoint fails, try alternative endpoints
                if (result.isFailure) {
                    Log.d(TAG, "First endpoint failed, trying alternative endpoint")
                    val alternativeUrl = URL("$BASE_URL/tutoring-sessions/student/$studentId")
                    val alternativeConnection = createGetConnection(alternativeUrl)
                    result = handleApiResponse(alternativeConnection)

                    // If second endpoint fails, try another alternative
                    if (result.isFailure) {
                        Log.d(TAG, "Second endpoint failed, trying third alternative endpoint")
                        val alternativeUrl2 = URL("$BASE_URL/tutoring-sessions/findByStudent/$studentId")
                        val alternativeConnection2 = createGetConnection(alternativeUrl2)
                        result = handleApiResponse(alternativeConnection2)

                        // If all previous fail, try one final alternative
                        if (result.isFailure) {
                            Log.d(TAG, "Third endpoint failed, trying final alternative endpoint")
                            val finalUrl = URL("$BASE_URL/sessions/student/$studentId")
                            val finalConnection = createGetConnection(finalUrl)
                            result = handleApiResponse(finalConnection)
                        }
                    }
                }

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    val jsonArray = parseJsonArrayResponse(response)
                    val sessions = mutableListOf<TutoringSession>()

                    for (i in 0 until jsonArray.length()) {
                        val sessionJson = jsonArray.getJSONObject(i)

                        // Extract tutorName from various possible JSON structures
                        var tutorName = sessionJson.optString("tutorName", "")

                        // If tutorName isn't directly in the session, try to get it from tutor object
                        if (tutorName.isEmpty() && sessionJson.has("tutor")) {
                            try {
                                val tutorObj = sessionJson.getJSONObject("tutor")
                                val firstName = tutorObj.optString("firstName", "")
                                val lastName = tutorObj.optString("lastName", "")

                                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                    tutorName = "$firstName $lastName".trim()
                                } else {
                                    // Try other possible fields
                                    tutorName = tutorObj.optString("name", tutorObj.optString("username", ""))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error extracting tutor name from tutor object: ${e.message}")
                            }
                        }

                        // If tutorName is still empty, try tutorFirstName/tutorLastName
                        if (tutorName.isEmpty()) {
                            val tutorFirstName = sessionJson.optString("tutorFirstName", "")
                            val tutorLastName = sessionJson.optString("tutorLastName", "")

                            if (tutorFirstName.isNotEmpty() || tutorLastName.isNotEmpty()) {
                                tutorName = "$tutorFirstName $tutorLastName".trim()
                            }
                        }

                        sessions.add(
                            TutoringSession(
                                id = sessionJson.optLong("sessionId", sessionJson.optLong("id")),
                                tutorId = sessionJson.optLong("tutorId"),
                                studentId = sessionJson.optString("studentId", ""), // Using studentId from backend
                                startTime = sessionJson.optString("startTime"),
                                endTime = sessionJson.optString("endTime"),
                                status = sessionJson.optString("status"),
                                subject = sessionJson.optString("subject"),
                                sessionType = sessionJson.optString("sessionType"),
                                notes = sessionJson.optString("notes", ""),
                                tutorName = tutorName,
                                conversationId = sessionJson.optLong("conversationId")
                            )
                        )
                    }

                    Log.d(TAG, "Successfully fetched ${sessions.size} sessions for student $studentId")
                    Result.success(sessions)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown error fetching student sessions"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching student sessions: ${e.message}", e)
                handleNetworkError(e, "fetching student sessions")
            }
        }
    }

    /**
     * Backward compatibility method - Get all tutoring sessions for a learner (student)
     * @param studentId The ID of the student (previously called learnerId)
     * @return List of tutoring sessions
     * @deprecated Use getStudentSessions instead as it aligns with backend terminology
     */
    @Deprecated("Use getStudentSessions instead to align with backend terminology", 
                ReplaceWith("getStudentSessions(studentId)"))
    suspend fun getLearnerSessions(studentId: String): Result<List<TutoringSession>> {
        // Call the new method for backward compatibility
        return getStudentSessions(studentId)
    }

    /**
     * Get all tutoring sessions for a student by ID
     * @param studentId The ID of the student
     * @return List of tutoring sessions
     */
    suspend fun getStudentSessionsById(studentId: String): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching sessions for student with ID: $studentId")
                val url = URL("$BASE_URL/tutoring-sessions/findByUser/$studentId")
                val connection = createGetConnection(url)

                var result = handleApiResponse(connection)

                // If first endpoint fails, try alternative endpoints
                if (result.isFailure) {
                    Log.d(TAG, "First endpoint failed, trying alternative endpoint")
                    val alternativeUrl = URL("$BASE_URL/tutoring-sessions/student/$studentId")
                    val alternativeConnection = createGetConnection(alternativeUrl)
                    result = handleApiResponse(alternativeConnection)

                    // If second endpoint fails, try another alternative
                    if (result.isFailure) {
                        Log.d(TAG, "Second endpoint failed, trying third alternative endpoint")
                        val alternativeUrl2 = URL("$BASE_URL/tutoring-sessions/findByStudent/$studentId")
                        val alternativeConnection2 = createGetConnection(alternativeUrl2)
                        result = handleApiResponse(alternativeConnection2)

                        // If all previous fail, try one final alternative
                        if (result.isFailure) {
                            Log.d(TAG, "Third endpoint failed, trying final alternative endpoint")
                            val finalUrl = URL("$BASE_URL/sessions/student/$studentId")
                            val finalConnection = createGetConnection(finalUrl)
                            result = handleApiResponse(finalConnection)
                        }
                    }
                }

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    val jsonArray = parseJsonArrayResponse(response)
                    val sessions = mutableListOf<TutoringSession>()

                    for (i in 0 until jsonArray.length()) {
                        val sessionJson = jsonArray.getJSONObject(i)

                        // Extract tutorName from various possible JSON structures
                        var tutorName = sessionJson.optString("tutorName", "")

                        // If tutorName isn't directly in the session, try to get it from tutor object
                        if (tutorName.isEmpty() && sessionJson.has("tutor")) {
                            try {
                                val tutorObj = sessionJson.getJSONObject("tutor")
                                val firstName = tutorObj.optString("firstName", "")
                                val lastName = tutorObj.optString("lastName", "")

                                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                    tutorName = "$firstName $lastName".trim()
                                } else {
                                    // Try other possible fields
                                    tutorName = tutorObj.optString("name", tutorObj.optString("username", ""))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error extracting tutor name from tutor object: ${e.message}")
                            }
                        }

                        // If tutorName is still empty, try tutorFirstName/tutorLastName
                        if (tutorName.isEmpty()) {
                            val tutorFirstName = sessionJson.optString("tutorFirstName", "")
                            val tutorLastName = sessionJson.optString("tutorLastName", "")

                            if (tutorFirstName.isNotEmpty() || tutorLastName.isNotEmpty()) {
                                tutorName = "$tutorFirstName $tutorLastName".trim()
                            }
                        }

                        sessions.add(
                            TutoringSession(
                                id = sessionJson.optLong("sessionId", sessionJson.optLong("id")),
                                tutorId = sessionJson.optLong("tutorId"),
                                studentId = sessionJson.optString("studentId", ""), // Using consistent studentId terminology
                                startTime = sessionJson.optString("startTime"),
                                endTime = sessionJson.optString("endTime"),
                                status = sessionJson.optString("status"),
                                subject = sessionJson.optString("subject"),
                                sessionType = sessionJson.optString("sessionType"),
                                notes = sessionJson.optString("notes", ""),
                                tutorName = tutorName,
                                conversationId = sessionJson.optLong("conversationId")
                            )
                        )
                    }

                    Log.d(TAG, "Successfully fetched ${sessions.size} sessions for student $studentId")
                    Result.success(sessions)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown error fetching student sessions"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching student sessions: ${e.message}", e)
                handleNetworkError(e, "fetching student sessions")
            }
        }
    }

    /**
     * Update the status of a tutoring session
     * @param sessionId The ID of the session to update
     * @param newStatus The new status for the session (e.g., "APPROVED", "REJECTED", "COMPLETED")
     * @return The updated tutoring session
     */
    suspend fun updateSessionStatus(sessionId: Long, newStatus: String): Result<TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating session status: sessionId=$sessionId, newStatus=$newStatus")
                val url = URL("$BASE_URL/tutoring-sessions/updateStatus/$sessionId")
                val connection = createPutConnection(url)

                // Set the new status as the request body
                val result = handleApiResponse(connection, newStatus)

                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    val sessionJson = parseJsonResponse(response)

                    // Extract tutorName from various possible JSON structures
                    var tutorName = sessionJson.optString("tutorName", "")

                    // If tutorName isn't directly in the session, try to get it from tutor object
                    if (tutorName.isEmpty() && sessionJson.has("tutor")) {
                        try {
                            val tutorObj = sessionJson.getJSONObject("tutor")
                            val firstName = tutorObj.optString("firstName", "")
                            val lastName = tutorObj.optString("lastName", "")

                            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                tutorName = "$firstName $lastName".trim()
                            } else {
                                // Try other possible fields
                                tutorName = tutorObj.optString("name", tutorObj.optString("username", ""))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error extracting tutor name from tutor object: ${e.message}")
                        }
                    }

                    // If tutorName is still empty, try tutorFirstName/tutorLastName
                    if (tutorName.isEmpty()) {
                        val tutorFirstName = sessionJson.optString("tutorFirstName", "")
                        val tutorLastName = sessionJson.optString("tutorLastName", "")

                        if (tutorFirstName.isNotEmpty() || tutorLastName.isNotEmpty()) {
                            tutorName = "$tutorFirstName $tutorLastName".trim()
                        }
                    }

                    val session = TutoringSession(
                        id = sessionJson.optLong("sessionId", sessionJson.optLong("id")),
                        tutorId = sessionJson.optLong("tutorId"),
                        studentId = sessionJson.optString("studentId", ""),
                        startTime = sessionJson.optString("startTime"),
                        endTime = sessionJson.optString("endTime"),
                        status = sessionJson.optString("status"),
                        subject = sessionJson.optString("subject"),
                        sessionType = sessionJson.optString("sessionType"),
                        notes = sessionJson.optString("notes", ""),
                        tutorName = tutorName,
                        conversationId = sessionJson.optLong("conversationId")
                    )

                    Log.d(TAG, "Successfully updated session status: ${session.id} to ${session.status}")
                    Result.success(session)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown error updating session status"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating session status: ${e.message}", e)
                handleNetworkError(e, "updating session status")
            }
        }
    }
}
