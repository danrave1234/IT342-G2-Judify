package com.mobile.utils

import android.util.Log
import com.mobile.data.model.SubjectDTO
import com.mobile.data.model.TutorRegistration
import com.mobile.data.model.User
import kotlinx.coroutines.Dispatchers
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

/**
 * Utility class for handling network operations
 * This class provides methods to interact with the backend API
 * All methods are suspend functions that run on IO dispatcher
 * Each method includes detailed error logging
 */
object NetworkUtils {

    private const val TAG = "NetworkUtils"

    // Debug mode flag - set to false for production
    // Using real data from the server for production environment
    // Note: If server connection issues occur, check the DEFAULT_SERVER_IP
    // to ensure it's set to the correct IP address of the server
    //NEVER TURN THIS TO TRUE
    private const val DEBUG_MODE = false

    // Server configuration
    // Use your computer's IP address for physical device (e.g., "192.168.1.100")
    // DONT EVER REPLACE THE SERVER IP AND THIS IS THE PRIMARY IP
    private const val DEFAULT_SERVER_IP = "192.168.1.10" // Default IP for physical devices
    private const val EMULATOR_SERVER_IP = "10.0.2.2" // IP for emulator (points to host machine)
    private const val SERVER_PORT = "8080"
    private const val API_PATH = "api"

    // List of IPs to try in order
    private val SERVER_IPS = listOf(DEFAULT_SERVER_IP, EMULATOR_SERVER_IP)

    // Base URL with fallback mechanism
    private fun getBaseUrl(index: Int = 0): String {
        val ip = if (index < SERVER_IPS.size) SERVER_IPS[index] else DEFAULT_SERVER_IP
        // Remove the trailing slash so we can consistently add it when constructing URLs
        return "http://$ip:$SERVER_PORT/$API_PATH"
    }

    // Current active IP index
    private var currentIpIndex = 0

    private val BASE_URL: String
        get() = getBaseUrl(currentIpIndex)

    // Try next IP if current one fails
    private fun tryNextIp() {
        currentIpIndex = (currentIpIndex + 1) % SERVER_IPS.size
        Log.d(TAG, "Switching to next IP: ${SERVER_IPS[currentIpIndex]}")
    }

    // Mock data for testing - kept for dev purposes
    private object MockData {
        val mockMessage = Message(
            id = 1,
            conversationId = 1,
            senderId = 1,
            content = "Test message",
            timestamp = "2024-03-18T10:00:00",
            isRead = false
        )

        val mockNotification = Notification(
            id = 1,
            userId = 1,
            type = "TEST",
            content = "Test notification",
            timestamp = "2024-03-18T10:00:00",
            isRead = false
        )

        val mockNotifications = listOf(
            Notification(
                id = 1,
                userId = 1,
                type = "TEST",
                content = "Test notification 1",
                timestamp = "2024-03-18T10:00:00",
                isRead = false
            ),
            Notification(
                id = 2,
                userId = 1,
                type = "TEST",
                content = "Test notification 2",
                timestamp = "2024-03-18T11:00:00",
                isRead = true
            )
        )

    }

    /**
     * Authentication response data class
     */
    data class AuthResponse(
        val success: Boolean = true,
        val isAuthenticated: Boolean,
        val userId: Long? = null,
        val username: String = "",
        val email: String = "",
        val firstName: String = "",
        val lastName: String = "",
        val role: String = "LEARNER",
        val token: String = ""
    )

    /**
     * Data class for Tutor Profile
     */
    data class TutorProfile(
        val id: Long,
        val name: String,
        val email: String,
        val bio: String,
        val rating: Float,
        val subjects: List<String>,
        val hourlyRate: Double
    )

    /**
     * Data class for Review
     */
    data class Review(
        val id: Long,
        val tutorId: Long,
        val learnerId: Long,
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
        val learnerId: String, // Make sure it's String to match Booking model
        val startTime: String,
        val endTime: String,
        val status: String,
        val subject: String,
        val sessionType: String,
        val notes: String?
    )

