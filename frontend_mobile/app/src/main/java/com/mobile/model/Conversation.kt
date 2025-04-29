package com.mobile.model

/**
 * Model class representing a conversation between users
 */
data class Conversation(
    val id: Long,
    val studentId: Long,
    val tutorId: Long,
    val studentName: String,
    val tutorName: String,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0
) 