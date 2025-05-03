package com.mobile.ui.chat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.ui.chat.adapters.UserSelectionAdapter

class UserSelectionActivity : AppCompatActivity() {
    private val TAG = "UserSelectionActivity"
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var retryButton: Button
    
    private lateinit var adapter: UserSelectionAdapter
    private lateinit var viewModel: UserSelectionViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_selection)
        
        // Initialize UI components
        recyclerView = findViewById(R.id.usersRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        retryButton = findViewById(R.id.retryButton)
        
        // Set up action bar
        supportActionBar?.apply {
            title = "Select User"
            setDisplayHomeAsUpEnabled(true)
        }
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[UserSelectionViewModel::class.java]
        
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
        
        // Load users
        viewModel.loadUsers()
    }
    
    private fun setupRecyclerView() {
        adapter = UserSelectionAdapter { user ->
            // Return selected user to calling activity
            val intent = Intent().apply {
                putExtra("SELECTED_USER_ID", user.userId)
                putExtra("SELECTED_USER_NAME", "${user.firstName} ${user.lastName}")
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserSelectionActivity)
            adapter = this@UserSelectionActivity.adapter
        }
    }
    
    private fun setupClickListeners() {
        retryButton.setOnClickListener {
            viewModel.loadUsers()
        }
    }
    
    private fun setupObservers() {
        viewModel.users.observe(this) { result ->
            result?.let { users ->
                adapter.submitList(users)
                progressBar.visibility = View.GONE
                
                if (users.isEmpty()) {
                    errorText.text = "No users found"
                    errorText.visibility = View.VISIBLE
                } else {
                    errorText.visibility = View.GONE
                }
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            
            if (isLoading) {
                errorText.visibility = View.GONE
                retryButton.visibility = View.GONE
            }
        }
        
        viewModel.error.observe(this) { error ->
            if (error != null) {
                errorText.text = error
                errorText.visibility = View.VISIBLE
                retryButton.visibility = View.VISIBLE
            } else {
                errorText.visibility = View.GONE
                retryButton.visibility = View.GONE
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 