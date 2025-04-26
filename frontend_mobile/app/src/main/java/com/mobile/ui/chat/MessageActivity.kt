package com.mobile.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.ui.chat.adapters.MessageAdapter
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity for displaying and sending messages in a conversation
 */
class MessageActivity : AppCompatActivity() {
    private val TAG = "MessageActivity"

    // UI components
    private lateinit var toolbar: Toolbar
    private lateinit var profileImage: CircleImageView
    private lateinit var contactNameText: TextView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var messageAdapter: MessageAdapter
    private var conversationId: Long = -1
    private var otherUserId: Long = -1
    private var currentUserId: Long = -1
    private var otherUserName: String = ""
    private var messagesRefreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        // Get conversation details from intent
        conversationId = intent.getLongExtra("CONVERSATION_ID", -1)
        
        // Get current user details
        currentUserId = PreferenceUtils.getUserId(this) ?: -1

        if (conversationId == -1L || currentUserId == -1L) {
            Log.e(TAG, "Invalid conversation ID or user ID")
            finish()
            return
        }
        
        // Extract conversation details - we need to determine other user ID and name
        val conversation = intent.getParcelableExtra<NetworkUtils.Conversation>("CONVERSATION")
        if (conversation != null) {
            determineOtherUserDetails(conversation)
        } else {
            // We need to use the passed other user ID and name as fallback
            otherUserId = intent.getLongExtra("OTHER_USER_ID", -1)
            otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "User"
            
            if (otherUserId == -1L) {
                Log.e(TAG, "Invalid other user ID")
                finish()
                return
            }
        }

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadMessages()
        startMessageRefreshJob()
    }
    
    /**
     * Determine the other user's details from the conversation
     */
    private fun determineOtherUserDetails(conversation: NetworkUtils.Conversation) {
        otherUserId = if (conversation.user1Id == currentUserId) {
            // If current user is user1, then other user is user2
            conversation.user2Id
        } else {
            // Otherwise, other user is user1
            conversation.user1Id
        }
        
        otherUserName = if (conversation.user1Id == currentUserId) {
            // If current user is user1, show user2's name
            conversation.user2Name
        } else {
            // Otherwise, show user1's name
            conversation.user1Name
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesRefreshJob?.cancel()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        profileImage = findViewById(R.id.profileImage)
        contactNameText = findViewById(R.id.contactNameText)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        progressBar = findViewById(R.id.progressBar)

        // Set other user name
        contactNameText.text = otherUserName
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(currentUserId)

        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Show newest messages at the bottom
        }

        messagesRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = messageAdapter
            itemAnimator = null // Disable animations for better performance
        }
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageEditText.text.clear()
            }
        }
    }

    private fun loadMessages() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = NetworkUtils.getMessages(conversationId)

                result.onSuccess { messages ->
                    showLoading(false)

                    if (messages.isEmpty()) {
                        showEmptyState(true)
                    } else {
                        showEmptyState(false)
                        messageAdapter.submitList(messages)
                        scrollToBottom()

                        // Mark messages as read
                        markMessagesAsRead()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Error loading messages: ${error.message}")
                    showLoading(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading messages: ${e.message}")
                showLoading(false)
            }
        }
    }

    private fun sendMessage(content: String) {
        lifecycleScope.launch {
            try {
                // Make sure we have the other user ID
                if (otherUserId == -1L) {
                    Log.e(TAG, "Error sending message: Other user ID not found")
                    return@launch
                }

                val result = NetworkUtils.sendMessage(conversationId, currentUserId, otherUserId, content)

                result.onSuccess { message ->
                    // Instead of manually adding the message, refresh all messages from the server
                    refreshMessages()
                }.onFailure { error ->
                    Log.e(TAG, "Error sending message: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message: ${e.message}")
            }
        }
    }

    private fun markMessagesAsRead() {
        lifecycleScope.launch {
            try {
                NetworkUtils.markAllMessagesAsRead(conversationId, currentUserId)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking messages as read: ${e.message}")
            }
        }
    }

    private fun startMessageRefreshJob() {
        messagesRefreshJob?.cancel()
        messagesRefreshJob = lifecycleScope.launch {
            while (true) {
                delay(5000) // Refresh every 5 seconds
                refreshMessages()
            }
        }
    }

    private fun refreshMessages() {
        lifecycleScope.launch {
            try {
                val result = NetworkUtils.getMessages(conversationId)

                result.onSuccess { messages ->
                    // Always update the message list to ensure we have the latest messages
                    messageAdapter.submitList(messages)
                    scrollToBottom()
                    markMessagesAsRead()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing messages: ${e.message}")
            }
        }
    }

    private fun scrollToBottom() {
        if (messageAdapter.itemCount > 0) {
            messagesRecyclerView.post {
                messagesRecyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        messagesRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        messagesRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
} 
