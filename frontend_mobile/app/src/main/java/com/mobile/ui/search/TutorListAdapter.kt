package com.mobile.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import de.hdodenhof.circleimageview.CircleImageView

class TutorListAdapter(
    private val onTutorClick: (Long) -> Unit
) : ListAdapter<TutorSearchItem, TutorListAdapter.TutorViewHolder>(TutorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutor, parent, false)
        return TutorViewHolder(view, onTutorClick)
    }

    override fun onBindViewHolder(holder: TutorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TutorViewHolder(
        itemView: View,
        private val onTutorClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val avatarImageView: CircleImageView = itemView.findViewById(R.id.tutorAvatarImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.tutorNameTextView)
        private val expertiseTextView: TextView = itemView.findViewById(R.id.tutorExpertiseTextView)
        private val ratingBadge: TextView = itemView.findViewById(R.id.tutorRatingBadge)
        private val priceTextView: TextView = itemView.findViewById(R.id.tutorPriceTextView)
        private val distanceContainer: LinearLayout = itemView.findViewById(R.id.distanceContainer)
        private val distanceTextView: TextView = itemView.findViewById(R.id.tutorDistanceTextView)
        
        fun bind(tutor: TutorSearchItem) {
            // Set tutor info
            nameTextView.text = tutor.name
            expertiseTextView.text = tutor.expertise
            
            // Format rating with one decimal place
            val formattedRating = String.format("%.1f", tutor.rating)
            ratingBadge.text = formattedRating
            
            // Format price with dollar sign
            priceTextView.text = "$${tutor.pricePerHour}/hr"
            
            // Set avatar placeholder
            // Using default avatar drawable set in XML
            
            // Set distance if available
            if (tutor.distance != null) {
                distanceContainer.visibility = View.VISIBLE
                distanceTextView.text = "${tutor.distance} km"
            } else {
                distanceContainer.visibility = View.GONE
            }
            
            // Set click listener
            itemView.setOnClickListener {
                onTutorClick(tutor.id)
            }
        }
    }
    
    class TutorDiffCallback : DiffUtil.ItemCallback<TutorSearchItem>() {
        override fun areItemsTheSame(oldItem: TutorSearchItem, newItem: TutorSearchItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TutorSearchItem, newItem: TutorSearchItem): Boolean {
            return oldItem == newItem
        }
    }
} 