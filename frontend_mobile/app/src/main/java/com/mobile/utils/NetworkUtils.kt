package com.mobile.utils

import android.util.Log
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

        // Mock courses data
        val mockCourses = listOf(
            com.mobile.ui.courses.models.Course(
                id = 1,
                title = "Mathematics",
                subtitle = "Algebra, Calculus, Geometry",
                description = "Learn mathematics from experienced tutors. Our courses cover a wide range of topics from basic arithmetic to advanced calculus.",
                tutorCount = 24,
                averageRating = 4.8f,
                averagePrice = 35.0f,
                category = "Mathematics",
                imageResId = com.mobile.R.drawable.ic_courses,
                tutorId = 101L,
                tutorName = "Dr. John Smith"
            ),
            com.mobile.ui.courses.models.Course(
                id = 2,
                title = "Physics",
                subtitle = "Mechanics, Thermodynamics, Electromagnetism",
                description = "Explore the fundamental laws that govern the universe. Our physics courses cover classical mechanics, thermodynamics, electromagnetism, and modern physics.",
                tutorCount = 18,
                averageRating = 4.7f,
                averagePrice = 40.0f,
                category = "Science",
                imageResId = com.mobile.R.drawable.ic_courses,
                tutorId = 102L,
                tutorName = "Prof. Maria Garcia"
            ),
            com.mobile.ui.courses.models.Course(
                id = 3,
                title = "Chemistry",
                subtitle = "Organic, Inorganic, Physical Chemistry",
                description = "Discover the fascinating world of chemistry. Our courses cover organic chemistry, inorganic chemistry, physical chemistry, and biochemistry.",
                tutorCount = 15,
                averageRating = 4.6f,
                averagePrice = 38.0f,
                category = "Science",
                imageResId = com.mobile.R.drawable.ic_courses,
                tutorId = 103L,
                tutorName = "Dr. Robert Chen"
            ),
            com.mobile.ui.courses.models.Course(
                id = 4,
                title = "English",
                subtitle = "Grammar, Literature, Writing",
                description = "Improve your English language skills with our comprehensive courses. We offer classes in grammar, literature, writing, and conversation.",
                tutorCount = 30,
                averageRating = 4.9f,
                averagePrice = 32.0f,
                category = "Languages",
                imageResId = com.mobile.R.drawable.ic_courses,
                tutorId = 104L,
                tutorName = "Sarah Johnson"
            ),
            com.mobile.ui.courses.models.Course(
                id = 5,
                title = "Spanish",
                subtitle = "Beginner to Advanced",
                description = "Learn Spanish from native speakers. Our courses are designed for all levels, from beginners to advanced learners.",
                tutorCount = 12,
                averageRating = 4.7f,
                averagePrice = 30.0f,
                category = "Languages",
                imageResId = com.mobile.R.drawable.ic_courses,
                tutorId = 105L,
                tutorName = "Carlos Rodriguez"
            ),
            com.mobile.ui.courses.models.Course(
                id = 6,
                title = "Programming",
                subtitle = "Java, Python, JavaScript",
                description = "Learn programming languages and software development. Our courses cover Java, Python, JavaScript, and web development.",
                tutorCount = 20,
                averageRating = 4.8f,
                averagePrice = 45.0f,
                category = "Programming",
                imageResId = com.mobile.R.drawable.ic_courses,
                tutorId = 106L,
                tutorName = "Michael Lee"
            ),
            com.mobile.ui.courses.models.Course(
                id = 7,
                title = "Data Science",
                subtitle = "Statistics, Machine Learning, Big Data",
                description = "Explore the world of data science. Our courses cover statistics, machine learning, big data, and data visualization.",
                tutorCount = 16,
                averageRating = 4.9f,
                averagePrice = 50.0f,
                category = "Programming",
                imageResId = com.mobile.R.drawable.ic_courses,
                tutorId = 107L,
                tutorName = "Emma Wilson"
            ),
            com.mobile.ui.courses.models.Course(
                id = 8,
                title = "Biology",
                subtitle = "Molecular, Cellular, Evolutionary",
                description = "Discover the science of life. Our biology courses cover molecular biology, cellular biology, evolutionary biology, and ecology.",
                tutorCount = 14,
                averageRating = 4.6f,
                averagePrice = 36.0f,
                category = "Science",
                imageResId = com.mobile.R.drawable.ic_courses,
                tutorId = 108L,
                tutorName = "Daniel Brown"
            )
        )

        // Popular courses (subset of all courses with highest ratings)
        val mockPopularCourses = mockCourses
            .sortedByDescending { it.averageRating }
            .take(4)
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
        val learnerId: String,
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
     * Get tutor profile by course ID
     */
    suspend fun getTutorProfileByCourseId(courseId: Long): Result<TutorProfile> {
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

                val url = URL("$BASE_URL/courses/$courseId/tutor")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    // Log the raw JSON response for debugging
                    Log.d(TAG, "Tutor Profile from Course JSON response: $json")

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
                return@withContext handleNetworkError(e, "getting tutor profile by course ID")
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

                val url = URL("$BASE_URL/conversations/user/$userId")
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

                val url = URL("$BASE_URL/conversations/create")
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

                val url = URL("$BASE_URL/conversations/$conversationId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting conversation")
            }
        }
    }

    /**
     * Create a new course
     * @param tutorId ID of the tutor creating the course
     * @param courseDTO Course data transfer object containing course details
     * @return Result<Course> containing the created course
     */
    suspend fun createCourse(tutorId: Long, courseDTO: com.mobile.data.model.CourseDTO): Result<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return a mock course for testing
                    return@withContext Result.success(
                        com.mobile.ui.courses.models.Course(
                            id = System.currentTimeMillis(),
                            title = courseDTO.title,
                            subtitle = courseDTO.subtitle,
                            description = courseDTO.description,
                            tutorCount = 1,
                            averageRating = 0.0f,
                            averagePrice = courseDTO.price.toFloat(),
                            category = courseDTO.category,
                            imageResId = com.mobile.R.drawable.ic_courses,
                            tutorId = tutorId,
                            tutorName = "Test Tutor"
                        )
                    )
                }

                val url = URL("$BASE_URL/courses/create")
                val connection = createPostConnection(url)

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("tutorId", tutorId)
                    put("title", courseDTO.title)
                    put("subtitle", courseDTO.subtitle)
                    put("description", courseDTO.description)
                    put("category", courseDTO.category)
                    put("price", courseDTO.price)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    com.mobile.ui.courses.models.Course(
                        id = json.optLong("id", System.currentTimeMillis()),
                        title = json.getString("title"),
                        subtitle = json.getString("subtitle"),
                        description = json.getString("description"),
                        tutorCount = 1,
                        averageRating = json.optFloat("rating", 0.0f),
                        averagePrice = json.optDouble("price", 0.0).toFloat(),
                        category = json.getString("category"),
                        imageResId = com.mobile.R.drawable.ic_courses,
                        tutorId = json.optLong("tutorId", tutorId),
                        tutorName = json.optString("tutorName", "")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "creating course")
            }
        }
    }

    /**
     * Update an existing course
     * @param courseId ID of the course to update
     * @param courseDTO Course data transfer object containing updated course details
     * @return Result<Course> containing the updated course
     */
    suspend fun updateCourse(courseId: Long, courseDTO: com.mobile.data.model.CourseDTO): Result<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return a mock course for testing
                    return@withContext Result.success(
                        com.mobile.ui.courses.models.Course(
                            id = courseId,
                            title = courseDTO.title,
                            subtitle = courseDTO.subtitle,
                            description = courseDTO.description,
                            tutorCount = 1,
                            averageRating = 4.5f,
                            averagePrice = courseDTO.price.toFloat(),
                            category = courseDTO.category,
                            imageResId = com.mobile.R.drawable.ic_courses,
                            tutorId = courseDTO.tutorId,
                            tutorName = courseDTO.tutorName
                        )
                    )
                }

                val url = URL("$BASE_URL/courses/update/$courseId")
                val connection = createPutConnection(url)

                // Create request body
                val jsonObject = JSONObject().apply {
                    put("id", courseId)
                    put("title", courseDTO.title)
                    put("subtitle", courseDTO.subtitle)
                    put("description", courseDTO.description)
                    put("category", courseDTO.category)
                    put("price", courseDTO.price)
                    courseDTO.tutorId?.let { put("tutorId", it) }
                    courseDTO.tutorName?.let { put("tutorName", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)

                    com.mobile.ui.courses.models.Course(
                        id = json.optLong("id", courseId),
                        title = json.getString("title"),
                        subtitle = json.getString("subtitle"),
                        description = json.getString("description"),
                        tutorCount = 1,
                        averageRating = json.optFloat("rating", 0.0f),
                        averagePrice = json.optDouble("price", 0.0).toFloat(),
                        category = json.getString("category"),
                        imageResId = com.mobile.R.drawable.ic_courses,
                        tutorId = json.optLong("tutorId", courseDTO.tutorId ?: 0L),
                        tutorName = json.optString("tutorName", courseDTO.tutorName ?: "")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "updating course")
            }
        }
    }

    /**
     * Delete a course
     * @param courseId ID of the course to delete
     * @return Result<Unit> indicating success or failure
     */
    suspend fun deleteCourse(courseId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Simulate success for testing
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/courses/delete/$courseId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting course")
            }
        }
    }

    /**
     * Get all courses
     * @return Result<List<Course>> containing all available courses
     */
    suspend fun getAllCourses(): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return mock courses for testing
                    return@withContext MockData.mockCourses
                }

                val url = URL("$BASE_URL/courses")
                val connection = createGetConnection(url)

                val result = handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val courses = mutableListOf<com.mobile.ui.courses.models.Course>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        courses.add(
                            com.mobile.ui.courses.models.Course(
                                id = json.getLong("id"),
                                title = json.getString("title"),
                                subtitle = json.optString("subtitle", ""),
                                description = json.optString("description", ""),
                                tutorCount = 1,
                                averageRating = json.optFloat("rating", 0.0f),
                                averagePrice = json.optDouble("price", 0.0).toFloat(),
                                category = json.optString("category", "Other"),
                                imageResId = com.mobile.R.drawable.ic_courses,
                                tutorId = json.optLong("tutorId", 0L),
                                tutorName = json.optString("tutorName", "")
                            )
                        )
                    }
                    courses
                }

                // Unwrap the result to propagate errors
                val courses = result.getOrThrow()
                Log.d(TAG, "Successfully fetched ${courses.size} courses")
                return@withContext courses
            } catch (e: Exception) {
                Log.e(TAG, "Error getting all courses: ${e.message}", e)
                // Throw the exception to propagate the error
                throw Exception("Failed to fetch courses: ${e.message}", e)
            }
        }
    }

    /**
     * Get popular courses
     * @return List<Course> containing the most popular courses
     */
    suspend fun getPopularCourses(): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return mock popular courses for testing
                    return@withContext MockData.mockPopularCourses
                }

                val url = URL("$BASE_URL/courses/popular")
                val connection = createGetConnection(url)

                val result = handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val courses = mutableListOf<com.mobile.ui.courses.models.Course>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        courses.add(
                            com.mobile.ui.courses.models.Course(
                                id = json.getLong("id"),
                                title = json.getString("title"),
                                subtitle = json.optString("subtitle", ""),
                                description = json.optString("description", ""),
                                tutorCount = 1,
                                averageRating = json.optFloat("rating", 0.0f),
                                averagePrice = json.optDouble("price", 0.0).toFloat(),
                                category = json.optString("category", "Other"),
                                imageResId = com.mobile.R.drawable.ic_courses,
                                tutorId = json.optLong("tutorId", 0L),
                                tutorName = json.optString("tutorName", "")
                            )
                        )
                    }
                    courses
                }

                // Unwrap the result to propagate errors
                val courses = result.getOrThrow()
                Log.d(TAG, "Successfully fetched ${courses.size} popular courses")
                return@withContext courses
            } catch (e: Exception) {
                Log.e(TAG, "Error getting popular courses: ${e.message}", e)
                // Throw the exception to propagate the error
                throw Exception("Failed to fetch popular courses: ${e.message}", e)
            }
        }
    }

    /**
     * Get courses by tutor ID
     * @param tutorId ID of the tutor
     * @return List<Course> containing the tutor's courses
     */
    suspend fun getCoursesByTutor(tutorId: Long): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return filtered mock courses for testing
                    return@withContext MockData.mockCourses.filter { it.tutorId == tutorId }
                }

                val url = URL("$BASE_URL/courses/tutor/$tutorId")
                val connection = createGetConnection(url)

                val result = handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val courses = mutableListOf<com.mobile.ui.courses.models.Course>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        courses.add(
                            com.mobile.ui.courses.models.Course(
                                id = json.getLong("id"),
                                title = json.getString("title"),
                                subtitle = json.optString("subtitle", ""),
                                description = json.optString("description", ""),
                                tutorCount = 1,
                                averageRating = json.optFloat("rating", 0.0f),
                                averagePrice = json.optDouble("price", 0.0).toFloat(),
                                category = json.optString("category", "Other"),
                                imageResId = com.mobile.R.drawable.ic_courses,
                                tutorId = tutorId,
                                tutorName = json.optString("tutorName", "")
                            )
                        )
                    }
                    courses
                }

                return@withContext result.getOrDefault(emptyList())
            } catch (e: Exception) {
                Log.e(TAG, "Error getting courses by tutor: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }

    /**
     * Search courses by query
     * @param query Search query
     * @return List<Course> containing matching courses
     */
    suspend fun searchCourses(query: String): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Filter mock courses by query for testing
                    val lowerQuery = query.lowercase()
                    return@withContext MockData.mockCourses.filter { 
                        it.title.lowercase().contains(lowerQuery) || 
                        it.description.lowercase().contains(lowerQuery) ||
                        it.category.lowercase().contains(lowerQuery)
                    }
                }

                val params = mapOf("query" to query)
                val url = createUrlWithParams("$BASE_URL/courses/search", params)
                val connection = createGetConnection(url)

                val result = handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val courses = mutableListOf<com.mobile.ui.courses.models.Course>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        courses.add(
                            com.mobile.ui.courses.models.Course(
                                id = json.getLong("id"),
                                title = json.getString("title"),
                                subtitle = json.optString("subtitle", ""),
                                description = json.optString("description", ""),
                                tutorCount = 1,
                                averageRating = json.optFloat("rating", 0.0f),
                                averagePrice = json.optDouble("price", 0.0).toFloat(),
                                category = json.optString("category", "Other"),
                                imageResId = com.mobile.R.drawable.ic_courses,
                                tutorId = json.optLong("tutorId", 0L),
                                tutorName = json.optString("tutorName", "")
                            )
                        )
                    }
                    courses
                }

                return@withContext result.getOrDefault(emptyList())
            } catch (e: Exception) {
                Log.e(TAG, "Error searching courses: ${e.message}", e)
                return@withContext emptyList()
            }
        }
    }

    /**
     * Get courses by category
     * @param category Category name (null for all categories)
     * @return List<Course> containing courses in the specified category
     */
    suspend fun getCoursesByCategory(category: String?): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Filter mock courses by category for testing
                    return@withContext if (category == null) {
                        MockData.mockCourses
                    } else {
                        MockData.mockCourses.filter { it.category.equals(category, ignoreCase = true) }
                    }
                }

                val url = if (category == null) {
                    URL("$BASE_URL/courses/all")
                } else {
                    val params = mapOf("category" to category)
                    createUrlWithParams("$BASE_URL/courses/category", params)
                }

                val connection = createGetConnection(url)

                val result = handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response)
                    val courses = mutableListOf<com.mobile.ui.courses.models.Course>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        courses.add(
                            com.mobile.ui.courses.models.Course(
                                id = json.getLong("id"),
                                title = json.getString("title"),
                                subtitle = json.optString("subtitle", ""),
                                description = json.optString("description", ""),
                                tutorCount = 1,
                                averageRating = json.optFloat("rating", 0.0f),
                                averagePrice = json.optDouble("price", 0.0).toFloat(),
                                category = json.optString("category", "Other"),
                                imageResId = com.mobile.R.drawable.ic_courses,
                                tutorId = json.optLong("tutorId", 0L),
                                tutorName = json.optString("tutorName", "")
                            )
                        )
                    }
                    courses
                }

                return@withContext result.getOrDefault(emptyList())
            } catch (e: Exception) {
                Log.e(TAG, "Error getting courses by category: ${e.message}", e)
                return@withContext emptyList()
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
                val url = createUrlWithParams("$BASE_URL/users/findByEmail", params)
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

                        tutors.add(
                            TutorProfile(
                                id = tutorJson.getLong("profileId"),
                                name = tutorJson.optString("username", "Unknown"),
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
     * Get course by ID
     * @param courseId ID of the course
     * @return Course object or null if not found
     */
    suspend fun getCourseById(courseId: Long): com.mobile.ui.courses.models.Course? {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Look for the course in mock data
                    return@withContext MockData.mockCourses.firstOrNull { it.id == courseId }
                }

                val url = URL("$BASE_URL/courses/$courseId")
                val connection = createGetConnection(url)

                val result = handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)

                    com.mobile.ui.courses.models.Course(
                        id = json.getLong("id"),
                        title = json.getString("title"),
                        subtitle = json.optString("subtitle", ""),
                        description = json.optString("description", ""),
                        tutorCount = 1,
                        averageRating = json.optFloat("rating", 0.0f),
                        averagePrice = json.optDouble("price", 0.0).toFloat(),
                        category = json.optString("category", "Other"),
                        imageResId = com.mobile.R.drawable.ic_courses,
                        tutorId = json.optLong("tutorId", 0L),
                        tutorName = json.optString("tutorName", "")
                    )
                }

                return@withContext result.getOrNull()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting course by ID: ${e.message}", e)
                return@withContext null
            }
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
}