    /**
     * Data class for Conversation
     */
    data class Conversation(
        val id: Long,
        val participants: List<Long>,
        val lastMessage: String? = null,
        val lastMessageTime: String? = null,
        val unreadCount: Int = 0,
        val createdAt: String = "",
        val updatedAt: String? = null
    )

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
                if (DEBUG_MODE) {
                    // Check if email contains "tutor" to simulate tutor login
                    val role = if (email.contains("tutor", ignoreCase = true)) "TUTOR" else "LEARNER"
                    Log.d(TAG, "Using mock auth response for: $email with role: $role")
                    return@withContext Result.success(
                        AuthResponse(
                            isAuthenticated = true,
                            userId = 1,
                            username = email,
                            email = email,
                            firstName = "Test",
                            lastName = "User",
                            role = role,
                            token = "mock-jwt-token-for-testing"
                        )
                    )
                }

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
                            username = "",
                            email = "",
                            firstName = "",
                            lastName = "",
                            role = "LEARNER",
                            token = ""
                        )
                    }

                    // If authenticated, get all the user details
                    // Map backend role to frontend role (STUDENT -> LEARNER)
                    val backendRole = json.optString("role", "")
                    val frontendRole = when (backendRole) {
                        "STUDENT" -> "LEARNER"
                        else -> backendRole
                    }

                    AuthResponse(
                        isAuthenticated = true,
                        userId = json.optLong("userId"),
                        username = json.optString("username", ""),
                        email = json.optString("email", ""),
                        firstName = json.optString("firstName", ""),
                        lastName = json.optString("lastName", ""),
                        role = frontendRole,
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
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        User(
                            userId = 1,
                            email = user.email,
                            passwordHash = "hashed_password",
                            firstName = user.firstName,
                            lastName = user.lastName,
                            roles = user.roles
                        )
                    )
                }

                val url = URL("$BASE_URL/users/addUser")
                val connection = createPostConnection(url)

                // Map frontend role to backend role (LEARNER -> STUDENT)
                val backendRole = when (user.roles) {
                    "LEARNER" -> "STUDENT"
                    else -> user.roles
                }

                // Log a warning about sending plain passwords
                Log.w(TAG, "Warning: Sending plain password to server. Consider implementing proper password hashing on the client side.")

                val jsonObject = JSONObject().apply {
                    put("username", user.email) // Using email as username
                    put("email", user.email)
                    // Note: passwordHash is actually the plain password - server will hash it
                    put("password", user.passwordHash) 
                    put("firstName", user.firstName)
                    put("lastName", user.lastName)
                    put("role", backendRole) // Renamed from roles to role and mapped to backend role
                    user.profilePicture?.let { put("profilePicture", it) }
                    user.contactDetails?.let { put("contactDetails", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    // Map backend role to frontend role (STUDENT -> LEARNER)
                    val responseRole = json.optString("role", "")
                    val frontendRole = when (responseRole) {
                        "STUDENT" -> "LEARNER"
                        else -> responseRole
                    }

                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = json.optString("password", json.optString("passwordHash", "")),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = frontendRole
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
                if (DEBUG_MODE) {
                    return@withContext Result.success(true)
                }

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
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        TutorProfile(
                            id = 1L,
                            name = "John Doe",
                            email = "john.doe@example.com",
                            bio = "Experienced tutor in mathematics and physics",
                            rating = 4.5f,
                            subjects = listOf("Mathematics", "Physics"),
                            hourlyRate = 50.0
                        )
                    )
                }

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
                                .joinToString(" ") { it.capitalize() }
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
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        TutorProfile(
                            id = tutorId,
                            name = "John Doe",
                            email = "john.doe@example.com",
                            bio = "Experienced tutor in mathematics and physics",
                            rating = 4.5f,
                            subjects = listOf("Mathematics", "Physics"),
                            hourlyRate = 50.0
                        )
                    )
                }

                val url = URL("$BASE_URL/tutors/findById/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    // Log the raw JSON response for debugging
                    Log.d(TAG, "Tutor Profile JSON response: $json")

                    // Map the fields from the backend TutorProfileDTO to our TutorProfile model
                    val id = json.optLong("profileId", tutorId)

                    // Extract username directly from the response (based on the provided JSON example)
                    val username = json.optString("username", "")

                    // Process username to create a display name
                    var displayName = ""
                    if (username.isNotEmpty()) {
                        if (username.contains("@")) {
                            // Extract name from email
                            val emailParts = username.split("@")
                            if (emailParts.isNotEmpty()) {
                                val nameFromEmail = emailParts[0].replace(".", " ").split(" ")
                                    .joinToString(" ") { it.capitalize() }
                                displayName = nameFromEmail
                                Log.d(TAG, "Extracted name from email: $displayName")
                            }
                        } else {
                            // Use username directly if it's not an email
                            displayName = username
                            Log.d(TAG, "Using username as display name: $displayName")
                        }
                    }

                    // If we still don't have a name, try other fields
                    if (displayName.isEmpty()) {
                        // Check if the response has a user object that contains name fields
                        if (json.has("user")) {
                            try {
                                val userObj = json.getJSONObject("user")
                                val firstName = userObj.optString("firstName", "")
                                val lastName = userObj.optString("lastName", "")

                                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                    displayName = "$firstName $lastName".trim()
                                    Log.d(TAG, "Found name in user object: $displayName")
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "Error extracting name from user object: ${e.message}")
                            }
                        }

                        // If still no name, try direct fields
                        if (displayName.isEmpty()) {
                            val firstName = json.optString("firstName", "")
                            val lastName = json.optString("lastName", "")

                            if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                displayName = "$firstName $lastName".trim()
                            } else {
                                // Last resort, use a generic name
                                displayName = "Tutor #$id"
                            }
                        }
                    }

                    // Extract subjects array directly from the response
                    val subjectsList = if (json.has("subjects")) {
                        try {
                            val subjectsArray = json.getJSONArray("subjects")
                            (0 until subjectsArray.length()).map { subjectsArray.getString(it) }
                        } catch (e: Exception) {
                            Log.d(TAG, "Error extracting subjects array: ${e.message}")
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }

                    // If subjects list is empty, try expertise field
                    val finalSubjects = if (subjectsList.isEmpty()) {
                        val expertise = json.optString("expertise", "")
                        if (expertise.isNotEmpty()) {
                            expertise.split(",").map { it.trim() }
                        } else {
                            // Fallback to default subjects
                            listOf("Academic Support")
                        }
                    } else {
                        subjectsList
                    }

                    TutorProfile(
                        id = id,
                        name = displayName,
                        email = json.optString("email", ""),
                        bio = json.optString("bio", ""),
                        rating = json.optFloat("rating", 0.0f),
                        subjects = finalSubjects,
                        hourlyRate = json.optDouble("hourlyRate", 0.0)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting tutor profile: ${e.message}", e)
                handleNetworkError(e, "getting tutor profile")
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
                // Log server information
                Log.d(TAG, "Verifying connection to server: ${SERVER_IPS[0]}:$SERVER_PORT")
                Log.d(TAG, "Base URL: $BASE_URL")

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
     * Find conversations by user ID
     * @param userId ID of the user
     * @return Result<List<Conversation>> containing the user's conversations
     */
    suspend fun findConversationsByUser(userId: Long): Result<List<Conversation>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return mock conversations for testing
                    val mockConversations = listOf(
                        Conversation(
                            id = 1,
                            participants = listOf(userId, 2),
                            lastMessage = "Hello, how are you?",
                            lastMessageTime = "2024-03-20T14:30:00",
                            unreadCount = 1,
                            createdAt = "2024-03-20T10:00:00"
                        ),
                        Conversation(
                            id = 2,
                            participants = listOf(userId, 3),
                            lastMessage = "When is our next session?",
                            lastMessageTime = "2024-03-19T09:15:00",
                            unreadCount = 0,
                            createdAt = "2024-03-15T16:45:00"
                        )
                    )
                    return@withContext Result.success(mockConversations)
                }

                // Use the correct endpoint path as defined in the backend ConversationController
                val url = URL(createApiUrl("conversations/findByUser/$userId"))
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "conversations")
                    val conversations = mutableListOf<Conversation>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)

                        // Parse participants array
                        val participantsArray = json.getJSONArray("participants")
                        val participants = (0 until participantsArray.length()).map { 
                            participantsArray.getLong(it) 
                        }

                        conversations.add(
                            Conversation(
                                id = json.optLong("id", i.toLong()),
                                participants = participants,
                                lastMessage = json.optString("lastMessage", null),
                                lastMessageTime = json.optString("lastMessageTime", null),
                                unreadCount = json.optInt("unreadCount", 0),
                                createdAt = json.optString("createdAt", ""),
                                updatedAt = json.optString("updatedAt", null)
                            )
                        )
                    }
                    conversations
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding conversations for user")
            }
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
                if (DEBUG_MODE) {
                    // Return a mock conversation for testing
                    val mockConversation = Conversation(
                        id = System.currentTimeMillis(), // Use current time as mock ID
                        participants = participantIds,
                        createdAt = "2024-03-20T10:00:00"
                    )
                    return@withContext Result.success(mockConversation)
                }

                // Use the correct endpoint path as defined in the backend ConversationController
                val url = URL(createApiUrl("conversations/createConversation"))
                val connection = createPostConnection(url)

                // Create JSON array of participant IDs
                val participantsJsonArray = JSONArray()
                participantIds.forEach { participantsJsonArray.put(it) }

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("participants", participantsJsonArray)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    // Parse participants array
                    val participantsArray = json.getJSONArray("participants")
                    val participants = (0 until participantsArray.length()).map { 
                        participantsArray.getLong(it) 
                    }

                    Conversation(
                        id = json.optLong("id", System.currentTimeMillis()),
                        participants = participants,
                        createdAt = json.optString("createdAt", ""),
                        updatedAt = json.optString("updatedAt", null)
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "creating conversation")
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
                if (DEBUG_MODE) {
                    // Simulate success for testing
                    return@withContext Result.success(Unit)
                }

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
                if (DEBUG_MODE) {
                    // Return mock subjects for testing
                    return@withContext Result.success(
                        listOf(
                            TutorSubject(
                                id = 1,
                                tutorProfileId = tutorProfileId,
                                subject = "Mathematics",
                                createdAt = "2024-03-18T10:00:00"
                            ),
                            TutorSubject(
                                id = 2,
                                tutorProfileId = tutorProfileId,
                                subject = "Physics",
                                createdAt = "2024-03-18T10:00:00"
                            )
                        )
                    )
                }

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
                if (DEBUG_MODE) {
                    // Return mock subjects for testing
                    return@withContext Result.success(
                        listOf(
                            TutorSubject(
                                id = 1,
                                tutorProfileId = 1,
                                subject = "Mathematics",
                                createdAt = "2024-03-18T10:00:00"
                            ),
                            TutorSubject(
                                id = 2,
                                tutorProfileId = 2,
                                subject = "Physics",
                                createdAt = "2024-03-18T10:00:00"
                            )
                        )
                    )
                }

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
                if (DEBUG_MODE) {
                    // Return a mock subject for testing
                    return@withContext Result.success(
                        TutorSubject(
                            id = System.currentTimeMillis(),
                            tutorProfileId = tutorProfileId,
                            subject = subject,
                            createdAt = "2024-03-18T10:00:00"
                        )
                    )
                }

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
                if (DEBUG_MODE) {
                    // Return mock subjects for testing
                    val mockSubjects = mutableListOf<TutorSubject>()
                    subjects.forEachIndexed { index, subject ->
                        mockSubjects.add(
                            TutorSubject(
                                id = index.toLong() + 1,
                                tutorProfileId = tutorProfileId,
                                subject = subject,
                                createdAt = "2024-03-18T10:00:00"
                            )
                        )
                    }
                    return@withContext Result.success(mockSubjects)
                }

                val url = URL("$BASE_URL/subjects/add-multiple/$tutorProfileId")
                val connection = createPostConnection(url)

                // Create request body with JSON array of subjects
                val jsonArray = JSONArray()
                subjects.forEach { jsonArray.put(it) }

                return@withContext handleResponse(connection, jsonArray.toString()) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val addedSubjects = mutableListOf<TutorSubject>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
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
                if (DEBUG_MODE) {
                    // Simulate success for testing
                    return@withContext Result.success(Unit)
                }

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
                if (DEBUG_MODE) {
                    // Simulate success for testing
                    return@withContext Result.success(Unit)
                }

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
                if (DEBUG_MODE) {
                    // Return a mock subject for testing
                    return@withContext Result.success(
                        TutorSubject(
                            id = subjectId,
                            tutorProfileId = 1L, // Mock tutor profile ID
                            subject = subject,
                            createdAt = "2024-03-18T10:00:00"
                        )
                    )
                }

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
                if (DEBUG_MODE) {
                    // Return a mock user for testing
                    return@withContext Result.success(
                        User(
                            userId = 1L,
                            email = email,
                            passwordHash = "",
                            firstName = "Test",
                            lastName = "User",
                            roles = "TUTOR"
                        )
                    )
                }

                val params = mapOf("email" to email)
                val url = createUrlWithParams("$BASE_URL/users/find-by-email", params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)

                    // Map backend role to frontend role (STUDENT -> LEARNER)
                    val backendRole = json.optString("role", "")
                    val frontendRole = when (backendRole) {
                        "STUDENT" -> "LEARNER"
                        else -> backendRole
                    }

                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = json.optString("password", json.optString("passwordHash", "")),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = frontendRole
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
                if (DEBUG_MODE) {
                    // Return a mock updated user for testing
                    Log.d(TAG, "Mock update user: ID=${user.userId}, Name=${user.firstName} ${user.lastName}")
                    return@withContext Result.success(user)
                }

                val url = URL("$BASE_URL/users/update/${user.userId}")
                val connection = createPutConnection(url)

                // Map frontend role to backend role (LEARNER -> STUDENT)
                val backendRole = when (user.roles) {
                    "LEARNER" -> "STUDENT"
                    else -> user.roles
                }

                // Create request body with updated user information
                val jsonObject = JSONObject().apply {
                    put("userId", user.userId)
                    put("username", user.email) // Using email as username
                    put("email", user.email)
                    // Don't include password when updating user info
                    // Only include it when specifically changing password
                    put("firstName", user.firstName)
                    put("lastName", user.lastName)
                    put("role", backendRole)
                    user.profilePicture?.let { put("profilePicture", it) }
                    user.contactDetails?.let { put("contactDetails", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    // Map backend role to frontend role (STUDENT -> LEARNER)
                    val responseRole = json.optString("role", "")
                    val frontendRole = when (responseRole) {
                        "STUDENT" -> "LEARNER"
                        else -> responseRole
                    }

                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = "", // Don't store password hash in response
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = frontendRole
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
     * @param userId ID of the user to find the tutor profile for
     * @return Result<TutorProfile> containing the tutor profile if found
     */
    suspend fun findTutorByUserId(userId: Long): Result<TutorProfile> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return a mock tutor profile for testing
                    return@withContext Result.success(
                        TutorProfile(
                            id = userId,
                            name = "Test Tutor",
                            email = "test.tutor@example.com",
                            bio = "Experienced tutor in various subjects",
                            rating = 4.5f,
                            subjects = listOf("Mathematics", "Physics", "Programming"),
                            hourlyRate = 50.0
                        )
                    )
                }

                val url = URL("$BASE_URL/tutors/findByUserId/$userId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)

                    // Extract tutor profile ID
                    val profileId = json.optLong("profileId", json.optLong("id", userId))

                    // Extract tutor name
                    var displayName = ""

                    // Try to get name from user object
                    if (json.has("user")) {
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

                    // If no name from user object, try direct fields
                    if (displayName.isEmpty()) {
                        val firstName = json.optString("firstName", "")
                        val lastName = json.optString("lastName", "")

                        if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            displayName = "$firstName $lastName".trim()
                        } else {
                            // Use username if available
                            displayName = json.optString("username", "Tutor #$profileId")
                        }
                    }

                    // Clean up display name - make sure it doesn't contain email
                    if (displayName.contains("@")) {
                        displayName = "Tutor #$profileId"
                    }

                    TutorProfile(
                        id = profileId,
                        name = displayName,
                        email = json.optString("email", ""),
                        bio = json.optString("bio", ""),
                        rating = json.optFloat("rating", 0.0f),
                        subjects = json.optJSONArray("subjects")?.let { subjectsArray ->
                            (0 until subjectsArray.length()).map { subjectsArray.getString(it) }
                        } ?: run {
                            // If subjects array isn't available, try expertise field
                            val expertise = json.optString("expertise", "")
                            if (expertise.isNotEmpty()) {
                                listOf(expertise)
                            } else {
                                emptyList()
                            }
                        },
                        hourlyRate = json.optDouble("hourlyRate", 0.0)
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding tutor by user ID")
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
                if (DEBUG_MODE) {
                    // Simulate success for testing
                    Log.d(TAG, "Mock update tutor location: TutorID=$tutorId, Lat=$latitude, Long=$longitude")
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/tutors/updateLocation/$tutorId")
                val connection = createPutConnection(url)

                // Create request body with location data
                val jsonObject = JSONObject().apply {
                    put("latitude", latitude)
                    put("longitude", longitude)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { _ -> Unit }
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
                if (DEBUG_MODE) {
                    // Return mock reviews for testing
                    val mockReviews = listOf(
                        Review(
                            id = 1,
                            tutorId = tutorId,
                            learnerId = 1,
                            rating = 5,
                            comment = "Excellent tutor! Very knowledgeable and patient.",
                            dateCreated = "2024-05-01T14:30:00"
                        ),
                        Review(
                            id = 2,
                            tutorId = tutorId,
                            learnerId = 2,
                            rating = 4,
                            comment = "Great teaching style, helped me understand difficult concepts.",
                            dateCreated = "2024-04-22T09:15:00"
                        ),
                        Review(
                            id = 3,
                            tutorId = tutorId,
                            learnerId = 3,
                            rating = 5,
                            comment = "Very professional and knowledgeable. Highly recommended!",
                            dateCreated = "2024-04-15T16:45:00"
                        )
                    )
                    return@withContext Result.success(mockReviews)
                }

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
                                learnerId = json.getLong("learnerId"),
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
                if (DEBUG_MODE) {
                    // Return a mock review with generated ID for testing
                    val mockReview = review.copy(
                        id = System.currentTimeMillis(),
                        dateCreated = java.time.LocalDateTime.now().toString()
                    )
                    return@withContext Result.success(mockReview)
                }

                val url = URL("$BASE_URL/reviews/create")
                val connection = createPostConnection(url)

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("tutorId", review.tutorId)
                    put("learnerId", review.learnerId)
                    put("rating", review.rating)
                    put("comment", review.comment)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    Review(
                        id = json.getLong("id"),
                        tutorId = json.getLong("tutorId"),
                        learnerId = json.getLong("learnerId"),
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
                if (DEBUG_MODE) {
                    // Generate mock tutor profiles for testing
                    val mockTutors = listOf(
                        TutorProfile(
                            id = 1,
                            name = "John Smith",
                            email = "john.smith@example.com",
                            bio = "Experienced mathematics and physics tutor with 10 years of teaching experience",
                            rating = 4.8f,
                            subjects = listOf("Mathematics", "Physics", "Calculus"),
                            hourlyRate = 45.0
                        ),
                        TutorProfile(
                            id = 2,
                            name = "Sarah Johnson",
                            email = "sarah.johnson@example.com",
                            bio = "English literature specialist with a focus on creative writing and literary analysis",
                            rating = 4.9f,
                            subjects = listOf("English", "Literature", "Creative Writing"),
                            hourlyRate = 40.0
                        ),
                        TutorProfile(
                            id = 3,
                            name = "Michael Chen",
                            email = "michael.chen@example.com",
                            bio = "Computer science tutor specializing in programming and software development",
                            rating = 4.7f,
                            subjects = listOf("Computer Science", "Programming", "Java", "Python"),
                            hourlyRate = 55.0
                        ),
                        TutorProfile(
                            id = 4,
                            name = "Emily Rodriguez",
                            email = "emily.rodriguez@example.com",
                            bio = "Chemistry and biology tutor with a PhD in biochemistry",
                            rating = 4.6f,
                            subjects = listOf("Chemistry", "Biology", "Biochemistry"),
                            hourlyRate = 50.0
                        ),
                        TutorProfile(
                            id = 5,
                            name = "David Kim",
                            email = "david.kim@example.com",
                            bio = "History and social studies tutor with expertise in world history and political science",
                            rating = 4.5f,
                            subjects = listOf("History", "Social Studies", "Political Science"),
                            hourlyRate = 42.0
                        )
                    )

                    // Filter mock tutors based on search query
                    val lowerQuery = query.lowercase()
                    val filteredTutors = if (query.isEmpty()) {
                        mockTutors
                    } else {
                        mockTutors.filter { tutor ->
                            tutor.subjects.any { it.lowercase().contains(lowerQuery) } ||
                            tutor.bio.lowercase().contains(lowerQuery) ||
                            tutor.name.lowercase().contains(lowerQuery)
                        }
                    }

                    return@withContext Result.success(filteredTutors)
                }

                // Enhanced logging for API call
                Log.d(TAG, "Fetching tutors from API with query: '$query'")

                // Start with DEFAULT_SERVER_IP (index 0 in SERVER_IPS)
                currentIpIndex = 0
                var lastException: Exception? = null

                // Try each IP in the SERVER_IPS list
                for (ipIndex in SERVER_IPS.indices) {
                    currentIpIndex = ipIndex
                    val currentBaseUrl = getBaseUrl(ipIndex)
                    val serverIp = SERVER_IPS[ipIndex]

                    try {
                        Log.d(TAG, "Trying to connect using IP: $serverIp")
                        Log.d(TAG, "Base URL: $currentBaseUrl")

                        val params = mapOf("subject" to query)
                        val url = createUrlWithParams("$currentBaseUrl/tutors/searchBySubject", params)
                        Log.d(TAG, "Making request to: $url")

                        val connection = createGetConnection(url)

                        return@withContext handleResponse(connection) { response ->
                            val jsonArray = parseJsonArrayResponse(response)
                            Log.d(TAG, "Success! Received JSON array with ${jsonArray.length()} tutors")

                            val tutors = mutableListOf<TutorProfile>()

                            for (i in 0 until jsonArray.length()) {
                                val json = jsonArray.getJSONObject(i)

                                // Extract tutor name
                                var displayName = ""
                                val firstName = json.optString("firstName", "")
                                val lastName = json.optString("lastName", "")

                                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                                    displayName = "$firstName $lastName".trim()
                                } else {
                                    // Use username if available
                                    displayName = json.optString("username", "Tutor #${json.optLong("id", i.toLong())}")
                                }

                                // Clean up display name - make sure it doesn't contain email
                                if (displayName.contains("@")) {
                                    displayName = "Tutor #${json.optLong("id", i.toLong())}"
                                }

                                tutors.add(
                                    TutorProfile(
                                        id = json.optLong("id", i.toLong()),
                                        name = displayName,
                                        email = json.optString("email", ""),
                                        bio = json.optString("bio", ""),
                                        rating = json.optFloat("rating", 0.0f),
                                        subjects = json.optJSONArray("subjects")?.let { subjectsArray ->
                                            (0 until subjectsArray.length()).map { subjectsArray.getString(it) }
                                        } ?: run {
                                            // If subjects array isn't available, try expertise field
                                            val expertise = json.optString("expertise", "")
                                            if (expertise.isNotEmpty()) {
                                                listOf(expertise)
                                            } else {
                                                emptyList()
                                            }
                                        },
                                        hourlyRate = json.optDouble("hourlyRate", 0.0)
                                    )
                                )
                            }

                            Log.d(TAG, "Successfully processed ${tutors.size} tutors from API")
                            tutors
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error with IP ${SERVER_IPS[ipIndex]}: ${e.message}", e)
                        lastException = e
                        // Continue to the next IP
                    }
                }

                // If all IPs failed, return the error
                Log.e(TAG, "All connection attempts failed")
                return@withContext handleNetworkError(
                    lastException ?: Exception("Failed to connect to any server IP"),
                    "finding tutors by expertise"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error finding tutors by expertise: ${e.message}", e)

                // More detailed error logging
                when (e) {
                    is ConnectException -> Log.e(TAG, "Connection error - unable to connect to server")
                    is SocketTimeoutException -> Log.e(TAG, "Connection timed out. Server may be slow or unresponsive")
                    else -> Log.e(TAG, "Unexpected error type: ${e.javaClass.simpleName}")
                }

                return@withContext handleNetworkError(e, "finding tutors by expertise")
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
                if (DEBUG_MODE) {
                    // Generate mock tutor profiles for testing
                    val mockTutors = listOf(
                        TutorProfile(
                            id = 1,
                            name = "John Smith",
                            email = "john.smith@example.com",
                            bio = "Experienced mathematics and physics tutor with 10 years of teaching experience",
                            rating = 4.8f,
                            subjects = listOf("Mathematics", "Physics", "Calculus"),
                            hourlyRate = 45.0
                        ),
                        TutorProfile(
                            id = 2,
                            name = "Sarah Johnson",
                            email = "sarah.johnson@example.com",
                            bio = "English literature specialist with a focus on creative writing and literary analysis",
                            rating = 4.9f,
                            subjects = listOf("English", "Literature", "Creative Writing"),
                            hourlyRate = 40.0
                        ),
                        TutorProfile(
                            id = 3,
                            name = "Michael Chen",
                            email = "michael.chen@example.com",
                            bio = "Computer science tutor specializing in programming and software development",
                            rating = 4.7f,
                            subjects = listOf("Computer Science", "Programming", "Java", "Python"),
                            hourlyRate = 55.0
                        ),
                        TutorProfile(
                            id = 4,
                            name = "Emily Rodriguez",
                            email = "emily.rodriguez@example.com",
                            bio = "Chemistry and biology tutor with a PhD in biochemistry",
                            rating = 4.6f,
                            subjects = listOf("Chemistry", "Biology", "Biochemistry"),
                            hourlyRate = 50.0
                        ),
                        TutorProfile(
                            id = 5,
                            name = "David Kim",
                            email = "david.kim@example.com",
                            bio = "History and social studies tutor with expertise in world history and political science",
                            rating = 4.5f,
                            subjects = listOf("History", "Social Studies", "Political Science"),
                            hourlyRate = 42.0
                        )
                    )

                    // Shuffle the list to get random tutors
                    val randomTutors = mockTutors.shuffled().take(limit)
                    return@withContext Result.success(randomTutors)
                }

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
     * Get subject by ID - using the SubjectDTO model
     */
    suspend fun getSubjectById(subjectId: Long): SubjectDTO? {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return mock data for testing
                    return@withContext SubjectDTO(
                        id = subjectId,
                        name = "Mathematics",
                        description = "Advanced calculus and algebra",
                        tutorId = 1L,
                        tutorName = "John Doe",
                        category = "Science & Math",
                        hourlyRate = 45.0,
                        createdAt = Date()
                    )
                }

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
                                return@handleResponse SubjectDTO(
                                    id = id,
                                    name = json.optString("subject", "Unknown Subject"),
                                    description = "Subject offered by tutor",
                                    tutorId = json.optLong("tutorProfileId"),
                                    tutorName = "Tutor #" + json.optLong("tutorProfileId"), // Use default name instead of null
                                    category = "Subject",
                                    hourlyRate = 0.0, // We don't have the hourly rate in this response
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
                return@withContext SubjectDTO(
                    id = subjectId,
                    name = "Subject #$subjectId",
                    description = "Subject information unavailable",
                    tutorId = null,
                    tutorName = null,
                    category = "General",
                    hourlyRate = 0.0,
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
        val serverIp = SERVER_IPS[currentIpIndex]
        Log.d(TAG, "Using server IP: $serverIp")

        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 15000 // 15 seconds timeout
            connection.readTimeout = 15000 // 15 seconds read timeout
            connection.doInput = true
            connection.connect()

            val responseCode = connection.responseCode
            Log.d(TAG, "Connected successfully with response code: $responseCode")

            return connection
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to $url: ${e.message}", e)
            throw e
        }
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
        var responseCode = 0
        try {
            // Write request body if provided
            if (requestBody != null) {
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(requestBody)
                writer.flush()
                writer.close()
            }

            // Get response code
            responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            // Read response
            val reader = if (responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream))
            } else {
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
            Log.d(TAG, "Response: $responseStr")

            // If response code is not successful, return failure
            if (responseCode !in 200..299) {
                return Result.failure(Exception("HTTP Error: $responseCode - $responseStr"))
            }

            // Process response with handler
            return Result.success(handler(responseStr))
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleResponse: ${e.message}", e)
            return Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Helper method to handle network errors
     * @param e The exception that occurred
     * @param operation Description of the operation that failed
     * @return Result.failure with appropriate error message
     */
    private fun <T> handleNetworkError(e: Exception, operation: String): Result<T> {
        val errorMessage = when (e) {
            is ConnectException -> "Could not connect to server. Please check your internet connection."
            is SocketTimeoutException -> "Connection timed out. Please try again later."
            else -> "Error $operation: ${e.message}"
        }
        Log.e(TAG, errorMessage, e)
        return Result.failure(Exception(errorMessage, e))
    }

    /**
     * Helper method to parse a JSON response string into a JSONObject
     * @param response The JSON response string
     * @return JSONObject parsed from the response
     */
    private fun parseJsonResponse(response: String): JSONObject {
        return try {
            JSONObject(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON response: ${e.message}", e)
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
    suspend fun getTutorSessions(tutorId: Long): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return mock data for testing
                    return@withContext Result.success(
                        listOf(
                            TutoringSession(
                                id = 1,
                                tutorId = tutorId,
                                learnerId = "2",
                                startTime = "2025-05-15T14:00:00",
                                endTime = "2025-05-15T15:30:00",
                                status = "Scheduled",
                                subject = "Mathematics",
                                sessionType = "Online",
                                notes = "Introduction to Calculus"
                            ),
                            TutoringSession(
                                id = 2,
                                tutorId = tutorId,
                                learnerId = "3",
                                startTime = "2025-05-16T10:00:00",
                                endTime = "2025-05-16T11:30:00",
                                status = "Confirmed",
                                subject = "Physics",
                                sessionType = "In-person",
                                notes = "Mechanics review"
                            ),
                            TutoringSession(
                                id = 3,
                                tutorId = tutorId,
                                learnerId = "4",
                                startTime = "2025-05-18T16:00:00",
                                endTime = "2025-05-18T17:30:00",
                                status = "Scheduled",
                                subject = "Chemistry",
                                sessionType = "Online",
                                notes = "Organic chemistry basics"
                            )
                        )
                    )
                }

                // Use the correct endpoint from the Spring Boot controller
                val url = URL("$BASE_URL/tutoring-sessions/findByTutor/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val sessions = mutableListOf<TutoringSession>()

                    for (i in 0 until jsonArray.length()) {
                        val sessionJson = jsonArray.getJSONObject(i)
                        sessions.add(
                            TutoringSession(
                                id = sessionJson.optLong("id"),
                                tutorId = sessionJson.optLong("tutorId"),
                                learnerId = sessionJson.optString("studentId"), // Backend uses studentId instead of learnerId
                                startTime = sessionJson.optString("startTime"),
                                endTime = sessionJson.optString("endTime"),
                                status = sessionJson.optString("status"),
                                subject = sessionJson.optString("subject"),
                                sessionType = sessionJson.optString("sessionType"),
                                notes = sessionJson.optString("notes")
                            )
                        )
                    }
                    sessions
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tutor sessions: ${e.message}", e)
                handleNetworkError(e, "fetching tutor sessions")
            }
        }
    }

    /**
     * Get upcoming tutoring sessions for a tutor (status: Scheduled or Confirmed)
     * @param tutorId The ID of the tutor
     * @return List of upcoming tutoring sessions
     */
    suspend fun getUpcomingTutorSessions(tutorId: Long): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                val allSessionsResult = getTutorSessions(tutorId)

                if (allSessionsResult.isFailure) {
                    return@withContext allSessionsResult
                }

                val allSessions = allSessionsResult.getOrNull() ?: emptyList()

                // Filter sessions with status "Scheduled" or "Confirmed"
                val upcomingSessions = allSessions.filter { 
                    it.status.equals("Scheduled", ignoreCase = true) || 
                    it.status.equals("Confirmed", ignoreCase = true) 
                }

                Result.success(upcomingSessions)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching upcoming tutor sessions: ${e.message}", e)
                handleNetworkError(e, "fetching upcoming tutor sessions")
            }
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
     * Get all availability slots for a tutor
     * @param tutorId The ID of the tutor
     * @return List of tutor availability slots
     */
    suspend fun getTutorAvailability(tutorId: Long): Result<List<TutorAvailability>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return mock data for testing
                    return@withContext Result.success(
                        listOf(
                            TutorAvailability(
                                id = 1,
                                tutorId = tutorId,
                                dayOfWeek = "MONDAY",
                                startTime = "9:00 AM",
                                endTime = "5:00 PM"
                            ),
                            TutorAvailability(
                                id = 2,
                                tutorId = tutorId,
                                dayOfWeek = "TUESDAY",
                                startTime = "10:00 AM",
                                endTime = "6:00 PM"
                            ),
                            TutorAvailability(
                                id = 3,
                                tutorId = tutorId,
                                dayOfWeek = "WEDNESDAY",
                                startTime = "9:00 AM",
                                endTime = "3:00 PM"
                            )
                        )
                    )
                }

                // Use the correct endpoint from the Spring Boot controller
                val url = URL("$BASE_URL/tutor-availability/findByTutor/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val availabilitySlots = mutableListOf<TutorAvailability>()

                    for (i in 0 until jsonArray.length()) {
                        val slotJson = jsonArray.getJSONObject(i)
                        availabilitySlots.add(
                            TutorAvailability(
                                id = slotJson.optLong("id"),
                                tutorId = slotJson.optLong("tutorId"),
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
     * Get all availability slots for a tutor on a specific day
     * @param tutorId The ID of the tutor
     * @param dayOfWeek The day of the week (e.g., MONDAY, TUESDAY)
     * @return List of tutor availability slots for the specified day
     */
    suspend fun getTutorAvailabilityByDay(tutorId: Long, dayOfWeek: String): Result<List<TutorAvailability>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return mock data for testing
                    return@withContext Result.success(
                        listOf(
                            TutorAvailability(
                                id = 1,
                                tutorId = tutorId,
                                dayOfWeek = dayOfWeek,
                                startTime = "9:00 AM",
                                endTime = "5:00 PM"
                            )
                        )
                    )
                }

                // Use the correct endpoint from the Spring Boot controller
                val url = URL("$BASE_URL/tutor-availability/findByTutorAndDay/$tutorId/$dayOfWeek")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val availabilitySlots = mutableListOf<TutorAvailability>()

                    for (i in 0 until jsonArray.length()) {
                        val slotJson = jsonArray.getJSONObject(i)
                        availabilitySlots.add(
                            TutorAvailability(
                                id = slotJson.optLong("id"),
                                tutorId = slotJson.optLong("tutorId"),
                                dayOfWeek = slotJson.optString("dayOfWeek"),
                                startTime = slotJson.optString("startTime"),
                                endTime = slotJson.optString("endTime")
                            )
                        )
                    }
                    availabilitySlots
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tutor availability by day: ${e.message}", e)
                handleNetworkError(e, "fetching tutor availability by day")
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
                if (DEBUG_MODE) {
                    // Return mock data for testing
                    return@withContext Result.success(
                        TutorAvailability(
                            id = System.currentTimeMillis(), // Generate a random ID
                            tutorId = tutorId,
                            dayOfWeek = dayOfWeek,
                            startTime = startTime,
                            endTime = endTime
                        )
                    )
                }

                // Use the correct endpoint from the Spring Boot controller
                val url = URL("$BASE_URL/tutor-availability/createAvailability")
                val connection = createPostConnection(url)

                // Create JSON payload
                val jsonObject = JSONObject()
                jsonObject.put("tutorId", tutorId)
                jsonObject.put("dayOfWeek", dayOfWeek)
                jsonObject.put("startTime", startTime)
                jsonObject.put("endTime", endTime)

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutorAvailability(
                        id = json.optLong("id"),
                        tutorId = json.optLong("tutorId"),
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
                if (DEBUG_MODE) {
                    // Return mock data for testing
                    return@withContext Result.success(
                        TutorAvailability(
                            id = id,
                            tutorId = tutorId,
                            dayOfWeek = dayOfWeek,
                            startTime = startTime,
                            endTime = endTime
                        )
                    )
                }

                // Use the correct endpoint from the Spring Boot controller
                val url = URL("$BASE_URL/tutor-availability/updateAvailability/$id")
                val connection = createPutConnection(url)

                // Create JSON payload
                val jsonObject = JSONObject()
                jsonObject.put("id", id)
                jsonObject.put("tutorId", tutorId)
                jsonObject.put("dayOfWeek", dayOfWeek)
                jsonObject.put("startTime", startTime)
                jsonObject.put("endTime", endTime)

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutorAvailability(
                        id = json.optLong("id"),
                        tutorId = json.optLong("tutorId"),
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
                if (DEBUG_MODE) {
                    // Return success for testing
                    return@withContext Result.success(Unit)
                }

                // Use the correct endpoint from the Spring Boot controller
                val url = URL("$BASE_URL/tutor-availability/deleteAvailability/$id")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ ->
                    Unit
                }
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
                if (DEBUG_MODE) {
                    // Return success for testing
                    return@withContext Result.success(Unit)
                }

                // Use the correct endpoint from the Spring Boot controller
                val url = URL("$BASE_URL/tutor-availability/deleteAllForTutor/$tutorId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ ->
                    Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all tutor availability: ${e.message}", e)
                handleNetworkError(e, "deleting all tutor availability")
            }
        }
    }

    /**
     * Create a new tutoring session
     * @param tutorId ID of the tutor
     * @param studentId ID of the student/learner
     * @param startTime Start time of the session (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param endTime End time of the session (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param subject Subject for the session
     * @param sessionType Type of session (e.g., "Online", "In-Person")
     * @param notes Additional notes for the session
     * @return Result containing the created TutoringSession
     */
    suspend fun createTutoringSession(
        tutorId: Long,
        studentId: Long,
        startTime: String,
        endTime: String,
        subject: String,
        sessionType: String,
        notes: String?
    ): Result<TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return mock data for testing
                    return@withContext Result.success(
                        TutoringSession(
                            id = System.currentTimeMillis(),
                            tutorId = tutorId,
                            learnerId = studentId.toString(),
                            startTime = startTime,
                            endTime = endTime,
                            status = "Scheduled",
                            subject = subject,
                            sessionType = sessionType,
                            notes = notes
                        )
                    )
                }

                // Use the correct endpoint from the Spring Boot controller
                val url = URL("$BASE_URL/tutoring-sessions/createSession")
                val connection = createPostConnection(url)

                // Create the request body
                val jsonObject = JSONObject().apply {
                    put("tutorId", tutorId)
                    put("studentId", studentId) // Backend uses studentId instead of learnerId
                    put("startTime", startTime)
                    put("endTime", endTime)
                    put("status", "Scheduled") // Default status for new sessions
                    put("subject", subject)
                    put("sessionType", sessionType)
                    notes?.let { put("notes", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    TutoringSession(
                        id = json.optLong("id"),
                        tutorId = json.optLong("tutorId"),
                        learnerId = json.optString("studentId"), // Backend uses studentId instead of learnerId
                        startTime = json.optString("startTime"),
                        endTime = json.optString("endTime"),
                        status = json.optString("status"),
                        subject = json.optString("subject"),
                        sessionType = json.optString("sessionType"),
                        notes = json.optString("notes", null)
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
                if (DEBUG_MODE) {
                    // Convert the date to a day of week for mock data
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
                    
                    // Return mock data based on the day of week that matches actual tutor availability
                    val baseAvailabilityMap = mapOf(
                        "MONDAY" to listOf(
                            TutorAvailability(
                                id = 1,
                                tutorId = tutorId,
                                dayOfWeek = "MONDAY",
                                startTime = "12:00",
                                endTime = "18:00"
                            )
                        ),
                        "TUESDAY" to listOf(
                            TutorAvailability(
                                id = 2,
                                tutorId = tutorId,
                                dayOfWeek = "TUESDAY",
                                startTime = "12:00",
                                endTime = "18:00"
                            )
                        ),
                        "WEDNESDAY" to listOf(
                            TutorAvailability(
                                id = 3,
                                tutorId = tutorId,
                                dayOfWeek = "WEDNESDAY",
                                startTime = "12:00",
                                endTime = "20:00"
                            )
                        ),
                        "THURSDAY" to listOf(
                            TutorAvailability(
                                id = 4,
                                tutorId = tutorId,
                                dayOfWeek = "THURSDAY",
                                startTime = "12:00",
                                endTime = "18:00"
                            )
                        ),
                        "FRIDAY" to listOf(
                            TutorAvailability(
                                id = 5,
                                tutorId = tutorId,
                                dayOfWeek = "FRIDAY",
                                startTime = "12:00",
                                endTime = "18:00"
                            )
                        ),
                        "SATURDAY" to emptyList(),
                        "SUNDAY" to emptyList()
                    )
                    
                    // Get the availability for the day of week
                    val availability = baseAvailabilityMap[dayOfWeek] ?: emptyList()
                    
                    // Check if this is a specific date we want to simulate special availability for
                    // (For example, if the tutor has a different schedule on specific dates)
                    val today = Calendar.getInstance()
                    val selectedDate = Calendar.getInstance()
                    selectedDate.time = dateFormat.parse(date) ?: Date()
                    
                    // If the selected date is today, adjust the start time to account for current time
                    if (today.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) && 
                        today.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) && 
                        today.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)
                    ) {
                        val currentHour = today.get(Calendar.HOUR_OF_DAY)
                        
                        // If we're already past the tutor's start time for today
                        if (currentHour >= 12) {
                            // Start the availability an hour after current time if it's not too late
                            if (currentHour < 17) { // Allow booking until 1 hour before end time
                                return@withContext Result.success(
                                    listOf(
                                        TutorAvailability(
                                            id = 1,
                                            tutorId = tutorId,
                                            dayOfWeek = dayOfWeek,
                                            startTime = "${currentHour + 1}:00",
                                            endTime = "18:00"
                                        )
                                    )
                                )
                            } else {
                                // No more slots available today
                                return@withContext Result.success(emptyList())
                            }
                        }
                    }
                    
                    // For a date 7 days from now, simulate no availability
                    val nextWeek = Calendar.getInstance()
                    nextWeek.add(Calendar.DAY_OF_MONTH, 7)
                    if (nextWeek.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) && 
                        nextWeek.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) && 
                        nextWeek.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)
                    ) {
                        // For this specific date, return no availability
                        return@withContext Result.success(emptyList())
                    }
                    
                    return@withContext Result.success(availability)
                }

                // Use the correct endpoint from the Spring Boot controller
                val url = URL("$BASE_URL/tutor-availability/findByTutorAndDate/$tutorId/$date")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val availabilitySlots = mutableListOf<TutorAvailability>()

                    for (i in 0 until jsonArray.length()) {
                        val slotJson = jsonArray.getJSONObject(i)
                        availabilitySlots.add(
                            TutorAvailability(
                                id = slotJson.optLong("id"),
                                tutorId = slotJson.optLong("tutorId"),
                                dayOfWeek = slotJson.optString("dayOfWeek"),
                                startTime = slotJson.optString("startTime"),
                                endTime = slotJson.optString("endTime")
                            )
                        )
                    }
                    availabilitySlots
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching tutor availability by date: ${e.message}", e)
                handleNetworkError(e, "fetching tutor availability by date")
            }
        }
    }
}
