package com.mobile.repository

import com.mobile.model.Conversation
import com.mobile.model.Message
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Repository for handling message operations
 */
class MessageRepository {
    // Define multiple date formats to try when parsing timestamps
    private val timeFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )

    // Primary format for formatting timestamps when sending messages
    private val primaryTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    /**
     * Helper method to parse a timestamp string using multiple formats
     * @param timestampStr The timestamp string to parse
     * @return The timestamp in milliseconds, or null if parsing failed
     */
    private fun parseTimestamp(timestampStr: String): Long? {
        for (format in timeFormats) {
            try {
                return format.parse(timestampStr)?.time
            } catch (e: ParseException) {
                // Try next format
            }
        }
        Log.w("MessageRepository", "Failed to parse timestamp: $timestampStr")
        return null
    }

    /**
     * Get conversation details
     * @param conversationId ID of the conversation
     * @return Result containing the conversation
     */
    suspend fun getConversation(conversationId: Long): Result<Conversation> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MessageRepository", "Getting conversation with ID: $conversationId")
                val networkResult = NetworkUtils.getConversation(conversationId)

                Log.d("MessageRepository", "Conversation result: ${networkResult.isSuccess}")
                if (networkResult.isFailure) {
                    Log.e("MessageRepository", "Error: ${networkResult.exceptionOrNull()?.message}")
                }

                networkResult.map { networkConversation ->
                    Log.d("MessageRepository", "Mapping conversation: studentId=${networkConversation.studentId}, tutorId=${networkConversation.tutorId}")
                    Conversation(
                        id = networkConversation.id,
                        studentId = networkConversation.studentId,
                        tutorId = networkConversation.tutorId,
                        studentName = networkConversation.studentName,
                        tutorName = networkConversation.tutorName,
                        lastMessage = networkConversation.lastMessage,
                        // Convert timestamp string to Long if available
                        lastMessageTime = networkConversation.lastMessageTime?.let { parseTimestamp(it) },
                        unreadCount = networkConversation.unreadCount
                    )
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Exception in getConversation: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get messages for a conversation
     * @param conversationId ID of the conversation
     * @return Result containing the list of messages
     */
    suspend fun getMessages(conversationId: Long): Result<List<Message>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MessageRepository", "Getting messages for conversation ID: $conversationId")
                val networkResult = NetworkUtils.getMessages(conversationId)

                Log.d("MessageRepository", "Messages result: ${networkResult.isSuccess}")
                if (networkResult.isFailure) {
                    Log.e("MessageRepository", "Error getting messages: ${networkResult.exceptionOrNull()?.message}")
                }

                networkResult.map { networkMessages ->
                    Log.d("MessageRepository", "Processing ${networkMessages.size} messages")

                    networkMessages.map { networkMessage ->
                        Message(
                            id = networkMessage.id,
                            conversationId = networkMessage.conversationId,
                            senderId = networkMessage.senderId,
                            // Use a placeholder for receiverId since the NetworkUtils.Message doesn't include it
                            receiverId = 0, // Will be updated later if needed
                            content = networkMessage.content,
                            timestamp = parseTimestamp(networkMessage.timestamp) ?: System.currentTimeMillis(),
                            readStatus = networkMessage.isRead
                        )
                    }
                    // Note: We're not sorting here anymore as the MessageActivity will handle sorting
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Exception in getMessages: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Send a message
     * @param message Message to send
     * @return Result containing the sent message
     */
    suspend fun sendMessage(message: Message): Result<Message> {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = primaryTimeFormat.format(Date(message.timestamp))

                val networkResult = NetworkUtils.sendMessage(
                    message.conversationId,
                    message.senderId,
                    message.receiverId,
                    message.content
                )

                networkResult.map { networkMessage ->
                    Message(
                        id = networkMessage.id,
                        conversationId = networkMessage.conversationId,
                        senderId = networkMessage.senderId,
                        receiverId = message.receiverId, // Use the receiver ID from the original message
                        content = networkMessage.content,
                        timestamp = parseTimestamp(networkMessage.timestamp) ?: System.currentTimeMillis(),
                        readStatus = networkMessage.isRead
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 
