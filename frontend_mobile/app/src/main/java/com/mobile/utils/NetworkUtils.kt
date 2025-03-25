package com.mobile.utils

import android.util.Log
import com.mobile.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    // Use 10.0.2.2 for Android Emulator
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
    }
    
    /**
     * Authentication response data class
     */
    data class AuthResponse(
        val success: Boolean = true,
        val isAuthenticated: Boolean,
        val userId: Long? = null,
        val email: String = "",
        val firstName: String = "",
        val lastName: String = "",
        val role: String = "LEARNER"
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
        val notes: String
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
                    Log.d(TAG, "Using mock auth response for: $email")
                    return@withContext Result.success(
                        AuthResponse(
                            isAuthenticated = true,
                            userId = 1,
                            email = email,
                            firstName = "Test",
                            lastName = "User",
                            role = "LEARNER"
                        )
                    )
                }

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
                    AuthResponse(
                        isAuthenticated = json.getBoolean("isAuthenticated"),
                        userId = json.optLong("userId"),
                        email = json.getString("email"),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        role = json.getString("role")
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

                val url = URL("$BASE_URL/users")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("email", user.email)
                    put("passwordHash", user.passwordHash)
                    put("firstName", user.firstName)
                    put("lastName", user.lastName)
                    put("roles", user.roles)
                    user.profilePicture?.let { put("profilePicture", it) }
                    user.contactDetails?.let { put("contactDetails", it) }
                }

                return@withContext handleResponse(connection, jsonObject.toString()) { response ->
                    val json = parseJsonResponse(response)
                    User(
                        userId = json.getLong("userId"),
                        email = json.getString("email"),
                        passwordHash = json.getString("passwordHash"),
                        firstName = json.getString("firstName"),
                        lastName = json.getString("lastName"),
                        profilePicture = json.optString("profilePicture"),
                        contactDetails = json.optString("contactDetails"),
                        roles = json.getString("roles")
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
                        rating = json.getFloat("rating"),
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
                                rating = json.getFloat("rating"),
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
     * POST / - Create new message
     * GET /{id} - Get message by ID
     * GET /conversation/{conversationId} - Get all messages in a conversation
     * GET /unread/sender/{senderId} - Get unread messages from a sender
     * GET /unread/conversation/{conversationId} - Get unread messages in a conversation
     * PUT /{id}/read - Mark message as read
     * PUT /conversation/{conversationId}/read-all - Mark all messages in conversation as read
     * DELETE /{id} - Delete a message
     */

    /**
     * Creates a new message in a conversation
     * @param message Message object containing conversationId, senderId, and content
     * @return Result<Message> containing the created message with server-generated ID and timestamp
     * 
     * Request body:
     * {
     *   "conversationId": Long,
     *   "senderId": Long,
     *   "content": String
     * }
     * 
     * Response:
     * {
     *   "id": Long,
     *   "conversationId": Long,
     *   "senderId": Long,
     *   "content": String,
     *   "timestamp": String,
     *   "isRead": Boolean
     * }
     */
    suspend fun createMessage(message: Message): Result<Message> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(MockData.mockMessage)
                }

                val url = URL("$BASE_URL/messages")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("conversationId", message.conversationId)
                    put("senderId", message.senderId)
                    put("content", message.content)
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
                handleNetworkError(e, "creating message")
            }
        }
    }

    /**
     * Gets a specific message by ID
     * @param messageId ID of the message to retrieve
     * @return Result<Message> containing the requested message
     * 
     * Response:
     * {
     *   "id": Long,
     *   "conversationId": Long,
     *   "senderId": Long,
     *   "content": String,
     *   "timestamp": String,
     *   "isRead": Boolean
     * }
     */
    suspend fun getMessageById(messageId: Long): Result<Message> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(MockData.mockMessage)
                }

                val url = URL("$BASE_URL/messages/$messageId")
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
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
                handleNetworkError(e, "getting message by ID")
            }
        }
    }

    /**
     * Gets all unread messages from a specific sender
     * @param senderId ID of the sender
     * @return Result<List<Message>> containing all unread messages from the sender
     * 
     * Response:
     * {
     *   "messages": [
     *     {
     *       "id": Long,
     *       "conversationId": Long,
     *       "senderId": Long,
     *       "content": String,
     *       "timestamp": String,
     *       "isRead": Boolean
     *     }
     *   ]
     * }
     */
    suspend fun getUnreadMessagesBySender(senderId: Long): Result<List<Message>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            Message(
                                id = 1,
                                conversationId = 1,
                                senderId = senderId,
                                content = "Unread message",
                                timestamp = "2024-03-18T10:00:00",
                                isRead = false
                            )
                        )
                    )
                }

                val url = URL("$BASE_URL/messages/unread/sender/$senderId")
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
                handleNetworkError(e, "getting unread messages")
            }
        }
    }

    /**
     * Marks all messages in a conversation as read
     * @param conversationId ID of the conversation
     * @return Result<Unit> indicating success or failure
     */
    suspend fun markAllConversationMessagesAsRead(conversationId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(Unit)
                }

                val url = URL("$BASE_URL/messages/conversation/$conversationId/read-all")
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "marking conversation messages as read")
            }
        }
    }

    /**
     * Notification-related API endpoints
     * Base URL: /api/notifications
     * 
     * Available endpoints:
     * POST / - Create new notification
     * GET /{id} - Get notification by ID
     * GET /user/{userId} - Get all notifications for a user
     * GET /user/{userId}/unread - Get unread notifications for a user
     * GET /type/{type} - Get notifications by type
     * PUT /{id}/read - Mark notification as read
     * PUT /user/{userId}/read-all - Mark all notifications as read
     * DELETE /{id} - Delete a notification
     * DELETE /user/{userId} - Delete all notifications for a user
     */

    /**
     * Creates a new notification
     * @param notification Notification object containing userId, type, and content
     * @return Result<Notification> containing the created notification
     * 
     * Request body:
     * {
     *   "userId": Long,
     *   "type": String,
     *   "content": String
     * }
     * 
     * Response:
     * {
     *   "id": Long,
     *   "userId": Long,
     *   "type": String,
     *   "content": String,
     *   "timestamp": String,
     *   "isRead": Boolean
     * }
     */
    suspend fun createNotification(notification: Notification): Result<Notification> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(MockData.mockNotification)
                }

                val url = URL("$BASE_URL/notifications")
                val connection = createPostConnection(url)

                val jsonObject = JSONObject().apply {
                    put("userId", notification.userId)
                    put("type", notification.type)
                    put("content", notification.content)
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
     * Gets all unread notifications for a user
     * @param userId ID of the user
     * @return Result<List<Notification>> containing all unread notifications
     * 
     * Response:
     * {
     *   "notifications": [
     *     {
     *       "id": Long,
     *       "userId": Long,
     *       "type": String,
     *       "content": String,
     *       "timestamp": String,
     *       "isRead": Boolean
     *     }
     *   ]
     * }
     */
    suspend fun getUnreadNotifications(userId: Long): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(MockData.mockNotifications)
                }

                val url = URL("$BASE_URL/notifications/user/$userId/unread")
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
                handleNetworkError(e, "getting unread notifications")
            }
        }
    }

    /**
     * Gets notifications by type
     * @param type Type of notifications to retrieve
     * @return Result<List<Notification>> containing notifications of the specified type
     * 
     * Response:
     * {
     *   "notifications": [
     *     {
     *       "id": Long,
     *       "userId": Long,
     *       "type": String,
     *       "content": String,
     *       "timestamp": String,
     *       "isRead": Boolean
     *     }
     *   ]
     * }
     */
    suspend fun getNotificationsByType(type: String): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(MockData.mockNotifications)
                }

                val url = URL("$BASE_URL/notifications/type/$type")
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
                handleNetworkError(e, "getting notifications by type")
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
     * POST / - Create new availability
     * GET /{id} - Get availability by ID
     * GET /tutor/{tutorId} - Get all availability for a tutor
     * GET /day/{dayOfWeek} - Get availability by day of week
     * GET /tutor/{tutorId}/day/{dayOfWeek} - Get tutor availability by day
     * PUT /{id} - Update availability
     * DELETE /{id} - Delete availability
     * DELETE /tutor/{tutorId} - Delete all availability for a tutor
     * GET /check-availability - Check if a time slot is available
     */

    /**
     * Gets availability for a specific day of the week
     * @param dayOfWeek Day of the week (e.g., "MONDAY", "TUESDAY")
     * @return Result<List<TutorAvailability>> containing all availability slots for the day
     * 
     * Response:
     * {
     *   "availability": [
     *     {
     *       "id": Long,
     *       "tutorId": Long,
     *       "dayOfWeek": String,
     *       "startTime": String,
     *       "endTime": String,
     *       "isAvailable": Boolean
     *     }
     *   ]
     * }
     */
    suspend fun getAvailabilityByDay(dayOfWeek: String): Result<List<TutorAvailability>> {
        return withContext(Dispatchers.IO) {
            try {
                if (DEBUG_MODE) {
                    return@withContext Result.success(
                        listOf(
                            TutorAvailability(
                                id = 1,
                                tutorId = 1,
                                dayOfWeek = dayOfWeek,
                                startTime = "09:00",
                                endTime = "17:00"
                            )
                        )
                    )
                }

                val params = mapOf("dayOfWeek" to dayOfWeek)
                val url = createUrlWithParams("$BASE_URL/tutor-availability/day", params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "availability")
                    val availability = mutableListOf<TutorAvailability>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        availability.add(
                            TutorAvailability(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                dayOfWeek = json.getString("dayOfWeek"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime"),
                                isAvailable = json.optBoolean("isAvailable", true)
                            )
                        )
                    }
                    availability
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting availability by day")
            }
        }
    }

    /**
     * Gets availability for a tutor on a specific day
     * @param tutorId ID of the tutor
     * @param dayOfWeek Day of the week (e.g., "MONDAY", "TUESDAY")
     * @return Result<List<TutorAvailability>> containing the tutor's availability for the day
     * 
     * Response:
     * {
     *   "availability": [
     *     {
     *       "id": Long,
     *       "tutorId": Long,
     *       "dayOfWeek": String,
     *       "startTime": String,
     *       "endTime": String,
     *       "isAvailable": Boolean
     *     }
     *   ]
     * }
     */
    suspend fun getTutorAvailabilityByDay(tutorId: Long, dayOfWeek: String): Result<List<TutorAvailability>> {
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
                                endTime = "17:00"
                            )
                        )
                    )
                }

                val params = mapOf("dayOfWeek" to dayOfWeek)
                val url = createUrlWithParams("$BASE_URL/tutor-availability/tutor/$tutorId/day", params)
                val connection = createGetConnection(url)

                return@withContext handleResponse(connection) { response ->
                    val jsonArray = parseJsonArrayResponse(response, "availability")
                    val availability = mutableListOf<TutorAvailability>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        availability.add(
                            TutorAvailability(
                                id = json.getLong("id"),
                                tutorId = json.getLong("tutorId"),
                                dayOfWeek = json.getString("dayOfWeek"),
                                startTime = json.getString("startTime"),
                                endTime = json.getString("endTime"),
                                isAvailable = json.optBoolean("isAvailable", true)
                            )
                        )
                    }
                    availability
                }
            } catch (e: Exception) {
                handleNetworkError(e, "getting tutor availability by day")
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
                                notes = json.optString("notes")
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
                                notes = json.optString("notes")
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
                                notes = json.optString("notes")
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
                HttpURLConnection.HTTP_OK -> {
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

                val url = URL("$BASE_URL/messages/$messageId/read")
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

                val url = URL("$BASE_URL/notifications/$notificationId/read")
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

                val url = URL("$BASE_URL/notifications/user/$userId/read-all")
                val connection = createPutConnection(url)

                return@withContext handleResponse(connection) { _ -> Unit }
            } catch (e: Exception) {
                handleNetworkError(e, "marking all notifications as read")
            }
        }
    }
} 