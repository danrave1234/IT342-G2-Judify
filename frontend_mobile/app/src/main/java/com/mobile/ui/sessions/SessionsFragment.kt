package com.mobile.ui.sessions

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
        recyclerView.adapter = sessionAdapter

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            loadTutorSessions()
        }

        // Load sessions
        loadTutorSessions()
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
                val result = NetworkUtils.getTutorSessions(userId.toString())

                // Hide loading indicator
                swipeRefreshLayout.isRefreshing = false

                if (result.isSuccess) {
                    val sessions = result.getOrNull() ?: emptyList()
                    if (sessions.isNotEmpty()) {
                        // Update session count label
                        sessionCountLabel.text = "Your tutoring sessions (${sessions.size})"

                        noSessionsContainer.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        sessionAdapter.submitList(sessions)
                        Log.d(TAG, "Loaded ${sessions.size} sessions for tutor")
                    } else {
                        sessionCountLabel.text = "Your tutoring sessions"
                        noSessionsText.text = "No sessions found"
                        noSessionsContainer.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        Log.d(TAG, "No sessions found for tutor")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Error loading sessions: ${error?.message}", error)
                    sessionCountLabel.text = "Your tutoring sessions"
                    noSessionsText.text = "Unable to load your sessions. Please try again."
                    noSessionsContainer.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load sessions: ${error?.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                swipeRefreshLayout.isRefreshing = false
                Log.e(TAG, "Exception loading sessions: ${e.message}", e)
                sessionCountLabel.text = "Your tutoring sessions"
                noSessionsText.text = "Unable to load your sessions. Please try again."
                noSessionsContainer.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
