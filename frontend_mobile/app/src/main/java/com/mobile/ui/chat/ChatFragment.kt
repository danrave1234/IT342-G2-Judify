package com.mobile.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.ui.chat.adapters.ConversationAdapter
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils

/**
 * Fragment for displaying user conversations
 */
class ChatFragment : Fragment() {
    private val TAG = "ChatFragment"

    // UI components
    private lateinit var conversationsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: ConstraintLayout
    private lateinit var errorStateLayout: ConstraintLayout
    private lateinit var errorText: TextView
    private lateinit var retryButton: Button
    private lateinit var startChatButton: Button

    private lateinit var viewModel: ChatViewModel
    private lateinit var conversationAdapter: ConversationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Initialize UI components
        conversationsRecyclerView = view.findViewById(R.id.conversationsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        errorStateLayout = view.findViewById(R.id.errorStateLayout)
        errorText = view.findViewById(R.id.errorText)
        retryButton = view.findViewById(R.id.retryButton)
        startChatButton = view.findViewById(R.id.startChatButton)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        loadConversations()
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter { conversation ->
            // Navigate to conversation detail/messages screen
            // In a real app, you would create a MessageActivity to display messages
            // For now, we'll just log the conversation ID
            Log.d(TAG, "Conversation clicked: ${conversation.id}")
            // TODO: Create and navigate to MessageActivity
            // val intent = Intent(requireContext(), MessageActivity::class.java)
            // intent.putExtra("CONVERSATION_ID", conversation.id)
            // startActivity(intent)
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
            }.onFailure { error ->
                Log.e(TAG, "Error loading conversations: ${error.message}")
                progressBar.visibility = View.GONE
                errorStateLayout.visibility = View.VISIBLE
                errorText.text = "Failed to load conversations: ${error.message}"
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
            // For now, we'll just create a mock conversation with the current user and another user
            val currentUserId = 1L // Mock user ID for testing
            val mockParticipantIds = listOf(currentUserId, 2L) // Mock second user with ID 2

            viewModel.createConversation(mockParticipantIds) { result ->
                result.onSuccess { conversation ->
                    Log.d(TAG, "Created conversation: ${conversation.id}")
                    loadConversations()
                }.onFailure { error ->
                    Log.e(TAG, "Error creating conversation: ${error.message}")
                    errorStateLayout.visibility = View.VISIBLE
                    errorText.text = "Failed to create conversation: ${error.message}"
                }
            }
        }
    }

    private fun loadConversations() {
        // Use a mock user ID for testing
        val userId = 1L
        viewModel.loadConversations(userId)
    }

    companion object {
        fun newInstance() = ChatFragment()
    }
}
