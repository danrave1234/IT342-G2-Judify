package com.mobile.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.utils.NetworkUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for the Chat screen
 * Handles loading conversations and messages
 */
class ChatViewModel : ViewModel() {
    private val TAG = "ChatViewModel"

    // LiveData for conversations
    private val _conversations = MutableLiveData<Result<List<NetworkUtils.Conversation>>>()
    val conversations: LiveData<Result<List<NetworkUtils.Conversation>>> = _conversations

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Load conversations for a user
     * @param userId ID of the user
     */
    fun loadConversations(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Use the updated API call method that handles the backend path variable correctly
                val result = NetworkUtils.getConversationsForUser(userId)
                _conversations.value = result
            } catch (e: Exception) {
                _conversations.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load conversations by tutorId
     * @param tutorId ID of the tutor
     */
    fun loadConversationsByTutorId(tutorId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = NetworkUtils.getConversationsByTutorId(tutorId)
                _conversations.value = result
            } catch (e: Exception) {
                _conversations.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load conversations by studentId
     * @param studentId ID of the student
     */
    fun loadConversationsByStudentId(studentId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = NetworkUtils.getConversationsByStudentId(studentId)
                _conversations.value = result
            } catch (e: Exception) {
                _conversations.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new conversation
     * @param participantIds List of user IDs who are participants in the conversation
     * @return Result<Conversation> containing the created conversation
     */
    fun createConversation(participantIds: List<Long>, onComplete: (Result<NetworkUtils.Conversation>) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = NetworkUtils.createConversation(participantIds)
                onComplete(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating conversation: ${e.message}", e)
                onComplete(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a conversation
     * @param conversationId ID of the conversation to delete
     * @param onComplete Callback to be invoked when the operation is complete
     */
    fun deleteConversation(conversationId: Long, onComplete: (Result<Unit>) -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = NetworkUtils.deleteConversation(conversationId)
                onComplete(result)
                // Refresh conversations if deletion was successful
                result.onSuccess {
                    val currentUserId = conversations.value?.getOrNull()?.firstOrNull()?.let { conv ->
                        // Use either studentId or tutorId, whichever is available
                        conv.studentId.takeIf { it > 0 } ?: conv.tutorId
                    }
                    currentUserId?.let { loadConversations(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting conversation: ${e.message}", e)
                onComplete(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }
}
