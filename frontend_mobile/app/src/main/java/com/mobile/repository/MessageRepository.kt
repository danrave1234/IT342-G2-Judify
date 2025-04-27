package com.mobile.repository

import com.mobile.model.Conversation
import com.mobile.model.Message
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import android.util.Log

/**
 * Repository for handling message operations
 */
class MessageRepository {
    private val timeFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
    
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
                        lastMessageTime = networkConversation.lastMessageTime?.let {
                            try { timeFormat.parse(it)?.time } catch (e: Exception) { null }
                        },
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
                            timestamp = try {
                                timeFormat.parse(networkMessage.timestamp)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {
                                Log.w("MessageRepository", "Error parsing timestamp: ${networkMessage.timestamp}", e)
                                System.currentTimeMillis()
                            },
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
                val timestamp = timeFormat.format(Date(message.timestamp))
                
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
                        timestamp = try {
                            timeFormat.parse(networkMessage.timestamp)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        },
                        readStatus = networkMessage.isRead
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 