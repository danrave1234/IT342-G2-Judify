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
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        },
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        },
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        },
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC") 
        }
    )

    // Primary format for formatting timestamps when sending messages
    private val primaryTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }

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
                        // Get original content
                        var content = networkMessage.content
                        
                        // For session details messages, filter out Tutor Profile ID
                        if (content.contains("Subject:") && 
                            (content.contains("Date:") || content.contains("Time:")) && 
                            content.contains("Status:")) {
                            
                            // Filter out Tutor Profile ID information with improved regex
                            content = content.replace(Regex("(?i)tutor\\s*profile\\s*i[d|:]+.*?(\\n|$)"), "")
                                           .replace(Regex("\\n\\s*\\n"), "\n") // Remove empty lines
                                           .trim()
                            
                            Log.d("MessageRepository", "Filtered tutor profile ID from message")
                        }
                        
                        // Analyze message content to determine message type and extract session ID if present
                        val messageType: Message.MessageType
                        var sessionId: Long? = null

                        // Check if this is a session details message
                        if (content.contains("Subject:") && 
                            (content.contains("Date:") || content.contains("Time:")) && 
                            content.contains("Status:")) {
                            messageType = Message.MessageType.SESSION_DETAILS

                            // Try various patterns to extract session ID from the message content
                            var extractedSessionId: Long? = null
                            
                            // Pattern 1: "Session ID: 123"
                            val sessionIdRegex = "Session ID:\\s*(\\d+)".toRegex()
                            val matchResult = sessionIdRegex.find(content)
                            if (matchResult != null) {
                                extractedSessionId = matchResult.groupValues[1].toLongOrNull()
                                Log.d("MessageRepository", "Found sessionId with pattern 1: $extractedSessionId")
                            }
                            
                            // Pattern 2: "#123" or "session #123"
                            if (extractedSessionId == null) {
                                val hashIdRegex = "[Ss]ession\\s*#?(\\d+)".toRegex()
                                val hashMatch = hashIdRegex.find(content)
                                if (hashMatch != null) {
                                    extractedSessionId = hashMatch.groupValues[1].toLongOrNull()
                                    Log.d("MessageRepository", "Found sessionId with pattern 2: $extractedSessionId")
                                }
                            }
                            
                            // Pattern 3: Look for IDs in the form of "ID: 123"
                            if (extractedSessionId == null) {
                                val idRegex = "ID:\\s*(\\d+)".toRegex()
                                val idMatch = idRegex.find(content)
                                if (idMatch != null) {
                                    extractedSessionId = idMatch.groupValues[1].toLongOrNull()
                                    Log.d("MessageRepository", "Found sessionId with pattern 3: $extractedSessionId")
                                }
                            }
                            
                            // Set the session ID if any pattern matched
                            sessionId = extractedSessionId
                            Log.d("MessageRepository", "Final sessionId for SESSION_DETAILS message: $sessionId")
                        }
                        // Check if this is a session action message
                        else if (content.contains("approved") || content.contains("rejected") || 
                                content.contains("scheduled") || content.contains("cancelled") || 
                                content.contains("completed")) {
                            messageType = Message.MessageType.SESSION_ACTION

                            // Try to extract session ID from the message content
                            val sessionIdRegex = "[Ss]ession\\s*(?:#|ID:)?\\s*(\\d+)".toRegex()
                            val matchResult = sessionIdRegex.find(content)
                            sessionId = matchResult?.groupValues?.get(1)?.toLongOrNull()

                            Log.d("MessageRepository", "Detected SESSION_ACTION message with sessionId: $sessionId")
                        }
                        // Default to regular text message
                        else {
                            messageType = Message.MessageType.TEXT
                        }

                        Message(
                            id = networkMessage.id,
                            conversationId = networkMessage.conversationId,
                            senderId = networkMessage.senderId,
                            // Use a placeholder for receiverId since the NetworkUtils.Message doesn't include it
                            receiverId = 0, // Will be updated later if needed
                            content = content, // Use the potentially filtered content
                            timestamp = parseTimestamp(networkMessage.timestamp) ?: System.currentTimeMillis(),
                            readStatus = networkMessage.isRead,
                            messageType = messageType,
                            sessionId = sessionId
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
                Log.d("MessageRepository", "Sending message: ${message.content} from ${message.senderId} to ${message.receiverId} in conversation ${message.conversationId}")

                // Format timestamp correctly
                val timestamp = primaryTimeFormat.format(Date(message.timestamp))

                val networkResult = NetworkUtils.sendMessage(
                    message.conversationId,
                    message.senderId,
                    message.receiverId,  // Make sure this is correctly passed to the backend
                    message.content
                )

                networkResult.map { networkMessage ->
                    Log.d("MessageRepository", "Message sent with ID: ${networkMessage.id}")
                    Message(
                        id = networkMessage.id,
                        conversationId = networkMessage.conversationId,
                        senderId = networkMessage.senderId,
                        receiverId = message.receiverId, // Use the receiver ID from the original message
                        content = networkMessage.content,
                        timestamp = parseTimestamp(networkMessage.timestamp) ?: System.currentTimeMillis(),
                        readStatus = networkMessage.isRead,
                        messageType = message.messageType, // Preserve the original message type
                        sessionId = message.sessionId // Preserve the original session ID
                    )
                }
            } catch (e: Exception) {
                Log.e("MessageRepository", "Failed to send message: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
} 
