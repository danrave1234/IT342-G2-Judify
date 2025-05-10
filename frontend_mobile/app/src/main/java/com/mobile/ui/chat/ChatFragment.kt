package com.mobile.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.ui.base.BaseFragment
import com.mobile.ui.chat.adapters.ConversationAdapter
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Fragment for displaying user conversations
 */
class ChatFragment : BaseFragment() {
    private val TAG = "ChatFragment"

    // UI components
    private lateinit var conversationsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var errorStateLayout: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var retryButton: Button
    private lateinit var startChatButton: Button
    private lateinit var newMessageFab: FloatingActionButton
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: View

    private lateinit var viewModel: ChatViewModel
    private lateinit var conversationAdapter: ConversationAdapter
    private var currentUserId: Long = -1

    // Original list of conversations to filter from
    private var allConversations: List<NetworkUtils.Conversation> = emptyList()

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_chat
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Initialize UI components
        conversationsRecyclerView = view.findViewById(R.id.conversationsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        errorStateLayout = view.findViewById(R.id.errorStateLayout)
        errorText = view.findViewById(R.id.errorText)
        retryButton = view.findViewById(R.id.retryButton)
        startChatButton = view.findViewById(R.id.startChatButton)
        newMessageFab = view.findViewById(R.id.newMessageFab)
        searchIcon = view.findViewById(R.id.searchIcon)

        // Initialize and configure search edit text
        searchEditText = view.findViewById(R.id.searchEditText)
        searchEditText.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            setOnClickListener {
                isFocusableInTouchMode = true
                isFocusable = true
                requestFocus()
            }

            // Set up search functionality
            setOnEditorActionListener { textView, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    val query = textView.text.toString().trim()
                    searchConversations(query)
                    return@setOnEditorActionListener true
                }
                false
            }
        }

        // Set up search icon click listener
        searchIcon.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            searchConversations(query)
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        // Get current user ID
        currentUserId = PreferenceUtils.getUserId(requireContext()) ?: 1L

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        loadConversations()
    }

    override fun onResume() {
        super.onResume()
        // Refresh conversations when returning to this fragment
        loadConversations()
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter { conversation ->
            // Navigate to the MessageActivity with conversation details
            navigateToMessageActivity(conversation)
        }

        conversationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversationAdapter
        }
    }

    private fun setupObservers() {
        // Observe conversations
        viewModel.conversations.observe(viewLifecycleOwner) { result ->
            result.onSuccess { conversations ->
                if (conversations.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    conversationsRecyclerView.visibility = View.GONE
                    allConversations = emptyList()
                } else {
                    emptyStateLayout.visibility = View.GONE
                    conversationsRecyclerView.visibility = View.VISIBLE

                    // Sort conversations by lastMessageTime (most recent first)
                    val sortedConversations = conversations.sortedByDescending { conversation ->
                        // Try to parse lastMessageTime, fallback to createdAt if null
                        conversation.lastMessageTime?.let { timeString ->
                            try {
                                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                format.parse(timeString)?.time
                            } catch (e: Exception) {
                                // If parsing fails, use 0 as fallback
                                0L
                            }
                        } ?: run {
                            // If lastMessageTime is null, try to use createdAt
                            try {
                                val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                                format.parse(conversation.createdAt)?.time ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                        }
                    }

                    // Store the original sorted list for filtering
                    allConversations = sortedConversations

                    // Check if there's a search query
                    val searchQuery = searchEditText.text.toString().trim()
                    if (searchQuery.isNotEmpty()) {
                        // Apply the current search filter
                        searchConversations(searchQuery)
                    } else {
                        // Show all conversations
                        conversationAdapter.submitList(sortedConversations)
                    }
                }
                progressBar.visibility = View.GONE
                errorStateLayout.visibility = View.GONE
            }.onFailure { error ->
                Log.e(TAG, "Error loading conversations: ${error.message}")
                progressBar.visibility = View.GONE

                // Show error
                if (conversationAdapter.itemCount > 0) {
                    // Keep existing data visible if we have any
                    errorStateLayout.visibility = View.GONE
                } else {
                    errorStateLayout.visibility = View.VISIBLE
                    errorText.text = "Failed to load conversations: ${error.message}"
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        retryButton.setOnClickListener {
            errorStateLayout.visibility = View.GONE
            loadConversations()
        }

        startChatButton.setOnClickListener {
            // In a real app, you would navigate to a screen to select users to chat with
            createNewConversation()
        }

        newMessageFab.setOnClickListener {
            createNewConversation()
        }
    }

    private fun createNewConversation() {
        // Navigate to user selection activity
        val intent = Intent(requireContext(), UserSelectionActivity::class.java)
        startActivityForResult(intent, REQUEST_SELECT_USER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_USER && resultCode == android.app.Activity.RESULT_OK && data != null) {
            val selectedUserId = data.getLongExtra("SELECTED_USER_ID", -1L)
            if (selectedUserId != -1L) {
                val participantIds = listOf(currentUserId, selectedUserId)

                viewModel.createConversation(participantIds) { result ->
                    result.onSuccess { conversation ->
                        Log.d(TAG, "Created conversation: ${conversation.id}")
                        // Navigate to the new conversation
                        navigateToMessageActivity(conversation)
                        // Refresh the conversations list
                        loadConversations()
                    }.onFailure { error ->
                        Log.e(TAG, "Error creating conversation: ${error.message}")
                        errorStateLayout.visibility = View.VISIBLE
                        errorText.text = "Failed to create conversation: ${error.message}"
                    }
                }
            }
        }
    }

    private fun loadConversations() {
        val userRole = PreferenceUtils.getUserRole(requireContext())

        when (userRole) {
            "TUTOR" -> {
                val tutorId = PreferenceUtils.getTutorId(requireContext())
                if (tutorId != null) {
                    viewModel.loadConversationsByTutorId(tutorId)
                } else {
                    // Fallback to using userId if tutorId is not available
                    viewModel.loadConversations(currentUserId)
                }
            }
            "LEARNER" -> {
                val studentId = PreferenceUtils.getStudentId(requireContext())
                if (studentId != null) {
                    viewModel.loadConversationsByStudentId(studentId)
                } else {
                    // Fallback to using userId if studentId is not available
                    viewModel.loadConversations(currentUserId)
                }
            }
            else -> {
                // Default fallback
                viewModel.loadConversations(currentUserId)
            }
        }
    }

    /**
     * Search conversations based on the query
     * @param query The search query
     */
    private fun searchConversations(query: String) {
        if (query.isEmpty()) {
            // If query is empty, show all conversations
            conversationAdapter.submitList(allConversations)

            // Update UI visibility
            if (allConversations.isEmpty()) {
                emptyStateLayout.visibility = View.VISIBLE
                conversationsRecyclerView.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                conversationsRecyclerView.visibility = View.VISIBLE
            }
            return
        }

        // Filter conversations based on query
        val filteredConversations = allConversations.filter { conversation ->
            // Search in student name
            conversation.studentName.contains(query, ignoreCase = true) ||
            // Search in tutor name
            conversation.tutorName.contains(query, ignoreCase = true) ||
            // Search in last message if available
            (conversation.lastMessage?.contains(query, ignoreCase = true) == true)
        }

        // Update adapter with filtered list
        conversationAdapter.submitList(filteredConversations)

        // Update UI visibility
        if (filteredConversations.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            conversationsRecyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            conversationsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun navigateToMessageActivity(conversation: NetworkUtils.Conversation) {
        val userRole = PreferenceUtils.getUserRole(requireContext())

        // Determine the other user's ID based on user role
        val otherUserId: Long
        val otherUserName: String

        when (userRole) {
            "TUTOR" -> {
                // For tutors, the other user is the student
                otherUserId = conversation.studentId
                otherUserName = conversation.studentName
            }
            "LEARNER" -> {
                // For students, the other user is the tutor
                otherUserId = conversation.tutorId
                otherUserName = conversation.tutorName
            }
            else -> {
                // Fallback to the old method if role is unknown
                otherUserId = if (conversation.studentId == currentUserId) {
                    conversation.tutorId
                } else {
                    conversation.studentId
                }

                otherUserName = if (conversation.studentId == currentUserId) {
                    conversation.tutorName
                } else {
                    conversation.studentName
                }
            }
        }

        // Log the values before starting activity for debugging
        Log.d(TAG, "Starting MessageActivity with: conversationId=${conversation.id}, otherUserId=$otherUserId, otherUserName=$otherUserName")

        // Create intent for MessageActivity with consistent keys
        val intent = Intent(requireContext(), MessageActivity::class.java).apply {
            putExtra("conversationId", conversation.id)
            putExtra("CONVERSATION_ID", conversation.id)  // Add both versions for compatibility
            putExtra("otherUserId", otherUserId)
            putExtra("OTHER_USER_ID", otherUserId)  // Add both versions for compatibility
            putExtra("otherUserName", otherUserName)
            putExtra("OTHER_USER_NAME", otherUserName)  // Add both versions for compatibility
        }
        startActivity(intent)
    }

    companion object {
        fun newInstance() = ChatFragment()
        private const val REQUEST_SELECT_USER = 1001
    }
}
