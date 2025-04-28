package com.mobile.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.databinding.ActivityMessageBinding
import com.mobile.model.Conversation
import com.mobile.model.Message
import com.mobile.repository.MessageRepository
import com.mobile.ui.chat.adapters.MessageAdapter
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for displaying and sending messages in a conversation
 */
class MessageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var messageRepository: MessageRepository

    private var conversationId: Long = -1
    private var otherUserId: Long = -1
    private var otherUserName: String = ""
    private var currentUserId: Long = -1
    private var userRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get conversation details from intent
        conversationId = intent.getLongExtra("CONVERSATION_ID", -1L)
        if (conversationId == -1L) {
            // Try alternative key for backward compatibility
            conversationId = intent.getLongExtra("conversationId", -1L)
        }

        otherUserId = intent.getLongExtra("OTHER_USER_ID", -1L)
        if (otherUserId == -1L) {
            // Try alternative key for backward compatibility
            otherUserId = intent.getLongExtra("otherUserId", -1L)
        }

        otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: 
                         intent.getStringExtra("otherUserName") ?: "User"

        // Log the values for debugging
        Log.d("MessageActivity", "conversationId: $conversationId, otherUserId: $otherUserId, otherUserName: $otherUserName")

        // Get current user ID
        currentUserId = PreferenceUtils.getUserId(this) ?: -1L
        userRole = PreferenceUtils.getUserRole(this)

        if (conversationId == -1L) {
            // No valid conversation ID
            Toast.makeText(this, "Error: Invalid conversation ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize repository and adapter
        messageRepository = MessageRepository()
        adapter = MessageAdapter(currentUserId)

        // Set up RecyclerView
        setupRecyclerView()

        // Set up UI with initial conversation name
        binding.toolbar.title = otherUserName
        binding.contactNameText.text = otherUserName
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Fetch conversation details to get the correct IDs and names
        fetchConversationDetails()

        // Load messages
        refreshMessages()

        // Set up send button
        binding.sendButton.setOnClickListener {
            val content = binding.messageEditText.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
                binding.messageEditText.text.clear()
            }
        }
    }

    private fun fetchConversationDetails() {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("MessageActivity", "Fetching conversation details for ID: $conversationId")
            val response = messageRepository.getConversation(conversationId)

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE

                response.onSuccess { conversation ->
                    Log.d("MessageActivity", "Successfully fetched conversation: ${conversation.id}")
                    updateConversationNames(conversation)

                    // Determine the other user ID based on roles if not already set
                    if (otherUserId == -1L) {
                        otherUserId = if (currentUserId == conversation.studentId) {
                            conversation.tutorId
                        } else {
                            conversation.studentId
                        }
                        Log.d("MessageActivity", "Updated otherUserId to: $otherUserId")
                    }
                }.onFailure { error ->
                    Log.e("MessageActivity", "Failed to load conversation details: ${error.message}", error)

                    // Show a shorter version of the error since the full stack trace is already logged
                    val shortErrorMessage = error.message?.let {
                        if (it.length > 100) it.substring(0, 100) + "..." else it
                    } ?: "Unknown error"

                    Toast.makeText(this@MessageActivity, 
                                  "Failed to load conversation details: $shortErrorMessage",
                                  Toast.LENGTH_SHORT).show()

                    // Even if conversation details failed to load, we should still proceed
                    // with loading messages if we have a valid conversation ID
                    if (conversationId > 0) {
                        Log.d("MessageActivity", "Proceeding to load messages despite conversation load failure")
                        refreshMessages()
                    }
                }
            }
        }
    }

    private fun updateConversationNames(conversation: Conversation) {
        try {
            // First determine if the current user is the tutor or the student in this conversation
            val isCurrentUserTutor = currentUserId == conversation.tutorId
            val isCurrentUserStudent = currentUserId == conversation.studentId

            Log.d("MessageActivity", "Current user ID: $currentUserId, Role: $userRole")
            Log.d("MessageActivity", "Conversation tutorId: ${conversation.tutorId}, studentId: ${conversation.studentId}")
            Log.d("MessageActivity", "Is current user tutor: $isCurrentUserTutor, Is current user student: $isCurrentUserStudent")

            // Determine which name to show based on who the current user is in this conversation
            val updatedName = if (isCurrentUserTutor) {
                // If current user is the tutor in this conversation, show the student's name
                conversation.studentName.takeIf { it.isNotEmpty() } ?: "Student"
            } else if (isCurrentUserStudent) {
                // If current user is the student in this conversation, show the tutor's name
                conversation.tutorName.takeIf { it.isNotEmpty() } ?: "Tutor"
            } else {
                // Fallback if user is neither the tutor nor the student (shouldn't happen)
                when (userRole) {
                    "TUTOR" -> conversation.studentName.takeIf { it.isNotEmpty() } ?: "Student"
                    "LEARNER" -> conversation.tutorName.takeIf { it.isNotEmpty() } ?: "Tutor"
                    else -> "Unknown User"
                }
            }

            // Update the toolbar title with the correct name
            binding.toolbar.title = updatedName
            // Update the contact name text in the toolbar
            binding.contactNameText.text = updatedName
            otherUserName = updatedName

            Log.d("MessageActivity", "Updated conversation name to: $updatedName")
        } catch (e: Exception) {
            Log.e("MessageActivity", "Error updating conversation names: ${e.message}", e)
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this).apply {
            // We want newest messages at the bottom, oldest at top - the standard layout for messaging apps
            stackFromEnd = true      // Start from the bottom of the view
            reverseLayout = true     // Reverse chronological order (newest at bottom)
        }

        // Apply the layout manager
        binding.messagesRecyclerView.layoutManager = layoutManager
        binding.messagesRecyclerView.adapter = adapter

        // Auto-scroll to bottom when new messages are added
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.messagesRecyclerView.post {
                    binding.messagesRecyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
        })

        // Add some padding at bottom to ensure messages don't get hidden behind the input box
        binding.messagesRecyclerView.setPadding(
            binding.messagesRecyclerView.paddingLeft,
            binding.messagesRecyclerView.paddingTop,
            binding.messagesRecyclerView.paddingRight,
            resources.getDimensionPixelSize(R.dimen.message_list_bottom_padding) // Define this in dimens.xml
        )
        binding.messagesRecyclerView.clipToPadding = false
    }

    private fun refreshMessages() {
        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("MessageActivity", "Refreshing messages for conversation: $conversationId")
            val result = messageRepository.getMessages(conversationId)

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE

                result.onSuccess { messages ->
                    Log.d("MessageActivity", "Successfully loaded ${messages.size} messages")

                    if (messages.isEmpty()) {
                        // Show empty state if needed
                        binding.emptyStateLayout?.visibility = View.VISIBLE
                        binding.messagesRecyclerView.visibility = View.GONE
                    } else {
                        // Hide empty state if present
                        binding.emptyStateLayout?.visibility = View.GONE
                        binding.messagesRecyclerView.visibility = View.VISIBLE

                        // Ensure messages are sorted by timestamp (newest first for reversed layout)
                        val sortedMessages = messages.sortedByDescending { it.timestamp }

                        // Submit the sorted list to the adapter
                        adapter.submitList(sortedMessages) {
                            // Execute after the list is updated
                            // Scroll to the most recent message
                            binding.messagesRecyclerView.postDelayed({
                                if (sortedMessages.isNotEmpty()) {
                                    binding.messagesRecyclerView.scrollToPosition(sortedMessages.size - 1)
                                }
                            }, 100)
                        }
                    }
                }.onFailure { error ->
                    Log.e("MessageActivity", "Failed to load messages: ${error.message}", error)
                    Toast.makeText(this@MessageActivity, "Failed to load messages: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessage(content: String) {
        // Check if we have valid otherUserId
        if (otherUserId == -1L) {
            Toast.makeText(this, "Error: Invalid recipient", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val message = Message(
                id = 0, // Will be assigned by the server
                conversationId = conversationId,
                senderId = currentUserId,
                receiverId = otherUserId,
                content = content,
                timestamp = System.currentTimeMillis(),
                readStatus = false
            )

            val result = messageRepository.sendMessage(message)

            withContext(Dispatchers.Main) {
                result.onSuccess { message ->
                    // Instead of manually adding the message, refresh all messages from the server
                    refreshMessages()
                }
                result.onFailure {
                    Toast.makeText(this@MessageActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 
