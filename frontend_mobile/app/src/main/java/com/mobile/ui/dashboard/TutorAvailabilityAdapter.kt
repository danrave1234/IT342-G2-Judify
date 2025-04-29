package com.mobile.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.utils.NetworkUtils

class TutorAvailabilityAdapter(
    private var availabilityList: List<NetworkUtils.TutorAvailability> = emptyList(),
    private val onEditClick: (NetworkUtils.TutorAvailability) -> Unit,
    private val onDeleteClick: (NetworkUtils.TutorAvailability) -> Unit
) : RecyclerView.Adapter<TutorAvailabilityAdapter.AvailabilityViewHolder>() {

    class AvailabilityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayOfWeekText: TextView = itemView.findViewById(R.id.dayOfWeekText)
        val timeRangeText: TextView = itemView.findViewById(R.id.timeRangeText)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailabilityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_availability_slot, parent, false)
        return AvailabilityViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvailabilityViewHolder, position: Int) {
        val availability = availabilityList[position]
        
        // Format day of week for display (e.g., "MONDAY" -> "Monday")
        val formattedDay = availability.dayOfWeek.lowercase().replaceFirstChar { it.uppercase() }
        holder.dayOfWeekText.text = formattedDay
        
        // Format time range (keep in 24-hour format)
        holder.timeRangeText.text = "${availability.startTime} - ${availability.endTime}"
        
        // Set click listeners
        holder.editButton.setOnClickListener {
            onEditClick(availability)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(availability)
        }
    }

    override fun getItemCount(): Int = availabilityList.size

    fun updateData(newAvailabilityList: List<NetworkUtils.TutorAvailability>) {
        availabilityList = newAvailabilityList
        notifyDataSetChanged()
    }
}