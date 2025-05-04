package com.mobile.ui.dashboard

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.utils.NetworkUtils
import java.util.*

class TutorAvailabilityAdapter(
    private var availabilityList: List<NetworkUtils.TutorAvailability> = emptyList(),
    private val onEditClick: (NetworkUtils.TutorAvailability) -> Unit,
    private val onDeleteClick: (NetworkUtils.TutorAvailability) -> Unit
) : RecyclerView.Adapter<TutorAvailabilityAdapter.AvailabilityViewHolder>() {

    private val TAG = "TutorAvailabilityAdapter"

    /**
     * Converts a time string in 24-hour format (HH:mm:ss or HH:mm) to 12-hour format (h:mm a)
     */
    private fun formatTime(timeString: String): String {
        try {
            // Extract hour and minute from the time string (format: HH:mm:ss or HH:mm)
            val timeParts = timeString.split(":")
            if (timeParts.size >= 2) {
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                // Convert to 12-hour format
                val hour12 = when {
                    hour == 0 -> 12 // 00:00 becomes 12:00 AM
                    hour > 12 -> hour - 12 // 13:00 becomes 1:00 PM
                    else -> hour // 10:00 stays as 10:00 AM
                }

                // Determine AM/PM
                val amPm = if (hour < 12) "AM" else "PM"

                // Format the time
                return String.format("%d:%02d %s", hour12, minute, amPm)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting time: $timeString", e)
        }

        // Return original if parsing fails
        return timeString
    }

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

        // Format time range in 12-hour format
        val formattedStartTime = formatTime(availability.startTime)
        val formattedEndTime = formatTime(availability.endTime)
        holder.timeRangeText.text = "$formattedStartTime - $formattedEndTime"

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
