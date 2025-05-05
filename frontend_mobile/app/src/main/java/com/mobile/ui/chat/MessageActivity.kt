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
import kotlinx.coroutines.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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

    // Polling variables
    private var pollingJob: Job? = null
    private val pollingInterval = 5000L // 5 seconds
    private var lastMessageId: Long = -1
    private var messageList: List<Message> = emptyList()

    // Session-related variables
    private var sessionId: Long = -1
    private var sessionDetails: NetworkUtils.TutoringSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide session details card initially
        binding.sessionDetailsInclude.root.visibility = View.GONE

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

        // Fetch session details if available
        fetchSessionDetails()

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

                    // Determine the other user ID based on roles if not already set or incorrectly set
                    // This is crucial when opening from sessions where otherUserId might not be passed correctly
                    if (otherUserId == -1L || otherUserId == currentUserId) {
                        otherUserId = if (currentUserId == conversation.studentId) {
                            conversation.tutorId
                        } else {
                            conversation.studentId
                        }
                        Log.d("MessageActivity", "Updated otherUserId to: $otherUserId")
                    }

                    updateConversationNames(conversation)
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

                    // Ensure messages are sorted by timestamp (newest first for reversed layout)
                    val sortedMessages = messages.sortedByDescending { it.timestamp }

                    // Update the message list
                    messageList = sortedMessages

                    // Update the last message ID if we have messages
                    if (sortedMessages.isNotEmpty()) {
                        lastMessageId = sortedMessages.first().id
                    }

                    // Update the UI
                    updateMessageList(sortedMessages)
                }.onFailure { error ->
                    Log.e("MessageActivity", "Failed to load messages: ${error.message}", error)
                    Toast.makeText(this@MessageActivity, "Failed to load messages: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessage(content: String) {
        // Even if otherUserId is not determined yet, try to find it
        if (otherUserId == -1L || otherUserId == currentUserId) {
            // Try to determine other user ID in case it wasn't set properly
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val conversation = messageRepository.getConversation(conversationId).getOrNull()
                    if (conversation != null) {
                        withContext(Dispatchers.Main) {
                            otherUserId = if (currentUserId == conversation.studentId) {
                                conversation.tutorId
                            } else {
                                conversation.studentId
                            }
                            Log.d("MessageActivity", "Determined otherUserId before sending: $otherUserId")
                            // Now try to send the message with the updated receiverId
                            if (otherUserId != -1L && otherUserId != currentUserId) {
                                sendMessageInternal(content)
                            } else {
                                Toast.makeText(this@MessageActivity, "Error: Could not determine recipient", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MessageActivity, "Error: Could not load conversation", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("MessageActivity", "Error determining message recipient: ${e.message}", e)
                        Toast.makeText(this@MessageActivity, "Error: Could not determine recipient", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return
        }

        // If we already have a valid otherUserId, send the message directly
        sendMessageInternal(content)
    }

    private fun sendMessageInternal(content: String) {
        Log.d("MessageActivity", "Sending message to user ID: $otherUserId in conversation: $conversationId")

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
                result.onSuccess { sentMessage ->
                    Log.d("MessageActivity", "Message sent successfully: ${sentMessage.id}")

                    // Clear the input field
                    binding.messageEditText.setText("")

                    // The polling mechanism will automatically fetch the new message
                    // No need to call refreshMessages() here

                    // Optionally, we can add the sent message to the UI immediately for better UX
                    // This creates a more responsive feel while waiting for the polling to fetch it
                    val updatedList = ArrayList(messageList)
                    updatedList.add(0, sentMessage) // Add at the beginning (newest first)
                    messageList = updatedList

                    // Update the last message ID
                    if (sentMessage.id > lastMessageId) {
                        lastMessageId = sentMessage.id
                    }

                    // Update the UI
                    updateMessageList(messageList)
                }
                result.onFailure { error ->
                    Log.e("MessageActivity", "Failed to send message: ${error.message}", error)
                    Toast.makeText(this@MessageActivity, "Failed to send message: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    /**
     * Start polling for new messages
     */
    private fun startPolling() {
        // Cancel any existing job
        pollingJob?.cancel()

        // Start a new polling job
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    Log.d("MessageActivity", "Polling for new messages")
                    val result = messageRepository.getMessages(conversationId)

                    result.onSuccess { messages ->
                        // Sort messages by timestamp (newest first)
                        val sortedMessages = messages.sortedByDescending { it.timestamp }

                        // Check if we have new messages
                        if (sortedMessages.isNotEmpty() && 
                            (messageList.isEmpty() || sortedMessages.first().id > lastMessageId)) {

                            // Update the last message ID
                            lastMessageId = sortedMessages.first().id

                            // Update the message list
                            messageList = sortedMessages

                            // Update the UI on the main thread
                            withContext(Dispatchers.Main) {
                                updateMessageList(sortedMessages)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MessageActivity", "Error polling for messages: ${e.message}", e)
                }

                // Wait for the polling interval
                delay(pollingInterval)
            }
        }
    }

    /**
     * Stop polling for new messages
     */
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Update the message list in the UI
     */
    private fun updateMessageList(messages: List<Message>) {
        if (messages.isEmpty()) {
            // Show empty state if needed
            binding.emptyStateLayout?.visibility = View.VISIBLE
            binding.messagesRecyclerView.visibility = View.GONE
        } else {
            // Hide empty state if present
            binding.emptyStateLayout?.visibility = View.GONE
            binding.messagesRecyclerView.visibility = View.VISIBLE

            // Submit the list to the adapter
            adapter.submitList(messages) {
                // Execute after the list is updated
                // Scroll to the most recent message
                binding.messagesRecyclerView.postDelayed({
                    if (messages.isNotEmpty()) {
                        binding.messagesRecyclerView.scrollToPosition(0)
                    }
                }, 100)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Start polling when the activity is resumed
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        // Stop polling when the activity is paused
        stopPolling()
    }

    /**
     * Fetch session details for the current conversation
     */
    private fun fetchSessionDetails() {
        if (conversationId <= 0) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use SessionUtils to get session details
                val result = com.mobile.utils.SessionUtils.getSessionByConversationId(conversationId)

                result.fold(
                    onSuccess = { session ->
                        // Update the session details
                        sessionId = session.id
                        sessionDetails = session

                        // Update the UI on the main thread
                        withContext(Dispatchers.Main) {
                            updateSessionDetailsUI()
                        }
                    },
                    onFailure = { exception ->
                        // Handle error
                        Log.e("MessageActivity", "Error fetching session details: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                Log.e("MessageActivity", "Exception fetching session details: ${e.message}", e)
            }
        }
    }

    /**
     * Update the UI with session details
     */
    private fun updateSessionDetailsUI() {
        val session = sessionDetails ?: return

        try {
            // Get the root view of the included layout
            val sessionDetailsView = binding.sessionDetailsInclude.root

            // Find views in the included layout
            val sessionSubjectText = sessionDetailsView.findViewById<android.widget.TextView>(R.id.sessionSubjectText)
            val sessionDateText = sessionDetailsView.findViewById<android.widget.TextView>(R.id.sessionDateText)
            val sessionTimeText = sessionDetailsView.findViewById<android.widget.TextView>(R.id.sessionTimeText)
            val sessionTypeText = sessionDetailsView.findViewById<android.widget.TextView>(R.id.sessionTypeText)
            val sessionLocationLayout = sessionDetailsView.findViewById<android.widget.LinearLayout>(R.id.sessionLocationLayout)
            val sessionLocationText = sessionDetailsView.findViewById<android.widget.TextView>(R.id.sessionLocationText)
            val sessionPriceText = sessionDetailsView.findViewById<android.widget.TextView>(R.id.sessionPriceText)
            val sessionStatusText = sessionDetailsView.findViewById<android.widget.TextView>(R.id.sessionStatusText)
            val sessionNotesLayout = sessionDetailsView.findViewById<android.widget.LinearLayout>(R.id.sessionNotesLayout)
            val sessionNotesText = sessionDetailsView.findViewById<android.widget.TextView>(R.id.sessionNotesText)
            val tutorActionButtonsLayout = sessionDetailsView.findViewById<android.widget.LinearLayout>(R.id.tutorActionButtonsLayout)
            val approveButton = sessionDetailsView.findViewById<com.google.android.material.button.MaterialButton>(R.id.approveButton)
            val rejectButton = sessionDetailsView.findViewById<com.google.android.material.button.MaterialButton>(R.id.rejectButton)

            // Set session details
            sessionSubjectText.text = session.subject

            // Format and set date
            val dateText = formatDateForDisplay(session.startTime)
            sessionDateText.text = dateText

            // Format and set time
            val timeText = formatTimeForDisplay(session.startTime, session.endTime)
            sessionTimeText.text = timeText

            // Set session type
            sessionTypeText.text = session.sessionType

            // Set location if available
            if (session.sessionType == "In-Person" && session.notes?.contains("location:") == true) {
                sessionLocationLayout.visibility = View.VISIBLE
                val locationText = session.notes.substringAfter("location:").trim()
                sessionLocationText.text = locationText
            } else {
                sessionLocationLayout.visibility = View.GONE
            }

            // Set price (hardcoded for now since TutoringSession doesn't have a price field)
            sessionPriceText.text = "$35.00"

            // Set status
            sessionStatusText.text = session.status

            // Set status color based on status
            when (session.status.uppercase()) {
                "PENDING" -> sessionStatusText.setTextColor(resources.getColor(R.color.warning, null))
                "SCHEDULED" -> sessionStatusText.setTextColor(resources.getColor(R.color.primary_blue, null))
                "COMPLETED" -> sessionStatusText.setTextColor(resources.getColor(R.color.success, null))
                "CANCELLED" -> sessionStatusText.setTextColor(resources.getColor(R.color.error, null))
                else -> sessionStatusText.setTextColor(resources.getColor(R.color.text_primary, null))
            }

            // Set notes
            if (session.notes.isNullOrEmpty()) {
                sessionNotesLayout.visibility = View.GONE
            } else {
                sessionNotesLayout.visibility = View.VISIBLE
                sessionNotesText.text = session.notes
            }

            // Show/hide tutor action buttons based on role and session status
            val userRole = PreferenceUtils.getUserRole(this)
            val isTutor = userRole == "TUTOR"
            val isPending = session.status.uppercase() == "PENDING"

            if (isTutor && isPending) {
                tutorActionButtonsLayout.visibility = View.VISIBLE

                // Set up approve button
                approveButton.setOnClickListener {
                    approveSession(session.id)
                }

                // Set up reject button
                rejectButton.setOnClickListener {
                    rejectSession(session.id)
                }
            } else {
                tutorActionButtonsLayout.visibility = View.GONE
            }

            // Show the session details card
            sessionDetailsView.visibility = View.VISIBLE

            Log.d("MessageActivity", "Session details UI updated successfully")
        } catch (e: Exception) {
            Log.e("MessageActivity", "Error updating session details UI: ${e.message}", e)
        }
    }

    /**
     * Approve a session
     */
    private fun approveSession(sessionId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use SessionUtils to approve the session
                val result = com.mobile.utils.SessionUtils.acceptSession(sessionId)

                result.fold(
                    onSuccess = { session ->
                        // Update the session details
                        sessionDetails = session

                        // Update the UI on the main thread
                        withContext(Dispatchers.Main) {
                            updateSessionDetailsUI()
                            Toast.makeText(this@MessageActivity, "Session approved", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { exception ->
                        // Handle error
                        Log.e("MessageActivity", "Error approving session: ${exception.message}", exception)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MessageActivity, "Error approving session", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("MessageActivity", "Exception approving session: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MessageActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Reject a session
     */
    private fun rejectSession(sessionId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use SessionUtils to reject the session
                val result = com.mobile.utils.SessionUtils.rejectSession(sessionId)

                result.fold(
                    onSuccess = { session ->
                        // Update the session details
                        sessionDetails = session

                        // Update the UI on the main thread
                        withContext(Dispatchers.Main) {
                            updateSessionDetailsUI()
                            Toast.makeText(this@MessageActivity, "Session rejected", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { exception ->
                        // Handle error
                        Log.e("MessageActivity", "Error rejecting session: ${exception.message}", exception)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MessageActivity, "Error rejecting session", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("MessageActivity", "Exception rejecting session: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MessageActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Format a date string for display
     */
    private fun formatDateForDisplay(dateString: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString) ?: return dateString
            return outputFormat.format(date)
        } catch (e: Exception) {
            Log.e("MessageActivity", "Error formatting date: ${e.message}", e)
            return dateString
        }
    }

    /**
     * Format a time string for display
     */
    private fun formatTimeForDisplay(startTimeString: String, endTimeString: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

            val startDate = inputFormat.parse(startTimeString) ?: return "$startTimeString - $endTimeString"
            val endDate = inputFormat.parse(endTimeString) ?: return "$startTimeString - $endTimeString"

            val formattedStartTime = outputFormat.format(startDate)
            val formattedEndTime = outputFormat.format(endDate)

            return "$formattedStartTime - $formattedEndTime"
        } catch (e: Exception) {
            Log.e("MessageActivity", "Error formatting time: ${e.message}", e)
            return "$startTimeString - $endTimeString"
        }
    }
}
