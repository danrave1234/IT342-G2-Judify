package com.mobile.model

/**
 * Model class representing a message in a conversation
 */
data class Message(
    val id: Long,
    val conversationId: Long,
    val senderId: Long,
    val receiverId: Long,
    val content: String,
    val timestamp: Long,
    val readStatus: Boolean = false
) 