package com.mobile.ui.sessions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mobile.R
import com.mobile.ui.adapters.SessionAdapter
import com.mobile.ui.base.BaseFragment
import com.mobile.utils.NetworkUtils
import com.mobile.utils.PreferenceUtils
import kotlinx.coroutines.launch

/**
 * Fragment for displaying tutor sessions.
 * This is separate from the map functionality, fixing the duplicate button issue.
 */
class SessionsFragment : BaseFragment() {
    private val TAG = "SessionsFragment"
    private lateinit var recyclerView: RecyclerView
    private lateinit var noSessionsText: TextView
    private lateinit var noSessionsContainer: LinearLayout
    private lateinit var sessionCountLabel: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var sessionAdapter: SessionAdapter
    
    // Broadcast receiver for session status changes
    private val sessionStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.mobile.SESSION_STATUS_CHANGED") {
                val sessionId = intent.getLongExtra("sessionId", -1)
                val newStatus = intent.getStringExtra("newStatus")
                Log.d(TAG, "Received broadcast: session $sessionId status changed to $newStatus")
                
                // Immediately reload sessions to reflect the changes
                loadTutorSessions()
                
                // Show a toast to inform the user
                context?.let {
                    Toast.makeText(
                        it,
                        "Session #$sessionId status changed to $newStatus",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.fragment_sessions
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Initialize views
        recyclerView = view.findViewById(R.id.sessionsRecyclerView)
        noSessionsText = view.findViewById(R.id.noSessionsText)
        noSessionsContainer = view.findViewById(R.id.noSessionsContainer)
        sessionCountLabel = view.findViewById(R.id.sessionCountLabel)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        sessionAdapter = SessionAdapter()
        
        // Set up session action listeners
        setupSessionActionListeners()
        
        recyclerView.adapter = sessionAdapter

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            loadTutorSessions()
        }

        // Register the broadcast receiver
        requireContext().registerReceiver(
            sessionStatusReceiver,
            IntentFilter("com.mobile.SESSION_STATUS_CHANGED")
        )

        // Load sessions
        loadTutorSessions()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // Unregister the broadcast receiver
        try {
            requireContext().unregisterReceiver(sessionStatusReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }
    
    private fun setupSessionActionListeners() {
        // Handle video call button clicks
        sessionAdapter.setOnVideoCallStartedListener { session ->
            try {
                // Try to launch the video call activity
                val videoCallClassName = "com.mobile.ui.videocall.VideoCallActivity"
                val intent = Intent(requireContext(), Class.forName(videoCallClassName))
                intent.putExtra("SESSION_ID", session.id)
                intent.putExtra("IS_HOST", true) // Tutor is the host
                startActivity(intent)
            } catch (e: Exception) {
                // Fall back to a basic WebRTC implementation or show a helpful message
                Log.e(TAG, "Video call activity not found: ${e.message}")
                Toast.makeText(requireContext(), 
                    "Video call feature not available yet. Try updating the app.", 
                    Toast.LENGTH_SHORT).show()
                
                // For demo purposes, we could try to launch a web-based video call
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://meet.jit.si/${session.id}"))
                    startActivity(intent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to launch web meeting: ${e2.message}")
                }
            }
        }
        
        // Handle session start button clicks
        sessionAdapter.setOnSessionStartedListener { session ->
            // Update session status in the database
            updateSessionStatus(session.id, "in progress")
            
            Toast.makeText(requireContext(), 
                "Session with ${session.studentName} has started", 
                Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateSessionStatus(sessionId: Long, newStatus: String) {
        lifecycleScope.launch {
            try {
                val result = NetworkUtils.updateSessionStatus(sessionId, newStatus)
                if (result.isSuccess) {
                    // Refresh the sessions list
                    loadTutorSessions()
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Error updating session status: ${error?.message}", error)
                    Toast.makeText(requireContext(), 
                        "Failed to update session status: ${error?.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating session status: ${e.message}", e)
                Toast.makeText(requireContext(), 
                    "Error updating session status: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTutorSessions() {
        // Show loading indicator
        swipeRefreshLayout.isRefreshing = true
        noSessionsContainer.visibility = View.GONE

        val userId = PreferenceUtils.getUserId(requireContext())
        if (userId == null) {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show()
            swipeRefreshLayout.isRefreshing = false
            noSessionsContainer.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                // Force fresh data from the server by adding a timestamp parameter
                val timestamp = System.currentTimeMillis()
                Log.d(TAG, "Fetching fresh tutor sessions data at timestamp: $timestamp")
                
                // Add a random parameter to ensure we bypass any caching
                val sessionUrl = "${userId.toString()}?timestamp=$timestamp"
                val result = NetworkUtils.getTutorSessions(sessionUrl)

                // Hide loading indicator
                swipeRefreshLayout.isRefreshing = false

                if (result.isSuccess) {
                    val sessions = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Loaded ${sessions.size} sessions with statuses: ${sessions.map { it.status }.distinct()}")
                    
                    if (sessions.isNotEmpty()) {
                        // Update session count label
                        sessionCountLabel.text = "Your tutoring sessions (${sessions.size})"

                        noSessionsContainer.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        
                        // Update the adapter
                        sessionAdapter.updateSessions(sessions)
                    } else {
                        noSessionsContainer.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Error loading sessions: ${error?.message}", error)
                    Toast.makeText(requireContext(), "Failed to load sessions: ${error?.message}", Toast.LENGTH_SHORT).show()
                    noSessionsContainer.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            } catch (e: Exception) {
                swipeRefreshLayout.isRefreshing = false
                Log.e(TAG, "Exception loading sessions: ${e.message}", e)
                Toast.makeText(requireContext(), "Error loading sessions: ${e.message}", Toast.LENGTH_SHORT).show()
                noSessionsContainer.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
        }
    }
}
