package com.mobile.ui.chat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.data.model.User

class UserSelectionAdapter(private val onUserSelected: (User) -> Unit) : 
    ListAdapter<User, UserSelectionAdapter.UserViewHolder>(UserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }
    
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val roleTextView: TextView = itemView.findViewById(R.id.roleTextView)
        
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserSelected(getItem(position))
                }
            }
        }
        
        fun bind(user: User) {
            val displayName = "${user.firstName} ${user.lastName}"
            nameTextView.text = displayName
            
            // Display role (TUTOR or LEARNER)
            val roleDisplay = when (user.roles) {
                "TUTOR" -> "Tutor"
                "LEARNER" -> "Learner"
                else -> user.roles
            }
            roleTextView.text = roleDisplay
        }
    }
    
    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }
        
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.email == newItem.email &&
                   oldItem.firstName == newItem.firstName &&
                   oldItem.lastName == newItem.lastName
        }
    }
} 