package com.mobile.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

    private lateinit var viewModel: ChatViewModel
    private lateinit var conversationAdapter: ConversationAdapter
    private var currentUserId: Long = -1

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
                } else {
                    emptyStateLayout.visibility = View.GONE
                    conversationsRecyclerView.visibility = View.VISIBLE
                    conversationAdapter.submitList(conversations)
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
        viewModel.loadConversations(currentUserId)
    }

    private fun navigateToMessageActivity(conversation: NetworkUtils.Conversation) {
        // Determine the other user's ID (not the current user)
        val otherUserId = if (conversation.user1Id == currentUserId) {
            conversation.user2Id
        } else {
            conversation.user1Id
        }

        // Determine which user name to display (the one that's not the current user)
        val otherUserName = if (conversation.user1Id == currentUserId) {
            conversation.user2Name
        } else {
            conversation.user1Name
        }

        // Create intent for MessageActivity
        val intent = Intent(requireContext(), MessageActivity::class.java).apply {
            putExtra("CONVERSATION_ID", conversation.id)
            putExtra("OTHER_USER_ID", otherUserId)
            putExtra("OTHER_USER_NAME", otherUserName)
            // Pass the full conversation object for more precise handling
            putExtra("CONVERSATION", conversation)
        }
        startActivity(intent)
    }

    companion object {
        fun newInstance() = ChatFragment()
        private const val REQUEST_SELECT_USER = 1001
    }
}
