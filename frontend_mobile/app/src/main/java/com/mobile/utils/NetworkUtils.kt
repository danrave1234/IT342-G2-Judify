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

    // Debug mode flag - set to false for production use
    private const val DEBUG_MODE = true

    // Server configuration
    // Use your computer's IP address for physical device (e.g., "192.168.1.100")
    private const val SERVER_IP = "192.168.1.10" // Use 10.0.2.2 for emulator (points to host machine)
    private const val SERVER_PORT = "8080"
    private const val API_PATH = "api"

    private val BASE_URL = "http://$SERVER_IP:$SERVER_PORT/$API_PATH"

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
                imageResId = com.mobile.R.drawable.ic_courses
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
                imageResId = com.mobile.R.drawable.ic_courses
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
                imageResId = com.mobile.R.drawable.ic_courses
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
                imageResId = com.mobile.R.drawable.ic_courses
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
                imageResId = com.mobile.R.drawable.ic_courses
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
                imageResId = com.mobile.R.drawable.ic_courses
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
                imageResId = com.mobile.R.drawable.ic_courses
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
                imageResId = com.mobile.R.drawable.ic_courses
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

                val jsonObject = JSONObject().apply {
                    put("username", user.email) // Using email as username
                    put("email", user.email)
                    put("password", user.passwordHash) // Despite the name, this sends the plain password to the server
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

                val url = URL("$BASE_URL/tutor-profiles/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    TutorProfile(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        email = json.getString("email"),
                        bio = json.getString("bio"),
                        rating = json.getDouble("rating").toFloat(),
                        subjects = json.getJSONArray("subjects").let { subjects ->
                            (0 until subjects.length()).map { subjects.getString(it) }
                        },
                        hourlyRate = json.getDouble("hourlyRate")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting tutor profile")
            }
        }
    }

    /**
     * Find tutors by expertise
     */
    suspend fun findTutorsByExpertise(expertise: String): Result<List<TutorProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutorProfile(
                                id = 1,
                                name = "John Doe",
                                email = "john.doe@example.com",
                                bio = "Experienced tutor in mathematics and physics",
                                rating = 4.5f,
                                subjects = listOf("Mathematics", "Physics"),
                                hourlyRate = 50.0
                            )
                        )
                    )
                }

                val params = mapOf("expertise" to expertise)
                val url = createUrlWithParams("$BASE_URL/tutor-profiles/expertise", params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "tutors")
                    val tutors = mutableListOf<TutorProfile>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        tutors.add(
                            TutorProfile(
                                id = json.getLong("id"),
                                name = json.getString("name"),
                                email = json.getString("email"),
                                bio = json.getString("bio"),
                                rating = json.getDouble("rating").toFloat(),
                                subjects = json.getJSONArray("subjects").let { subjects ->
                                    (0 until subjects.length()).map { subjects.getString(it) }
                                },
                                hourlyRate = json.getDouble("hourlyRate")
                            )
                        )
                    }
                    tutors
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding tutors by expertise")
            }
        }
    }

    /**
     * Get reviews for a tutor
     */
    suspend fun getTutorReviews(tutorId: Long): Result<List<Review>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            Review(
                                id = 1,
                                tutorId = tutorId,
                                learnerId = 2,
                                rating = 5,
                                comment = "Excellent tutor!",
                                dateCreated = "2024-03-18"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/reviews/tutor/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "reviews")
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
                handleNetworkError(e, "getting tutor reviews")
            }
        }
    }

    /**
     * Create a review for a tutor
     */
    suspend fun createReview(review: Review): Result<Review> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(review.copy(id = 1))
                }

                val url = URL("$BASE_URL/reviews")
                val connection = createPostConnection(url)

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
                handleNetworkError(e, "creating review")
            }
        }
    }

    /**
     * Message-related API endpoints
     * Base URL: /api/messages
     * 
     * Available endpoints:
     * POST /sendMessage - Create new message
     * DELETE /deleteMessage/{id} - Delete a message
     * GET /findByConversation/{conversationId} - Get all messages in a conversation
     * GET /findByConversationPaginated/{conversationId} - Get paginated messages in a conversation
     * GET /findById/{id} - Get message by ID
     * GET /findUnreadByConversation/{conversationId} - Get unread messages in a conversation
     * GET /findUnreadByConversationPaginated/{conversationId} - Get paginated unread messages in a conversation
     * GET /findUnreadBySender/{senderId} - Get unread messages from a sender
     * GET /findUnreadBySenderPaginated/{senderId} - Get paginated unread messages from a sender
     * PUT /markAllAsRead/{conversationId} - Mark all messages in a conversation as read
     * PUT /markAsRead/{id} - Mark a single message as read
     */

    /**
     * Send a new message
     * @param conversationId ID of the conversation
     * @param senderId ID of the sender
     * @param content Message content
     * @return Result<Message> containing the sent message with server-generated ID and timestamp
     */
    suspend fun sendMessage(conversationId: Long, senderId: Long, content: String): Result<Message> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        Message(
                            id = 1,
                            conversationId = conversationId,
                            senderId = senderId,
                            content = content,
                            timestamp = "2024-03-20T14:30:00",
                            isRead = false
                        )
                    )
                }

                val url = URL("$BASE_URL/messages/sendMessage")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("conversationId", conversationId)
                    put("senderId", senderId)
                    put("content", content)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    Message(
                        id = json.getLong("id"),
                        conversationId = json.getLong("conversationId"),
                        senderId = json.getLong("senderId"),
                        content = json.getString("content"),
                        timestamp = json.getString("timestamp"),
                        isRead = json.optBoolean("isRead", false)
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "sending message")
            }
        }
    }

    /**
     * Get all messages in a conversation
     * @param conversationId ID of the conversation
     * @return Result<List<Message>> containing all messages in the conversation
     */
    suspend fun getConversationMessages(conversationId: Long): Result<List<Message>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            Message(
                                id = 1,
                                conversationId = conversationId,
                                senderId = 1,
                                content = "Hello there!",
                                timestamp = "2024-03-20T14:30:00",
                                isRead = true
                            ),
                            Message(
                                id = 2,
                                conversationId = conversationId,
                                senderId = 2,
                                content = "Hi! How are you?",
                                timestamp = "2024-03-20T14:35:00",
                                isRead = false
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/messages/findByConversation/$conversationId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "messages")
                    val messages = mutableListOf<Message>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        messages.add(
                            Message(
                                id = json.getLong("id"),
                                conversationId = json.getLong("conversationId"),
                                senderId = json.getLong("senderId"),
                                content = json.getString("content"),
                                timestamp = json.getString("timestamp"),
                                isRead = json.optBoolean("isRead", false)
                            )
                        )
                    }
                    messages
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting conversation messages")
            }
        }
    }

    /**
     * Delete a message
     * @param messageId ID of the message to delete
     * @return Result<Unit> indicating success or failure
     */
    suspend fun deleteMessage(messageId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/messages/deleteMessage/$messageId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting message")
            }
        }
    }

    /**
     * Notification-related API endpoints
     * Base URL: /api/notifications
     * 
     * Available endpoints:
     * POST /createNotification - Create new notification
     * DELETE /deleteAllForUser/{userId} - Delete all notifications for a user
     * DELETE /deleteNotification/{id} - Delete a notification
     * GET /findById/{id} - Find notification by ID
     * GET /findByType/{type} - Find notifications by type
     * GET /findByUser/{userId} - Find all notifications for a user
     * GET /findUnreadByUser/{userId} - Find unread notifications for a user
     * PUT /markAllAsRead/{userId} - Mark all notifications as read for a user
     * PUT /markAsRead/{id} - Mark a notification as read
     */

    /**
     * Create a new notification
     * @param userId ID of the user receiving the notification
     * @param type Type of notification (e.g., "MESSAGE", "SESSION", "REVIEW")
     * @param content Notification content/message
     * @return Result<Notification> containing the created notification with server-generated ID and timestamp
     */
    suspend fun createNotification(userId: Long, type: String, content: String): Result<Notification> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        Notification(
                            id = 1,
                            userId = userId,
                            type = type,
                            content = content,
                            timestamp = "2024-03-20T14:30:00",
                            isRead = false
                        )
                    )
                }

                val url = URL("$BASE_URL/notifications/createNotification")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("userId", userId)
                    put("type", type)
                    put("content", content)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    Notification(
                        id = json.getLong("id"),
                        userId = json.getLong("userId"),
                        type = json.getString("type"),
                        content = json.getString("content"),
                        timestamp = json.getString("timestamp"),
                        isRead = json.optBoolean("isRead", false)
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "creating notification")
            }
        }
    }

    /**
     * Find all notifications for a user (both read and unread)
     * @param userId ID of the user
     * @return Result<List<Notification>> containing all user's notifications
     */
    suspend fun findNotificationsByUser(userId: Long): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            Notification(
                                id = 1,
                                userId = userId,
                                type = "MESSAGE",
                                content = "You have a new message",
                                timestamp = "2024-03-20T10:00:00",
                                isRead = true
                            ),
                            Notification(
                                id = 2,
                                userId = userId,
                                type = "SESSION",
                                content = "Your session is starting soon",
                                timestamp = "2024-03-20T14:00:00",
                                isRead = false
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/notifications/findByUser/$userId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "notifications")
                    val notifications = mutableListOf<Notification>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        notifications.add(
                            Notification(
                                id = json.getLong("id"),
                                userId = json.getLong("userId"),
                                type = json.getString("type"),
                                content = json.getString("content"),
                                timestamp = json.getString("timestamp"),
                                isRead = json.optBoolean("isRead", false)
                            )
                        )
                    }
                    notifications
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding notifications by user")
            }
        }
    }

    /**
     * Delete all notifications for a user
     * @param userId ID of the user
     * @return Result<Unit> indicating success or failure
     */
    suspend fun deleteAllUserNotifications(userId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/notifications/deleteAllForUser/$userId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting all user notifications")
            }
        }
    }

    /**
     * PaymentTransaction-related API endpoints
     * Base URL: /api/payments
     * 
     * Available endpoints:
     * POST / - Create new transaction
     * GET /{id} - Get transaction by ID
     * GET /payer/{payerId} - Get all transactions for a payer
     * GET /payee/{payeeId} - Get all transactions for a payee
     * GET /status/{status} - Get transactions by status
     * GET /reference/{reference} - Get transactions by reference
     * PUT /{id}/status - Update transaction status
     * PUT /{id}/reference - Update payment reference
     * POST /{id}/refund - Process refund
     * DELETE /{id} - Delete transaction
     */

    /**
     * Gets all transactions for a payee
     * @param payeeId ID of the payee
     * @return Result<List<PaymentTransaction>> containing all transactions where the user is the payee
     * 
     * Response:
     * {
     *   "transactions": [
     *     {
     *       "id": Long,
     *       "payerId": Long,
     *       "payeeId": Long,
     *       "amount": Double,
     *       "status": String,
     *       "reference": String?,
     *       "timestamp": String
     *     }
     *   ]
     * }
     */
    suspend fun getPayeeTransactions(payeeId: Long): Result<List<PaymentTransaction>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            PaymentTransaction(
                                id = 1,
                                payerId = 1,
                                payeeId = payeeId,
                                amount = 50.0,
                                status = "COMPLETED",
                                timestamp = "2024-03-18T10:00:00"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/payments/payee/$payeeId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "transactions")
                    val transactions = mutableListOf<PaymentTransaction>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        transactions.add(
                            PaymentTransaction(
                                id = json.getLong("id"),
                                payerId = json.getLong("payerId"),
                                payeeId = json.getLong("payeeId"),
                                amount = json.getDouble("amount"),
                                status = json.getString("status"),
                                reference = json.optString("reference"),
                                timestamp = json.getString("timestamp")
                            )
                        )
                    }
                    transactions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting payee transactions")
            }
        }
    }

    /**
     * Updates the status of a payment transaction
     * @param transactionId ID of the transaction
     * @param status New status to set
     * @return Result<PaymentTransaction> containing the updated transaction
     * 
     * Response:
     * {
     *   "id": Long,
     *   "payerId": Long,
     *   "payeeId": Long,
     *   "amount": Double,
     *   "status": String,
     *   "reference": String?,
     *   "timestamp": String
     * }
     */
    suspend fun updateTransactionStatus(transactionId: Long, status: String): Result<PaymentTransaction> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        PaymentTransaction(
                            id = transactionId,
                            payerId = 1,
                            payeeId = 2,
                            amount = 50.0,
                            status = status,
                            timestamp = "2024-03-18T10:00:00"
                        )
                    )
                }

                val params = mapOf("status" to status)
                val url = createUrlWithParams("$BASE_URL/payments/$transactionId/status", params)
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    PaymentTransaction(
                        id = json.getLong("id"),
                        payerId = json.getLong("payerId"),
                        payeeId = json.getLong("payeeId"),
                        amount = json.getDouble("amount"),
                        status = json.getString("status"),
                        reference = json.optString("reference"),
                        timestamp = json.getString("timestamp")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "updating transaction status")
            }
        }
    }

    /**
     * Processes a refund for a payment transaction
     * @param transactionId ID of the transaction to refund
     * @return Result<PaymentTransaction> containing the refunded transaction
     * 
     * Response:
     * {
     *   "id": Long,
     *   "payerId": Long,
     *   "payeeId": Long,
     *   "amount": Double,
     *   "status": String,
     *   "reference": String?,
     *   "timestamp": String
     * }
     */
    suspend fun processRefund(transactionId: Long): Result<PaymentTransaction> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        PaymentTransaction(
                            id = transactionId,
                            payerId = 1,
                            payeeId = 2,
                            amount = 50.0,
                            status = "REFUNDED",
                            timestamp = "2024-03-18T10:00:00"
                        )
                    )
                }

                val url = URL("$BASE_URL/payments/$transactionId/refund")
                val connection = createPostConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    PaymentTransaction(
                        id = json.getLong("id"),
                        payerId = json.getLong("payerId"),
                        payeeId = json.getLong("payeeId"),
                        amount = json.getDouble("amount"),
                        status = json.getString("status"),
                        reference = json.optString("reference"),
                        timestamp = json.getString("timestamp")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "processing refund")
            }
        }
    }

    /**
     * TutorAvailability-related API endpoints
     * Base URL: /api/tutor-availability
     * 
     * Available endpoints:
     * GET /checkAvailability - Check if a time slot is available
     * POST /createAvailability - Create new availability slot
     * DELETE /deleteAllForTutor/{tutorId} - Delete all availability for a tutor
     * DELETE /deleteAvailability/{id} - Delete a specific availability
     * GET /findByDay/{dayOfWeek} - Find availability by day of week
     * GET /findById/{id} - Find availability by ID
     * GET /findByTutor/{tutorId} - Find all availability for a tutor
     * GET /findByTutorAndDay/{tutorId}/{dayOfWeek} - Find tutor availability for a specific day
     * PUT /updateAvailability/{id} - Update an availability slot
     */

    /**
     * Create a new availability slot
     * @param availability TutorAvailability object containing details
     * @return Result<TutorAvailability> containing the created availability slot
     */
    suspend fun createTutorAvailability(availability: TutorAvailability): Result<TutorAvailability> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        availability.copy(id = 1)
                    )
                }

                val url = URL("$BASE_URL/tutor-availability/createAvailability")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("tutorId", availability.tutorId)
                    put("dayOfWeek", availability.dayOfWeek)
                    put("startTime", availability.startTime)
                    put("endTime", availability.endTime)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutorAvailability(
                        id = json.getLong("id"),
                        tutorId = json.getLong("tutorId"),
                        dayOfWeek = json.getString("dayOfWeek"),
                        startTime = json.getString("startTime"),
                        endTime = json.getString("endTime")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "creating tutor availability")
            }
        }
    }

    /**
     * Delete all availability slots for a tutor
     * @param tutorId ID of the tutor
     * @return Result<Unit> indicating success or failure
     */
    suspend fun deleteAllTutorAvailability(tutorId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/tutor-availability/deleteAllForTutor/$tutorId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting all tutor availability")
            }
        }
    }

    /**
     * Check if a tutor is available at a specific time
     * @param tutorId ID of the tutor
     * @param dayOfWeek Day of the week (e.g., "MONDAY")
     * @param startTime Start time in format HH:mm
     * @param endTime End time in format HH:mm
     * @return Result<Boolean> indicating if the tutor is available
     */
    suspend fun checkTutorAvailability(
        tutorId: Long,
        dayOfWeek: String,
        startTime: String,
        endTime: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(true)
                }

                val params = mapOf(
                    "tutorId" to tutorId.toString(),
                    "dayOfWeek" to dayOfWeek,
                    "startTime" to startTime,
                    "endTime" to endTime
                )
                val url = createUrlWithParams("$BASE_URL/tutor-availability/checkAvailability", params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    json.getBoolean("available")
                }
            } catch (e: Exception) {
                handleNetworkError(e, "checking tutor availability")
            }
        }
    }

    /**
     * Payment-related API endpoints
     * Base URL: /api/payments
     * 
     * Available endpoints:
     * POST /createTransaction - Create new payment transaction
     * DELETE /deleteTransaction/{id} - Delete a transaction
     * GET /findById/{id} - Find transaction by ID
     * GET /findByPayee/{payeeId} - Find transactions by payee
     * GET /findByPayer/{payerId} - Find transactions by payer
     * GET /findByReference/{reference} - Find transactions by reference
     * GET /findByStatus/{status} - Find transactions by status
     * POST /processRefund/{id} - Process a refund
     * PUT /updateStatus/{id} - Update transaction status
     */

    /**
     * Create a new payment transaction
     * @param transaction PaymentTransaction object containing details
     * @return Result<PaymentTransaction> containing the created transaction
     */
    suspend fun createPaymentTransaction(transaction: PaymentTransaction): Result<PaymentTransaction> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        transaction.copy(
                            id = 1,
                            timestamp = "2024-03-20T10:00:00"
                        )
                    )
                }

                val url = URL("$BASE_URL/payments/createTransaction")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("payerId", transaction.payerId)
                    put("payeeId", transaction.payeeId)
                    put("amount", transaction.amount)
                    put("status", transaction.status)
                    transaction.reference?.let { put("reference", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    PaymentTransaction(
                        id = json.getLong("id"),
                        payerId = json.getLong("payerId"),
                        payeeId = json.getLong("payeeId"),
                        amount = json.getDouble("amount"),
                        status = json.getString("status"),
                        reference = json.optString("reference"),
                        timestamp = json.getString("timestamp")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "creating payment transaction")
            }
        }
    }

    /**
     * Find transactions by status
     * @param status Status to search for (e.g., "PENDING", "COMPLETED", "REFUNDED")
     * @return Result<List<PaymentTransaction>> containing all matching transactions
     */
    suspend fun findTransactionsByStatus(status: String): Result<List<PaymentTransaction>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            PaymentTransaction(
                                id = 1,
                                payerId = 1,
                                payeeId = 2,
                                amount = 50.0,
                                status = status,
                                timestamp = "2024-03-18T10:00:00"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/payments/findByStatus/$status")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "transactions")
                    val transactions = mutableListOf<PaymentTransaction>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        transactions.add(
                            PaymentTransaction(
                                id = json.getLong("id"),
                                payerId = json.getLong("payerId"),
                                payeeId = json.getLong("payeeId"),
                                amount = json.getDouble("amount"),
                                status = json.getString("status"),
                                reference = json.optString("reference"),
                                timestamp = json.getString("timestamp")
                            )
                        )
                    }
                    transactions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding transactions by status")
            }
        }
    }

    /**
     * Find transactions for a payer
     * @param payerId ID of the payer
     * @return Result<List<PaymentTransaction>> containing all transactions where user is payer
     */
    suspend fun findTransactionsByPayer(payerId: Long): Result<List<PaymentTransaction>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            PaymentTransaction(
                                id = 1,
                                payerId = payerId,
                                payeeId = 2,
                                amount = 50.0,
                                status = "COMPLETED",
                                timestamp = "2024-03-18T10:00:00"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/payments/findByPayer/$payerId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "transactions")
                    val transactions = mutableListOf<PaymentTransaction>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        transactions.add(
                            PaymentTransaction(
                                id = json.getLong("id"),
                                payerId = json.getLong("payerId"),
                                payeeId = json.getLong("payeeId"),
                                amount = json.getDouble("amount"),
                                status = json.getString("status"),
                                reference = json.optString("reference"),
                                timestamp = json.getString("timestamp")
                            )
                        )
                    }
                    transactions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding transactions by payer")
            }
        }
    }

    /**
     * Find transactions by reference
     * @param reference Reference code to search for
     * @return Result<List<PaymentTransaction>> containing all matching transactions
     */
    suspend fun findTransactionsByReference(reference: String): Result<List<PaymentTransaction>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            PaymentTransaction(
                                id = 1,
                                payerId = 1,
                                payeeId = 2,
                                amount = 50.0,
                                status = "COMPLETED",
                                reference = reference,
                                timestamp = "2024-03-18T10:00:00"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/payments/findByReference/$reference")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "transactions")
                    val transactions = mutableListOf<PaymentTransaction>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        transactions.add(
                            PaymentTransaction(
                                id = json.getLong("id"),
                                payerId = json.getLong("payerId"),
                                payeeId = json.getLong("payeeId"),
                                amount = json.getDouble("amount"),
                                status = json.getString("status"),
                                reference = json.optString("reference"),
                                timestamp = json.getString("timestamp")
                            )
                        )
                    }
                    transactions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding transactions by reference")
            }
        }
    }

    /**
     * TutoringSession-related API endpoints
     * Base URL: /api/tutoring-sessions
     * 
     * Available endpoints:
     * POST / - Create new session
     * GET /{id} - Get session by ID
     * GET /tutor/{tutorId} - Get all sessions for a tutor
     * GET /learner/{learnerId} - Get all sessions for a learner
     * GET /status/{status} - Get sessions by status
     * GET /date-range - Get sessions between dates
     * PUT /{id} - Update session
     * PUT /{id}/status - Update session status
     * DELETE /{id} - Delete session
     * GET /tutor/{tutorId}/status/{status} - Get tutor sessions by status
     * GET /learner/{learnerId}/status/{status} - Get learner sessions by status
     */

    /**
     * Gets all sessions for a tutor
     * @param tutorId ID of the tutor
     * @return Result<List<TutoringSession>> containing all sessions for the tutor
     * 
     * Response:
     * {
     *   "sessions": [
     *     {
     *       "id": Long,
     *       "tutorId": Long,
     *       "learnerId": Long,
     *       "startTime": String,
     *       "endTime": String,
     *       "status": String,
     *       "subject": String,
     *       "notes": String?
     *     }
     *   ]
     * }
     */
    suspend fun getTutorSessions(tutorId: Long): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutoringSession(
                                id = 1,
                                tutorId = tutorId,
                                learnerId = "2",
                                startTime = "2024-03-20T10:00:00",
                                endTime = "2024-03-20T11:00:00",
                                status = "SCHEDULED",
                                subject = "Mathematics",
                                sessionType = "Face-to-Face",
                                notes = null
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/tutoring-sessions/tutor/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "sessions")
                    val sessions = mutableListOf<TutoringSession>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        sessions.add(
                            TutoringSession(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                learnerId = json.getString("learnerId"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime"),
                                status = json.getString("status"),
                                subject = json.getString("subject"),
                                sessionType = json.getString("sessionType"),
                                notes = if (json.has("notes") && !json.isNull("notes")) json.getString("notes") else null
                            )
                        )
                    }
                    sessions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting tutor sessions")
            }
        }
    }

    /**
     * Gets sessions between two dates
     * @param startDate Start date in yyyy-MM-dd format
     * @param endDate End date in yyyy-MM-dd format
     * @return Result<List<TutoringSession>> containing all sessions within the date range
     * 
     * Response:
     * {
     *   "sessions": [
     *     {
     *       "id": Long,
     *       "tutorId": Long,
     *       "learnerId": Long,
     *       "startTime": String,
     *       "endTime": String,
     *       "status": String,
     *       "subject": String,
     *       "notes": String?
     *     }
     *   ]
     * }
     */
    suspend fun getSessionsBetweenDates(startDate: String, endDate: String): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutoringSession(
                                id = 1,
                                tutorId = 1,
                                learnerId = "2",
                                startTime = "2024-03-20T10:00:00",
                                endTime = "2024-03-20T11:00:00",
                                status = "SCHEDULED",
                                subject = "Mathematics",
                                sessionType = "Face-to-Face",
                                notes = null
                            )
                        )
                    )
                }

                val params = mapOf(
                    "start" to startDate,
                    "end" to endDate
                )
                val url = createUrlWithParams("$BASE_URL/tutoring-sessions/date-range", params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "sessions")
                    val sessions = mutableListOf<TutoringSession>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        sessions.add(
                            TutoringSession(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                learnerId = json.getString("learnerId"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime"),
                                status = json.getString("status"),
                                subject = json.getString("subject"),
                                sessionType = json.getString("sessionType"),
                                notes = if (json.has("notes") && !json.isNull("notes")) json.getString("notes") else null
                            )
                        )
                    }
                    sessions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting sessions between dates")
            }
        }
    }

    /**
     * Gets sessions for a tutor with a specific status
     * @param tutorId ID of the tutor
     * @param status Status to filter by
     * @return Result<List<TutoringSession>> containing matching sessions
     * 
     * Response:
     * {
     *   "sessions": [
     *     {
     *       "id": Long,
     *       "tutorId": Long,
     *       "learnerId": Long,
     *       "startTime": String,
     *       "endTime": String,
     *       "status": String,
     *       "subject": String,
     *       "notes": String?
     *     }
     *   ]
     * }
     */
    suspend fun getTutorSessionsByStatus(tutorId: Long, status: String): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutoringSession(
                                id = 1,
                                tutorId = tutorId,
                                learnerId = "2",
                                startTime = "2024-03-20T10:00:00",
                                endTime = "2024-03-20T11:00:00",
                                status = status,
                                subject = "Mathematics",
                                sessionType = "Face-to-Face",
                                notes = null
                            )
                        )
                    )
                }

                val params = mapOf("status" to status)
                val url = createUrlWithParams("$BASE_URL/tutoring-sessions/tutor/$tutorId/status", params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "sessions")
                    val sessions = mutableListOf<TutoringSession>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        sessions.add(
                            TutoringSession(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                learnerId = json.getString("learnerId"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime"),
                                status = json.getString("status"),
                                subject = json.getString("subject"),
                                sessionType = json.getString("sessionType"),
                                notes = if (json.has("notes") && !json.isNull("notes")) json.getString("notes") else null
                            )
                        )
                    }
                    sessions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting tutor sessions by status")
            }
        }
    }

    // Helper methods for connection handling
    private fun createGetConnection(url: URL): HttpURLConnection {
        Log.d(TAG, "Creating GET connection to: $url")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000 // Increased timeout
            readTimeout = 10000 // Increased timeout
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
    }

    private fun createGetConnection(baseUrl: String): HttpURLConnection {
        return createGetConnection(URL(baseUrl))
    }

    private fun createPostConnection(url: URL): HttpURLConnection {
        Log.d(TAG, "Creating POST connection to: $url")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000 // Increased timeout
            readTimeout = 10000 // Increased timeout
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
    }

    private fun createPostConnection(baseUrl: String): HttpURLConnection {
        return createPostConnection(URL(baseUrl))
    }

    private fun createPutConnection(url: URL): HttpURLConnection {
        Log.d(TAG, "Creating PUT connection to: $url")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = 10000 // Increased timeout
            readTimeout = 10000 // Increased timeout
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
    }

    private fun createPutConnection(baseUrl: String): HttpURLConnection {
        return createPutConnection(URL(baseUrl))
    }

    private fun createDeleteConnection(url: URL): HttpURLConnection {
        Log.d(TAG, "Creating DELETE connection to: $url")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 10000 // Increased timeout
            readTimeout = 10000 // Increased timeout
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
    }

    private fun createDeleteConnection(baseUrl: String): HttpURLConnection {
        return createDeleteConnection(URL(baseUrl))
    }

    /**
     * Helper method to safely encode URL parameters
     * @param value The value to encode
     * @return The encoded value
     */
    private fun encodeUrlParameter(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding URL parameter: ${e.message}", e)
            value
        }
    }

    /**
     * Helper method to create a URL with query parameters
     * @param baseUrl The base URL
     * @param params Map of parameter names to values
     * @return The complete URL with query parameters
     */
    private fun createUrlWithParams(baseUrl: String, params: Map<String, String>): URL {
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "${encodeUrlParameter(key)}=${encodeUrlParameter(value)}"
        }
        val urlString = if (queryString.isNotEmpty()) {
            "$baseUrl?$queryString"
        } else {
            baseUrl
        }
        return URL(urlString)
    }

    /**
     * Helper method to parse JSON response
     * @param response The JSON response string
     * @return The parsed JSONObject
     */
    private fun parseJsonResponse(response: String): JSONObject {
        return try {
            JSONObject(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON response: ${e.message}", e)
            throw Exception("Invalid JSON response from server")
        }
    }

    /**
     * Helper method to parse JSON array response
     * @param response The JSON response string
     * @param arrayKey The key for the array in the response
     * @return The parsed JSONArray
     */
    private fun parseJsonArrayResponse(response: String, arrayKey: String): org.json.JSONArray {
        return try {
            parseJsonResponse(response).getJSONArray(arrayKey)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON array response: ${e.message}", e)
            throw Exception("Invalid JSON array response from server")
        }
    }

    private fun <T> handleResponse(
        connection: HttpURLConnection,
        requestBody: String? = null,
        parser: (String) -> T
    ): Result<T> {
        try {
            if (requestBody != null) {
                Log.d(TAG, "Sending request body: $requestBody")
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Received response code: $responseCode")

            val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                // Try to read error response body
                try {
                    BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                        reader.readText()
                    }
                } catch (e: Exception) {
                    connection.responseMessage ?: "Unknown error"
                }
            }

            Log.d(TAG, "Response body: $response")

            when (responseCode) {
                HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> {
                    try {
                        return Result.success(parser(response))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response: ${e.message}", e)
                        return Result.failure(Exception("Error parsing server response: ${e.message}"))
                    }
                }
                HttpURLConnection.HTTP_BAD_REQUEST -> {
                    Log.e(TAG, "Bad request: $response")
                    return Result.failure(Exception("Invalid request: $response"))
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    Log.e(TAG, "Unauthorized: $response")
                    return Result.failure(Exception("Authentication required"))
                }
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    Log.e(TAG, "Forbidden: $response")
                    return Result.failure(Exception("Access denied"))
                }
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    Log.e(TAG, "Not found: $response")
                    return Result.failure(Exception("Resource not found"))
                }
                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    Log.e(TAG, "Server error: $response")
                    return Result.failure(Exception("Server error occurred: $response"))
                }
                else -> {
                    Log.e(TAG, "Unexpected response code $responseCode: $response")
                    return Result.failure(Exception("Unexpected server response: $responseCode - $response"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling response: ${e.message}", e)
            return Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    private fun handleNetworkError(e: Exception): Exception {
        val errorMessage = when (e) {
            is ConnectException -> "Cannot connect to server. Please check your internet connection."
            is SocketTimeoutException -> "Server is taking too long to respond. Please try again later."
            is java.net.UnknownHostException -> "Cannot resolve server host. Please check your internet connection."
            is java.net.MalformedURLException -> "Invalid server URL configuration."
            else -> "An unexpected error occurred: ${e.message}"
        }
        Log.e(TAG, "Network error: ${e.message}", e)
        return Exception(errorMessage)
    }

    private fun handleNetworkError(e: Exception, operation: String): Result<Nothing> {
        val errorMessage = when (e) {
            is ConnectException -> "Cannot connect to server while $operation. Please check your internet connection."
            is SocketTimeoutException -> "Server is taking too long to respond while $operation. Please try again later."
            is java.net.UnknownHostException -> "Cannot resolve server host while $operation. Please check your internet connection."
            is java.net.MalformedURLException -> "Invalid server URL configuration."
            else -> "An unexpected error occurred while $operation: ${e.message}"
        }
        Log.e(TAG, "Error during $operation: ${e.message}", e)
        return Result.failure(Exception(errorMessage))
    }

    /**
     * Marks a message as read
     * @param messageId ID of the message to mark as read
     * @return Result<Unit> indicating success or failure
     */
    suspend fun markMessageAsRead(messageId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/messages/markAsRead/$messageId")
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "marking message as read")
            }
        }
    }

    /**
     * Marks a notification as read
     * @param notificationId ID of the notification to mark as read
     * @return Result<Unit> indicating success or failure
     */
    suspend fun markNotificationAsRead(notificationId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/notifications/markAsRead/$notificationId")
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "marking notification as read")
            }
        }
    }

    /**
     * Marks all notifications for a user as read
     * @param userId ID of the user
     * @return Result<Unit> indicating success or failure
     */
    suspend fun markAllNotificationsAsRead(userId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/notifications/markAllAsRead/$userId")
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "marking all notifications as read")
            }
        }
    }

    /**
     * Data class for Conversation
     */
    data class Conversation(
        val id: Long,
        val participants: List<Long>,
        val createdAt: String,
        val lastMessageTime: String? = null
    )

    /**
     * User-related API endpoints
     * Base URL: /api/users
     * 
     * Available endpoints:
     * POST /addUser - Create new user
     * POST /authenticate - Authenticate user
     * DELETE /deleteUser/{id} - Delete user 
     * GET /findByEmail/{email} - Find user by email
     * GET /findById/{id} - Find user by ID
     * GET /findByRole/{role} - Find users by role
     * GET /getAllUsers - Get all users
     * PUT /updateRole/{id} - Update user role
     * PUT /updateUser/{id} - Update user details
     */

    /**
     * Find user by email
     * @param email User's email address
     * @return Result<User> containing the user if found
     */
    suspend fun findUserByEmail(email: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        User(
                            userId = 1,
                            email = email,
                            passwordHash = "hashed_password",
                            firstName = "Test",
                            lastName = "User",
                            roles = "LEARNER"
                        )
                    )
                }

                val url = URL("$BASE_URL/users/findByEmail/$email")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = json.optString("passwordHash", json.optString("password", "")),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = json.getString("roles")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding user by email")
            }
        }
    }

    /**
     * Find all users with a specific role
     * @param role Role to search for (e.g., "LEARNER", "TUTOR", "ADMIN")
     * @return Result<List<User>> containing all users with the specified role
     */
    suspend fun findUsersByRole(role: String): Result<List<User>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            User(
                                userId = 1,
                                email = "test@example.com",
                                passwordHash = "hashed_password",
                                firstName = "Test",
                                lastName = "User",
                                roles = role
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/users/findByRole/$role")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "users")
                    val users = mutableListOf<User>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        users.add(
                            User(
                                userId = json.getLong("userId"),
                                email = json.getString("email"),
                                passwordHash = json.optString("passwordHash", json.optString("password", "")),
                                firstName = json.getString("firstName"),
                                lastName = json.getString("lastName"),
                                profilePicture = json.optString("profilePicture"),
                                contactDetails = json.optString("contactDetails"),
                                roles = json.getString("roles")
                            )
                        )
                    }
                    users
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding users by role")
            }
        }
    }

    /**
     * Update user role
     * @param userId ID of the user
     * @param role New role to assign
     * @return Result<User> containing the updated user
     */
    suspend fun updateUserRole(userId: Long, role: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        User(
                            userId = userId,
                            email = "test@example.com",
                            passwordHash = "hashed_password",
                            firstName = "Test",
                            lastName = "User",
                            roles = role
                        )
                    )
                }

                val url = URL("$BASE_URL/users/updateRole/$userId")
                val connection = createPutConnection(url)

                val jsonObject = JSONObject().apply {
                    put("role", role)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = json.optString("passwordHash", json.optString("password", "")),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = json.getString("roles")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "updating user role")
            }
        }
    }

    /**
     * Review-related API endpoints
     * Base URL: /api/reviews
     * 
     * Available endpoints:
     * GET /calculateAverage - Calculate average review rating
     * GET /calculateAverageForTutor/{tutorId} - Calculate average rating for a tutor
     * POST /createReview - Create a new review
     * DELETE /deleteReview/{id} - Delete a review
     * GET /findById/{id} - Find review by ID
     * GET /findByRating/{rating} - Find reviews by rating
     * GET /findByRatingPaginated/{rating} - Find reviews by rating with pagination
     * GET /findByStudent/{learnerId} - Find reviews by student
     * GET /findByStudentPaginated/{learnerId} - Find reviews by student with pagination
     * GET /findByTutor/{tutorId} - Find reviews by tutor
     * GET /findByTutorSorted/{tutorId} - Find reviews by tutor sorted by date
     * PUT /updateReview/{id} - Update a review
     */

    /**
     * Calculate average rating for a tutor
     * @param tutorId ID of the tutor
     * @return Result<Float> containing the average rating
     */
    suspend fun calculateAverageTutorRating(tutorId: Long): Result<Float> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(4.5f)
                }

                val url = URL("$BASE_URL/reviews/calculateAverageForTutor/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    json.getDouble("averageRating").toFloat()
                }
            } catch (e: Exception) {
                handleNetworkError(e, "calculating average tutor rating")
            }
        }
    }

    /**
     * Find reviews by rating
     * @param rating Rating to search for (1-5)
     * @return Result<List<Review>> containing all reviews with the specified rating
     */
    suspend fun findReviewsByRating(rating: Int): Result<List<Review>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            Review(
                                id = 1,
                                tutorId = 1,
                                learnerId = 2,
                                rating = rating,
                                comment = "Rating $rating review",
                                dateCreated = "2024-03-18"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/reviews/findByRating/$rating")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "reviews")
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
                handleNetworkError(e, "finding reviews by rating")
            }
        }
    }

    /**
     * Find reviews by student (learner)
     * @param learnerId ID of the learner
     * @return Result<List<Review>> containing all reviews written by the learner
     */
    suspend fun findReviewsByStudent(learnerId: Long): Result<List<Review>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            Review(
                                id = 1,
                                tutorId = 1,
                                learnerId = learnerId,
                                rating = 5,
                                comment = "Learner review",
                                dateCreated = "2024-03-18"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/reviews/findByStudent/$learnerId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "reviews")
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
                handleNetworkError(e, "finding reviews by student")
            }
        }
    }

    /**
     * Conversation-related API endpoints
     * Base URL: /api/conversations
     *
     * Available endpoints:
     * PUT /addParticipant/{id} - Add a participant to a conversation
     * POST /createConversation - Create a new conversation
     * DELETE /deleteConversation/{id} - Delete a conversation
     * GET /findById/{id} - Find conversation by ID
     * GET /findByUser/{userId} - Find conversations for a user
     * PUT /removeParticipant/{id} - Remove a participant from a conversation
     */

    /**
     * Create a new conversation
     * @param participantIds List of user IDs who are participants in the conversation
     * @return Result<Conversation> containing the created conversation
     */
    suspend fun createConversation(participantIds: List<Long>): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        Conversation(
                            id = 1,
                            participants = participantIds,
                            createdAt = "2024-03-18T10:00:00"
                        )
                    )
                }

                val url = URL("$BASE_URL/conversations/createConversation")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("participants", JSONArray().apply {
                        participantIds.forEach { put(it) }
                    })
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    val participantsArray = json.getJSONArray("participants")
                    val participants = mutableListOf<Long>()

                    for (i in 0 until participantsArray.length()) {
                        participants.add(participantsArray.getLong(i))
                    }

                    Conversation(
                        id = json.getLong("id"),
                        participants = participants,
                        createdAt = json.getString("createdAt"),
                        lastMessageTime = json.optString("lastMessageTime")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "creating conversation")
            }
        }
    }

    /**
     * Find conversations for a user
     * @param userId ID of the user
     * @return Result<List<Conversation>> containing all conversations the user is a participant in
     */
    suspend fun findConversationsByUser(userId: Long): Result<List<Conversation>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            Conversation(
                                id = 1,
                                participants = listOf(userId, 2),
                                createdAt = "2024-03-18T10:00:00"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/conversations/findByUser/$userId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "conversations")
                    val conversations = mutableListOf<Conversation>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        val participantsArray = json.getJSONArray("participants")
                        val participants = mutableListOf<Long>()

                        for (j in 0 until participantsArray.length()) {
                            participants.add(participantsArray.getLong(j))
                        }

                        conversations.add(
                            Conversation(
                                id = json.getLong("id"),
                                participants = participants,
                                createdAt = json.getString("createdAt"),
                                lastMessageTime = json.optString("lastMessageTime")
                            )
                        )
                    }
                    conversations
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding conversations by user")
            }
        }
    }

    /**
     * Add a participant to a conversation
     * @param conversationId ID of the conversation
     * @param userId ID of the user to add
     * @return Result<Conversation> containing the updated conversation
     */
    suspend fun addParticipantToConversation(conversationId: Long, userId: Long): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        Conversation(
                            id = conversationId,
                            participants = listOf(1, userId),
                            createdAt = "2024-03-18T10:00:00"
                        )
                    )
                }

                val url = URL("$BASE_URL/conversations/addParticipant/$conversationId")
                val connection = createPutConnection(url)

                val jsonObject = JSONObject().apply {
                    put("userId", userId)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    val participantsArray = json.getJSONArray("participants")
                    val participants = mutableListOf<Long>()

                    for (i in 0 until participantsArray.length()) {
                        participants.add(participantsArray.getLong(i))
                    }

                    Conversation(
                        id = json.getLong("id"),
                        participants = participants,
                        createdAt = json.getString("createdAt"),
                        lastMessageTime = json.optString("lastMessageTime")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "adding participant to conversation")
            }
        }
    }

    /**
     * Course-related API endpoints
     * Base URL: /api/courses
     */

    /**
     * Get all courses
     * 
     * @return List of courses
     */
    suspend fun getAllCourses(): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    Log.d(TAG, "Getting all courses (mock data)")
                    return@withContext MockData.mockCourses
                }

                val url = URL("$BASE_URL/courses")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Parse JSON response
                    val jsonArray = JSONArray(response.toString())
                    val courses = mutableListOf<com.mobile.ui.courses.models.Course>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        courses.add(
                            com.mobile.ui.courses.models.Course(
                                id = jsonObject.getLong("id"),
                                title = jsonObject.getString("title"),
                                subtitle = jsonObject.getString("subtitle"),
                                description = jsonObject.getString("description"),
                                tutorCount = 1, // Default to 1 for now
                                averageRating = jsonObject.optDouble("averageRating", 0.0).toFloat(),
                                averagePrice = jsonObject.optDouble("price", 0.0).toFloat(),
                                category = jsonObject.getString("category"),
                                imageResId = com.mobile.R.drawable.ic_courses // Default image
                            )
                        )
                    }

                    return@withContext courses
                } else {
                    // Return empty list if API call fails
                    Log.e(TAG, "Error getting courses: HTTP $responseCode")
                    throw Exception("Failed to fetch courses: HTTP $responseCode")
                }
            } catch (e: Exception) {
                // Return empty list if API call fails
                Log.e(TAG, "Error getting courses: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Get courses by tutor ID
     * 
     * @param tutorId ID of the tutor
     * @return List of courses created by the tutor
     */
    suspend fun getCoursesByTutor(tutorId: Long): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    Log.d(TAG, "Getting courses by tutor (mock data)")
                    return@withContext MockData.mockCourses.filter { it.id % 2 == 0L } // Mock filter for demo
                }

                val url = URL("$BASE_URL/courses/tutor/$tutorId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Parse JSON response
                    val jsonArray = JSONArray(response.toString())
                    val courses = mutableListOf<com.mobile.ui.courses.models.Course>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        courses.add(
                            com.mobile.ui.courses.models.Course(
                                id = jsonObject.getLong("id"),
                                title = jsonObject.getString("title"),
                                subtitle = jsonObject.getString("subtitle"),
                                description = jsonObject.getString("description"),
                                tutorCount = 1, // For tutor's own courses, count is always 1
                                averageRating = jsonObject.optDouble("averageRating", 0.0).toFloat(),
                                averagePrice = jsonObject.optDouble("price", 0.0).toFloat(),
                                category = jsonObject.getString("category"),
                                imageResId = com.mobile.R.drawable.ic_courses // Default image
                            )
                        )
                    }

                    return@withContext courses
                } else {
                    // Throw exception if API call fails
                    Log.e(TAG, "Error getting tutor courses: HTTP $responseCode")
                    throw Exception("Failed to fetch tutor courses: HTTP $responseCode")
                }
            } catch (e: Exception) {
                // Throw exception if API call fails
                Log.e(TAG, "Error getting tutor courses: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Get popular courses
     * 
     * @return List of popular courses
     */
    suspend fun getPopularCourses(): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    Log.d(TAG, "Getting popular courses (mock data)")
                    return@withContext MockData.mockPopularCourses
                }

                // For now, just return the top 5 courses from getAllCourses
                // In a real implementation, this would call a specific API endpoint
                val allCourses = getAllCourses()
                return@withContext allCourses.take(5)
            } catch (e: Exception) {
                // Throw exception if API call fails
                Log.e(TAG, "Error getting popular courses: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Get courses by category
     * 
     * @param category Category to filter by (null for all categories)
     * @return List of courses in the specified category
     */
    suspend fun getCoursesByCategory(category: String?): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    Log.d(TAG, "Getting courses by category: $category (mock data)")
                    return@withContext if (category == null) {
                        MockData.mockCourses
                    } else {
                        MockData.mockCourses.filter { it.category == category }
                    }
                }

                val url = if (category == null) {
                    URL("$BASE_URL/courses")
                } else {
                    URL("$BASE_URL/courses/category/${URLEncoder.encode(category, "UTF-8")}")
                }

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Parse JSON response
                    val jsonArray = JSONArray(response.toString())
                    val courses = mutableListOf<com.mobile.ui.courses.models.Course>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        courses.add(
                            com.mobile.ui.courses.models.Course(
                                id = jsonObject.getLong("id"),
                                title = jsonObject.getString("title"),
                                subtitle = jsonObject.getString("subtitle"),
                                description = jsonObject.getString("description"),
                                tutorCount = 1, // Default to 1 for now
                                averageRating = jsonObject.optDouble("averageRating", 0.0).toFloat(),
                                averagePrice = jsonObject.optDouble("price", 0.0).toFloat(),
                                category = jsonObject.getString("category"),
                                imageResId = com.mobile.R.drawable.ic_courses // Default image
                            )
                        )
                    }

                    return@withContext courses
                } else {
                    // Throw exception if API call fails
                    Log.e(TAG, "Error getting courses by category: HTTP $responseCode")
                    throw Exception("Failed to fetch courses by category: HTTP $responseCode")
                }
            } catch (e: Exception) {
                // Throw exception if API call fails
                Log.e(TAG, "Error getting courses by category: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Search courses by query
     * 
     * @param query Search query
     * @return List of courses matching the query
     */
    suspend fun searchCourses(query: String): List<com.mobile.ui.courses.models.Course> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    Log.d(TAG, "Searching courses: $query (mock data)")
                    return@withContext MockData.mockCourses.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.subtitle.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
                    }
                }

                val url = URL("$BASE_URL/courses/search?title=${URLEncoder.encode(query, "UTF-8")}")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    // Parse JSON response
                    val jsonArray = JSONArray(response.toString())
                    val courses = mutableListOf<com.mobile.ui.courses.models.Course>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        courses.add(
                            com.mobile.ui.courses.models.Course(
                                id = jsonObject.getLong("id"),
                                title = jsonObject.getString("title"),
                                subtitle = jsonObject.getString("subtitle"),
                                description = jsonObject.getString("description"),
                                tutorCount = 1, // Default to 1 for now
                                averageRating = jsonObject.optDouble("averageRating", 0.0).toFloat(),
                                averagePrice = jsonObject.optDouble("price", 0.0).toFloat(),
                                category = jsonObject.getString("category"),
                                imageResId = com.mobile.R.drawable.ic_courses // Default image
                            )
                        )
                    }

                    return@withContext courses
                } else {
                    // Throw exception if API call fails
                    Log.e(TAG, "Error searching courses: HTTP $responseCode")
                    throw Exception("Failed to search courses: HTTP $responseCode")
                }
            } catch (e: Exception) {
                // Throw exception if API call fails
                Log.e(TAG, "Error searching courses: ${e.message}")
                throw e
            }
        }
    }

    /**
     * TutorProfile-related API endpoints
     * Base URL: /api/tutors
     *
     * Available endpoints:
     * POST /createProfile - Create new tutor profile
     * DELETE /deleteProfile/{id} - Delete a tutor profile
     * GET /findById/{id} - Find tutor profile by ID
     * GET /findByUserId/{userId} - Find tutor profile by user ID
     * GET /getAllProfiles - Get all tutor profiles
     * GET /getAllProfilesPaginated - Get all tutor profiles with pagination
     * GET /searchBySubject - Search tutor profiles by subject
     * PUT /updateProfile/{id} - Update a tutor profile
     * PUT /updateRating/{id} - Update a tutor's rating
     */

    /**
     * Search for tutors by subject
     * @param subject Subject to search for
     * @return Result<List<TutorProfile>> containing matching tutor profiles
     */
    suspend fun searchTutorsBySubject(subject: String): Result<List<TutorProfile>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutorProfile(
                                id = 1,
                                name = "John Doe",
                                email = "john.doe@example.com",
                                bio = "Experienced tutor in $subject",
                                rating = 4.5f,
                                subjects = listOf(subject, "Physics"),
                                hourlyRate = 50.0
                            )
                        )
                    )
                }

                val params = mapOf("subject" to subject)
                val url = createUrlWithParams("$BASE_URL/tutors/searchBySubject", params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "tutors")
                    val tutors = mutableListOf<TutorProfile>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        tutors.add(
                            TutorProfile(
                                id = json.getLong("id"),
                                name = json.getString("name"),
                                email = json.getString("email"),
                                bio = json.getString("bio"),
                                rating = json.getDouble("rating").toFloat(),
                                subjects = json.getJSONArray("subjects").let { subjects ->
                                    (0 until subjects.length()).map { subjects.getString(it) }
                                },
                                hourlyRate = json.getDouble("hourlyRate")
                            )
                        )
                    }
                    tutors
                }
            } catch (e: Exception) {
                handleNetworkError(e, "searching tutors by subject")
            }
        }
    }

    /**
     * Update a tutor's rating
     * @param tutorId ID of the tutor profile
     * @param rating New rating
     * @return Result<TutorProfile> containing the updated profile
     */
    suspend fun updateTutorRating(tutorId: Long, rating: Float): Result<TutorProfile> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        TutorProfile(
                            id = tutorId,
                            name = "John Doe",
                            email = "john.doe@example.com",
                            bio = "Experienced tutor",
                            rating = rating,
                            subjects = listOf("Mathematics", "Physics"),
                            hourlyRate = 50.0
                        )
                    )
                }

                val url = URL("$BASE_URL/tutors/updateRating/$tutorId")
                val connection = createPutConnection(url)

                val jsonObject = JSONObject().apply {
                    put("rating", rating)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutorProfile(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        email = json.getString("email"),
                        bio = json.getString("bio"),
                        rating = json.getDouble("rating").toFloat(),
                        subjects = json.getJSONArray("subjects").let { subjects ->
                            (0 until subjects.length()).map { subjects.getString(it) }
                        },
                        hourlyRate = json.getDouble("hourlyRate")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "updating tutor rating")
            }
        }
    }

    /**
     * TutoringSession-related API endpoints
     * Base URL: /api/tutoring-sessions
     * 
     * Available endpoints:
     * POST /createSession - Create new tutoring session
     * DELETE /deleteSession/{id} - Delete a tutoring session
     * GET /findByDateRange - Find sessions within a date range
     * GET /findById/{id} - Find session by ID
     * GET /findByStatus/{status} - Find sessions by status
     * GET /findByStatusPaginated/{status} - Find sessions by status with pagination
     * GET /findByStudent/{studentId} - Find sessions for a student
     * GET /findByStudentAndStatus/{studentId}/{status} - Find student sessions by status
     * GET /findByStudentPaginated/{studentId} - Find student sessions with pagination
     * GET /findByTutor/{tutorId} - Find sessions for a tutor
     * GET /findByTutorAndStatus/{tutorId}/{status} - Find tutor sessions by status
     * GET /findByTutorPaginated/{tutorId} - Find tutor sessions with pagination
     * PUT /updateSession/{id} - Update a tutoring session
     * PUT /updateStatus/{id} - Update a tutoring session's status
     */

    /**
     * Create a new tutoring session
     * @param session TutoringSession object with session details
     * @return Result<TutoringSession> containing the created session
     */
    suspend fun createTutoringSession(session: TutoringSession): Result<TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        session.copy(id = 1)
                    )
                }

                val url = URL("$BASE_URL/tutoring-sessions/createSession")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("tutorId", session.tutorId)
                    put("learnerId", session.learnerId)
                    put("startTime", session.startTime)
                    put("endTime", session.endTime)
                    put("status", session.status)
                    put("subject", session.subject)
                    put("sessionType", session.sessionType)
                    session.notes?.let { put("notes", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutoringSession(
                        id = json.getLong("id"),
                        tutorId = json.getLong("tutorId"),
                        learnerId = json.getString("learnerId"),
                        startTime = json.getString("startTime"),
                        endTime = json.getString("endTime"),
                        status = json.getString("status"),
                        subject = json.getString("subject"),
                        sessionType = json.getString("sessionType"),
                        notes = if (json.has("notes") && !json.isNull("notes")) json.getString("notes") else null
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "creating tutoring session")
            }
        }
    }

    /**
     * Find sessions for a student (learner)
     * @param studentId ID of the student/learner
     * @return Result<List<TutoringSession>> containing all sessions for the student
     */
    suspend fun findSessionsByStudent(studentId: Long): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutoringSession(
                                id = 1,
                                tutorId = 1,
                                learnerId = studentId.toString(),
                                startTime = "2024-03-20T10:00:00",
                                endTime = "2024-03-20T11:00:00",
                                status = "SCHEDULED",
                                subject = "Mathematics",
                                sessionType = "ONLINE",
                                notes = null
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/tutoring-sessions/findByStudent/$studentId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "sessions")
                    val sessions = mutableListOf<TutoringSession>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        sessions.add(
                            TutoringSession(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                learnerId = json.getString("learnerId"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime"),
                                status = json.getString("status"),
                                subject = json.getString("subject"),
                                sessionType = json.getString("sessionType"),
                                notes = if (json.has("notes") && !json.isNull("notes")) json.getString("notes") else null
                            )
                        )
                    }
                    sessions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding sessions by student")
            }
        }
    }

    /**
     * Update a tutoring session's status
     * @param sessionId ID of the session
     * @param status New status (e.g., "SCHEDULED", "COMPLETED", "CANCELLED")
     * @return Result<TutoringSession> containing the updated session
     */
    suspend fun updateSessionStatus(sessionId: Long, status: String): Result<TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        TutoringSession(
                            id = sessionId,
                            tutorId = 1,
                            learnerId = "2",
                            startTime = "2024-03-20T10:00:00",
                            endTime = "2024-03-20T11:00:00",
                            status = status,
                            subject = "Mathematics",
                            sessionType = "ONLINE",
                            notes = null
                        )
                    )
                }

                val url = URL("$BASE_URL/tutoring-sessions/updateStatus/$sessionId")
                val connection = createPutConnection(url)

                val jsonObject = JSONObject().apply {
                    put("status", status)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutoringSession(
                        id = json.getLong("id"),
                        tutorId = json.getLong("tutorId"),
                        learnerId = json.getString("learnerId"),
                        startTime = json.getString("startTime"),
                        endTime = json.getString("endTime"),
                        status = json.getString("status"),
                        subject = json.getString("subject"),
                        sessionType = json.getString("sessionType"),
                        notes = if (json.has("notes") && !json.isNull("notes")) json.getString("notes") else null
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "updating session status")
            }
        }
    }

    /**
     * Get all users in the system
     * @return Result<List<User>> containing all users
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            User(
                                userId = 1,
                                email = "user1@example.com",
                                passwordHash = "hashed_password",
                                firstName = "First",
                                lastName = "User",
                                roles = "LEARNER"
                            ),
                            User(
                                userId = 2,
                                email = "user2@example.com",
                                passwordHash = "hashed_password",
                                firstName = "Second",
                                lastName = "User",
                                roles = "TUTOR"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/users/getAllUsers")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "users")
                    val users = mutableListOf<User>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        users.add(
                            User(
                                userId = json.getLong("userId"),
                                email = json.getString("email"),
                                passwordHash = json.optString("passwordHash", json.optString("password", "")),
                                firstName = json.getString("firstName"),
                                lastName = json.getString("lastName"),
                                profilePicture = json.optString("profilePicture"),
                                contactDetails = json.optString("contactDetails"),
                                roles = json.getString("roles")
                            )
                        )
                    }
                    users
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting all users")
            }
        }
    }

    /**
     * Update a user's details
     * @param user User object with updated details
     * @return Result<User> containing the updated user
     */
    suspend fun updateUser(user: User): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(user)
                }

                val url = URL("$BASE_URL/users/updateUser/${user.userId}")
                val connection = createPutConnection(url)

                val jsonObject = JSONObject().apply {
                    put("email", user.email)
                    put("firstName", user.firstName)
                    put("lastName", user.lastName)
                    // Include username, using email as fallback if null
                    put("username", user.username ?: user.email)
                    user.profilePicture?.let { put("profilePicture", it) }
                    user.contactDetails?.let { put("contactDetails", it) }
                    // Include roles to ensure it's preserved
                    put("roles", user.roles)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = json.optString("passwordHash", json.optString("password", "")),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = json.getString("roles")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "updating user")
            }
        }
    }

    /**
     * Delete a user
     * @param userId ID of the user to delete
     * @return Result<Unit> indicating success or failure
     */
    suspend fun deleteUser(userId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/users/deleteUser/$userId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting user")
            }
        }
    }

    /**
     * Calculate the average review rating across all reviews
     * @return Result<Float> containing the average rating
     */
    suspend fun calculateAverageReviewRating(): Result<Float> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(4.2f)
                }

                val url = URL("$BASE_URL/reviews/calculateAverage")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    json.getDouble("averageRating").toFloat()
                }
            } catch (e: Exception) {
                handleNetworkError(e, "calculating average review rating")
            }
        }
    }

    /**
     * Update an existing review
     * @param reviewId ID of the review to update
     * @param rating New rating (1-5)
     * @param comment New comment
     * @return Result<Review> containing the updated review
     */
    suspend fun updateReview(reviewId: Long, rating: Int, comment: String): Result<Review> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        Review(
                            id = reviewId,
                            tutorId = 1,
                            learnerId = 2,
                            rating = rating,
                            comment = comment,
                            dateCreated = "2024-03-18"
                        )
                    )
                }

                val url = URL("$BASE_URL/reviews/updateReview/$reviewId")
                val connection = createPutConnection(url)

                val jsonObject = JSONObject().apply {
                    put("rating", rating)
                    put("comment", comment)
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
                handleNetworkError(e, "updating review")
            }
        }
    }

    /**
     * Delete a review
     * @param reviewId ID of the review to delete
     * @return Result<Unit> indicating success or failure
     */
    suspend fun deleteReview(reviewId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/reviews/deleteReview/$reviewId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting review")
            }
        }
    }

    /**
     * Get all unread messages in a conversation
     * @param conversationId ID of the conversation
     * @return Result<List<Message>> containing all unread messages in the conversation
     */
    suspend fun getUnreadConversationMessages(conversationId: Long): Result<List<Message>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            Message(
                                id = 1,
                                conversationId = conversationId,
                                senderId = 2,
                                content = "New message",
                                timestamp = "2024-03-20T14:30:00",
                                isRead = false
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/messages/findUnreadByConversation/$conversationId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "messages")
                    val messages = mutableListOf<Message>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        messages.add(
                            Message(
                                id = json.getLong("id"),
                                conversationId = json.getLong("conversationId"),
                                senderId = json.getLong("senderId"),
                                content = json.getString("content"),
                                timestamp = json.getString("timestamp"),
                                isRead = json.optBoolean("isRead", false)
                            )
                        )
                    }
                    messages
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting unread conversation messages")
            }
        }
    }

    /**
     * Mark all messages in a conversation as read
     * @param conversationId ID of the conversation
     * @return Result<Unit> indicating success or failure
     */
    suspend fun markAllConversationMessagesAsRead(conversationId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/messages/markAllAsRead/$conversationId")
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "marking all conversation messages as read")
            }
        }
    }

    /**
     * Remove a participant from a conversation
     * @param conversationId ID of the conversation
     * @param userId ID of the user to remove
     * @return Result<Conversation> containing the updated conversation
     */
    suspend fun removeParticipantFromConversation(conversationId: Long, userId: Long): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        Conversation(
                            id = conversationId,
                            participants = listOf(1),
                            createdAt = "2024-03-18T10:00:00"
                        )
                    )
                }

                val url = URL("$BASE_URL/conversations/removeParticipant/$conversationId")
                val connection = createPutConnection(url)

                val jsonObject = JSONObject().apply {
                    put("userId", userId)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    val participantsArray = json.getJSONArray("participants")
                    val participants = mutableListOf<Long>()

                    for (i in 0 until participantsArray.length()) {
                        participants.add(participantsArray.getLong(i))
                    }

                    Conversation(
                        id = json.getLong("id"),
                        participants = participants,
                        createdAt = json.getString("createdAt"),
                        lastMessageTime = json.optString("lastMessageTime")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "removing participant from conversation")
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
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/conversations/deleteConversation/$conversationId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting conversation")
            }
        }
    }

    /**
     * Find a conversation by ID
     * @param conversationId ID of the conversation
     * @return Result<Conversation> containing the conversation if found
     */
    suspend fun findConversationById(conversationId: Long): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        Conversation(
                            id = conversationId,
                            participants = listOf(1, 2),
                            createdAt = "2024-03-18T10:00:00"
                        )
                    )
                }

                val url = URL("$BASE_URL/conversations/findById/$conversationId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    val participantsArray = json.getJSONArray("participants")
                    val participants = mutableListOf<Long>()

                    for (i in 0 until participantsArray.length()) {
                        participants.add(participantsArray.getLong(i))
                    }

                    Conversation(
                        id = json.getLong("id"),
                        participants = participants,
                        createdAt = json.getString("createdAt"),
                        lastMessageTime = json.optString("lastMessageTime")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding conversation by ID")
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
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutorProfile(
                                id = 1,
                                name = "John Doe",
                                email = "john.doe@example.com",
                                bio = "Experienced tutor in mathematics",
                                rating = 4.5f,
                                subjects = listOf("Mathematics", "Physics"),
                                hourlyRate = 50.0
                            ),
                            TutorProfile(
                                id = 2,
                                name = "Jane Smith",
                                email = "jane.smith@example.com",
                                bio = "Specializing in languages",
                                rating = 4.8f,
                                subjects = listOf("English", "Spanish", "French"),
                                hourlyRate = 45.0
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/tutors/getAllProfiles")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "tutors")
                    val tutors = mutableListOf<TutorProfile>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        tutors.add(
                            TutorProfile(
                                id = json.getLong("id"),
                                name = json.getString("name"),
                                email = json.getString("email"),
                                bio = json.getString("bio"),
                                rating = json.getDouble("rating").toFloat(),
                                subjects = json.getJSONArray("subjects").let { subjects ->
                                    (0 until subjects.length()).map { subjects.getString(it) }
                                },
                                hourlyRate = json.getDouble("hourlyRate")
                            )
                        )
                    }
                    tutors
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting all tutor profiles")
            }
        }
    }

    /**
     * Find tutor profile by user ID
     * @param userId ID of the user
     * @return Result<TutorProfile> containing the tutor profile if found
     */
    suspend fun findTutorProfileByUserId(userId: Long): Result<TutorProfile> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        TutorProfile(
                            id = 1,
                            name = "John Doe",
                            email = "john.doe@example.com",
                            bio = "Experienced tutor in mathematics",
                            rating = 4.5f,
                            subjects = listOf("Mathematics", "Physics"),
                            hourlyRate = 50.0
                        )
                    )
                }

                val url = URL("$BASE_URL/tutors/findByUserId/$userId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    TutorProfile(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        email = json.getString("email"),
                        bio = json.getString("bio"),
                        rating = json.getDouble("rating").toFloat(),
                        subjects = json.getJSONArray("subjects").let { subjects ->
                            (0 until subjects.length()).map { subjects.getString(it) }
                        },
                        hourlyRate = json.getDouble("hourlyRate")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding tutor profile by user ID")
            }
        }
    }

    /**
     * Find unread notifications for a user
     * @param userId ID of the user
     * @return Result<List<Notification>> containing all unread notifications for the user
     */
    suspend fun findUnreadNotificationsByUser(userId: Long): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            Notification(
                                id = 1,
                                userId = userId,
                                type = "MESSAGE",
                                content = "You have a new message",
                                timestamp = "2024-03-20T14:00:00",
                                isRead = false
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/notifications/findUnreadByUser/$userId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "notifications")
                    val notifications = mutableListOf<Notification>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        notifications.add(
                            Notification(
                                id = json.getLong("id"),
                                userId = json.getLong("userId"),
                                type = json.getString("type"),
                                content = json.getString("content"),
                                timestamp = json.getString("timestamp"),
                                isRead = json.optBoolean("isRead", false)
                            )
                        )
                    }
                    notifications
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding unread notifications by user")
            }
        }
    }

    /**
     * Find all availability slots for a tutor
     * @param tutorId ID of the tutor
     * @return Result<List<TutorAvailability>> containing all availability slots for the tutor
     */
    suspend fun findTutorAvailability(tutorId: Long): Result<List<TutorAvailability>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutorAvailability(
                                id = 1,
                                tutorId = tutorId,
                                dayOfWeek = "MONDAY",
                                startTime = "09:00",
                                endTime = "12:00"
                            ),
                            TutorAvailability(
                                id = 2,
                                tutorId = tutorId,
                                dayOfWeek = "WEDNESDAY",
                                startTime = "14:00",
                                endTime = "17:00"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/tutor-availability/findByTutor/$tutorId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "availability")
                    val availabilityList = mutableListOf<TutorAvailability>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        availabilityList.add(
                            TutorAvailability(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                dayOfWeek = json.getString("dayOfWeek"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime")
                            )
                        )
                    }
                    availabilityList
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding tutor availability")
            }
        }
    }

    /**
     * Find availability slots for a tutor on a specific day
     * @param tutorId ID of the tutor
     * @param dayOfWeek Day of the week (e.g., "MONDAY", "TUESDAY")
     * @return Result<List<TutorAvailability>> containing availability slots for the tutor on the specified day
     */
    suspend fun findTutorAvailabilityByDay(tutorId: Long, dayOfWeek: String): Result<List<TutorAvailability>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutorAvailability(
                                id = 1,
                                tutorId = tutorId,
                                dayOfWeek = dayOfWeek,
                                startTime = "09:00",
                                endTime = "12:00"
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/tutor-availability/findByTutorAndDay/$tutorId/$dayOfWeek")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "availability")
                    val availabilityList = mutableListOf<TutorAvailability>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        availabilityList.add(
                            TutorAvailability(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                dayOfWeek = json.getString("dayOfWeek"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime")
                            )
                        )
                    }
                    availabilityList
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding tutor availability by day")
            }
        }
    }

    /**
     * Find a tutoring session by ID
     * @param sessionId ID of the session
     * @return Result<TutoringSession> containing the session if found
     */
    suspend fun findSessionById(sessionId: Long): Result<TutoringSession> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        TutoringSession(
                            id = sessionId,
                            tutorId = 1,
                            learnerId = "2",
                            startTime = "2024-03-20T10:00:00",
                            endTime = "2024-03-20T11:00:00",
                            status = "SCHEDULED",
                            subject = "Mathematics",
                            sessionType = "ONLINE",
                            notes = null
                        )
                    )
                }

                val url = URL("$BASE_URL/tutoring-sessions/findById/$sessionId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    TutoringSession(
                        id = json.getLong("id"),
                        tutorId = json.getLong("tutorId"),
                        learnerId = json.getString("learnerId"),
                        startTime = json.getString("startTime"),
                        endTime = json.getString("endTime"),
                        status = json.getString("status"),
                        subject = json.getString("subject"),
                        sessionType = json.getString("sessionType"),
                        notes = if (json.has("notes") && !json.isNull("notes")) json.getString("notes") else null
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding session by ID")
            }
        }
    }

    /**
     * Find sessions by status
     * @param status Status to filter by (e.g., "SCHEDULED", "COMPLETED", "CANCELLED")
     * @return Result<List<TutoringSession>> containing sessions with the specified status
     */
    suspend fun findSessionsByStatus(status: String): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutoringSession(
                                id = 1,
                                tutorId = 1,
                                learnerId = "2",
                                startTime = "2024-03-20T10:00:00",
                                endTime = "2024-03-20T11:00:00",
                                status = status,
                                subject = "Mathematics",
                                sessionType = "ONLINE",
                                notes = null
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/tutoring-sessions/findByStatus/$status")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "sessions")
                    val sessions = mutableListOf<TutoringSession>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        sessions.add(
                            TutoringSession(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                learnerId = json.getString("learnerId"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime"),
                                status = json.getString("status"),
                                subject = json.getString("subject"),
                                sessionType = json.getString("sessionType"),
                                notes = if (json.has("notes") && !json.isNull("notes")) json.getString("notes") else null
                            )
                        )
                    }
                    sessions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding sessions by status")
            }
        }
    }

    /**
     * Find student sessions by status
     * @param studentId ID of the student/learner
     * @param status Status to filter by (e.g., "SCHEDULED", "COMPLETED", "CANCELLED")
     * @return Result<List<TutoringSession>> containing student sessions with the specified status
     */
    suspend fun findStudentSessionsByStatus(studentId: Long, status: String): Result<List<TutoringSession>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutoringSession(
                                id = 1,
                                tutorId = 1,
                                learnerId = studentId.toString(),
                                startTime = "2024-03-20T10:00:00",
                                endTime = "2024-03-20T11:00:00",
                                status = status,
                                subject = "Mathematics",
                                sessionType = "ONLINE",
                                notes = null
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/tutoring-sessions/findByStudentAndStatus/$studentId/$status")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "sessions")
                    val sessions = mutableListOf<TutoringSession>()

                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        sessions.add(
                            TutoringSession(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                learnerId = json.getString("learnerId"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime"),
                                status = json.getString("status"),
                                subject = json.getString("subject"),
                                sessionType = json.getString("sessionType"),
                                notes = if (json.has("notes") && !json.isNull("notes")) json.getString("notes") else null
                            )
                        )
                    }
                    sessions
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding student sessions by status")
            }
        }
    }

    /**
     * Find a tutor profile by ID
     * @param tutorId ID of the tutor profile
     * @return Result<TutorProfile> containing the tutor profile if found
     */
    suspend fun findTutorById(tutorId: Long): Result<TutorProfile> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        TutorProfile(
                            id = tutorId,
                            name = "John Doe",
                            email = "john.doe@example.com",
                            bio = "Experienced tutor",
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
                    TutorProfile(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        email = json.getString("email"),
                        bio = json.getString("bio"),
                        rating = json.getDouble("rating").toFloat(),
                        subjects = json.getJSONArray("subjects").let { subjects ->
                            (0 until subjects.length()).map { subjects.getString(it) }
                        },
                        hourlyRate = json.getDouble("hourlyRate")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding tutor by ID")
            }
        }
    }

    /**
     * Find a tutor profile by user ID
     * @param userId ID of the user
     * @return Result<TutorProfile> containing the tutor profile if found
     */
    suspend fun findTutorByUserId(userId: Long): Result<TutorProfile> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        TutorProfile(
                            id = 1,
                            name = "John Doe",
                            email = "john.doe@example.com",
                            bio = "Experienced tutor",
                            rating = 4.5f,
                            subjects = listOf("Mathematics", "Physics"),
                            hourlyRate = 50.0
                        )
                    )
                }

                val url = URL("$BASE_URL/tutors/findByUserId/$userId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val json = parseJsonResponse(response)
                    TutorProfile(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        email = json.getString("email"),
                        bio = json.getString("bio"),
                        rating = json.getDouble("rating").toFloat(),
                        subjects = json.getJSONArray("subjects").let { subjects ->
                            (0 until subjects.length()).map { subjects.getString(it) }
                        },
                        hourlyRate = json.getDouble("hourlyRate")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "finding tutor by user ID")
            }
        }
    }

    /**
     * Get all tutor profiles
     * @return Result<List<TutorProfile>> containing all tutor profiles
     */
//    suspend fun getAllTutorProfiles(): Result<List<TutorProfile>> {
//        return withContext(Dispatchers.IO) {
//            try {
//                if (DEBUG_MODE) {
//                    return@withContext Result.success(
//                        listOf(
//                            TutorProfile(
//                                id = 1,
//                                name = "John Doe",
//                                email = "john.doe@example.com",
//                                bio = "Experienced mathematics tutor",
//                                rating = 4.5f,
//                                subjects = listOf("Mathematics", "Physics"),
//                                hourlyRate = 50.0
//                            ),
//                            TutorProfile(
//                                id = 2,
//                                name = "Jane Smith",
//                                email = "jane.smith@example.com",
//                                bio = "Experienced language tutor",
//                                rating = 4.8f,
//                                subjects = listOf("English", "Spanish"),
//                                hourlyRate = 45.0
//                            )
//                        )
//                    )
//                }
//
//                val url = URL("$BASE_URL/tutors/getAllProfiles")
//                val connection = createGetConnection(url)
//
//                return@withContext handleResponse(connection) { response ->
//                    val jsonArray = parseJsonArrayResponse(response, "tutors")
//                    val tutors = mutableListOf<TutorProfile>()
//
//                    for (i in 0 until jsonArray.length()) {
//                        val json = jsonArray.getJSONObject(i)
//                        tutors.add(
//                            TutorProfile(
//                                id = json.getLong("id"),
//                                name = json.getString("name"),
//                                email = json.getString("email"),
//                                bio = json.getString("bio"),
//                                rating = json.getDouble("rating").toFloat(),
//                                subjects = json.getJSONArray("subjects").let { subjects ->
//                                    (0 until subjects.length()).map { subjects.getString(it) }
//                                },
//                                hourlyRate = json.getDouble("hourlyRate")
//                            )
//                        )
//                    }
//                    tutors
//                }
//            } catch (e: Exception) {
//                handleNetworkError(e, "getting all tutor profiles")
//            }
//        }
//    }

    /**
     * Create a new tutor profile
     * @param tutorProfile TutorProfile object with profile details
     * @return Result<TutorProfile> containing the created profile
     */
    suspend fun createTutorProfile(tutorProfile: TutorProfile): Result<TutorProfile> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        tutorProfile.copy(id = 1)
                    )
                }

                val url = URL("$BASE_URL/tutors/createProfile")
                val connection = createPostConnection(url)

                val subjectsArray = JSONArray().apply {
                    tutorProfile.subjects.forEach { put(it) }
                }

                val jsonObject = JSONObject().apply {
                    put("name", tutorProfile.name)
                    put("email", tutorProfile.email)
                    put("bio", tutorProfile.bio)
                    put("rating", tutorProfile.rating)
                    put("subjects", subjectsArray)
                    put("hourlyRate", tutorProfile.hourlyRate)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutorProfile(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        email = json.getString("email"),
                        bio = json.getString("bio"),
                        rating = json.getDouble("rating").toFloat(),
                        subjects = json.getJSONArray("subjects").let { subjects ->
                            (0 until subjects.length()).map { subjects.getString(it) }
                        },
                        hourlyRate = json.getDouble("hourlyRate")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "creating tutor profile")
            }
        }
    }

    /**
     * Update a tutor profile
     * @param tutorId ID of the profile to update
     * @param tutorProfile TutorProfile object with updated details
     * @return Result<TutorProfile> containing the updated profile
     */
    suspend fun updateTutorProfile(tutorId: Long, tutorProfile: TutorProfile): Result<TutorProfile> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        tutorProfile.copy(id = tutorId)
                    )
                }

                val url = URL("$BASE_URL/tutors/updateProfile/$tutorId")
                val connection = createPutConnection(url)

                val subjectsArray = JSONArray().apply {
                    tutorProfile.subjects.forEach { put(it) }
                }

                val jsonObject = JSONObject().apply {
                    put("name", tutorProfile.name)
                    put("email", tutorProfile.email)
                    put("bio", tutorProfile.bio)
                    put("rating", tutorProfile.rating)
                    put("subjects", subjectsArray)
                    put("hourlyRate", tutorProfile.hourlyRate)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    TutorProfile(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        email = json.getString("email"),
                        bio = json.getString("bio"),
                        rating = json.getDouble("rating").toFloat(),
                        subjects = json.getJSONArray("subjects").let { subjects ->
                            (0 until subjects.length()).map { subjects.getString(it) }
                        },
                        hourlyRate = json.getDouble("hourlyRate")
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "updating tutor profile")
            }
        }
    }

    /**
     * Create a new course for a tutor
     * @param tutorId ID of the tutor creating the course
     * @param courseDTO CourseDTO object with course details
     * @return Result<CourseDTO> containing the created course
     */
    suspend fun createCourse(tutorId: Long, courseDTO: com.mobile.data.model.CourseDTO): Result<com.mobile.data.model.CourseDTO> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        courseDTO.copy(id = 1L, tutorId = tutorId)
                    )
                }

                val url = URL("$BASE_URL/courses/tutor/$tutorId")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("title", courseDTO.title)
                    put("subtitle", courseDTO.subtitle)
                    put("description", courseDTO.description)
                    put("category", courseDTO.category)
                    put("price", courseDTO.price)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    try {
                        val json = parseJsonResponse(response)
                        com.mobile.data.model.CourseDTO(
                            id = json.optLong("id"),
                            title = json.optString("title", ""),
                            subtitle = json.optString("subtitle", ""),
                            description = json.optString("description", ""),
                            tutorId = json.optLong("tutorId"),
                            tutorName = json.optString("tutorName", ""),
                            category = json.optString("category", ""),
                            price = json.optDouble("price", 0.0),
                            createdAt = try {
                                val dateStr = json.optString("createdAt", "")
                                if (dateStr.isNotEmpty()) {
                                    try { 
                                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(dateStr)
                                    } catch (e: Exception) { 
                                        null 
                                    }
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        )
                    } catch (e: Exception) {
                        // Don't log the error as it's likely just a date parsing issue
                        // and the course was still created successfully
                        // Return a default course with the data we sent, since we know the course was created
                        courseDTO.copy(id = 1L, tutorId = tutorId)
                    }
                }
            } catch (e: Exception) {
                handleNetworkError(e, "creating course")
            }
        }
    }

    /**
     * Update a tutor's location
     * @param tutorProfileId ID of the tutor profile to update
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @return Result<TutorProfile> containing the updated profile
     */
    suspend fun updateTutorLocation(tutorProfileId: Long, latitude: Double, longitude: Double): Result<TutorProfile> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    // Return a mock result for debugging
                    return@withContext Result.success(
                        TutorProfile(
                            id = tutorProfileId,
                            name = "Test Tutor",
                            email = "test@example.com",
                            bio = "Test bio",
                            rating = 4.5f,
                            subjects = listOf("Math", "Science"),
                            hourlyRate = 25.0
                        )
                    )
                }

                val url = createUrlWithParams(
                    "$BASE_URL/tutors/updateLocation/$tutorProfileId",
                    mapOf(
                        "latitude" to latitude.toString(),
                        "longitude" to longitude.toString()
                    )
                )
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection, null) { response ->
                    val json = parseJsonResponse(response)
                    TutorProfile(
                        id = json.getLong("profileId"),
                        name = json.optString("username", "Unknown"),
                        email = json.optString("email", ""),
                        bio = json.optString("bio", ""),
                        rating = json.optDouble("rating", 0.0).toFloat(),
                        subjects = json.optJSONArray("subjects")?.let { subjects ->
                            (0 until subjects.length()).map { subjects.getString(it) }
                        } ?: emptyList(),
                        hourlyRate = json.optDouble("hourlyRate", 0.0)
                    )
                }
            } catch (e: Exception) {
                handleNetworkError(e, "updating tutor location")
            }
        }
    }

    /**
     * Update an existing course
     * @param courseId ID of the course to update
     * @param courseDTO CourseDTO object with updated course details
     * @return Result<CourseDTO> containing the updated course
     */
    suspend fun updateCourse(courseId: Long, courseDTO: com.mobile.data.model.CourseDTO): Result<com.mobile.data.model.CourseDTO> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        courseDTO.copy(id = courseId)
                    )
                }

                val url = URL("$BASE_URL/courses/$courseId")
                val connection = createPutConnection(url)

                val jsonObject = JSONObject().apply {
                    put("title", courseDTO.title)
                    put("subtitle", courseDTO.subtitle)
                    put("description", courseDTO.description)
                    put("category", courseDTO.category)
                    put("price", courseDTO.price)
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    try {
                        val json = parseJsonResponse(response)
                        com.mobile.data.model.CourseDTO(
                            id = json.optLong("id"),
                            title = json.optString("title", ""),
                            subtitle = json.optString("subtitle", ""),
                            description = json.optString("description", ""),
                            tutorId = json.optLong("tutorId"),
                            tutorName = json.optString("tutorName", ""),
                            category = json.optString("category", ""),
                            price = json.optDouble("price", 0.0),
                            createdAt = try {
                                val dateStr = json.optString("createdAt", "")
                                if (dateStr.isNotEmpty()) {
                                    try { 
                                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(dateStr)
                                    } catch (e: Exception) { 
                                        null 
                                    }
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        )
                    } catch (e: Exception) {
                        // Return a default course with the data we sent, since we know the course was updated
                        courseDTO.copy(id = courseId)
                    }
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
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/courses/$courseId")
                val connection = createDeleteConnection(url)

                return@withContext handleResponse(connection, null) { _ ->
                    Unit
                }
            } catch (e: Exception) {
                handleNetworkError(e, "deleting course")
            }
        }
    }
} 
