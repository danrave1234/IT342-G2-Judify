package com.mobile.ui.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mobile.R

class TimeSlotAdapter(
    private var timeSlots: List<String> = emptyList(),
    private val onTimeSlotSelected: (String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private var selectedPosition = -1
    private var unavailableTimeSlots: Set<String> = emptySet()

    inner class TimeSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeSlotCard: MaterialCardView = itemView as MaterialCardView
        val timeSlotText: TextView = itemView.findViewById(R.id.timeSlotText)
        val selectedIndicator: View = itemView.findViewById(R.id.selectedIndicator)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Only allow selection if the time slot is available
                    val timeSlot = timeSlots[position]
                    if (!unavailableTimeSlots.contains(timeSlot)) {
                        val oldSelectedPosition = selectedPosition
                        selectedPosition = position

                        // Update the previously selected item
                        if (oldSelectedPosition != -1) {
                            notifyItemChanged(oldSelectedPosition)
                        }

                        // Update the newly selected item
                        notifyItemChanged(selectedPosition)

                        // Notify the activity about the selection
                        onTimeSlotSelected(timeSlots[position])
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        holder.timeSlotText.text = timeSlot

        // Check if this time slot is unavailable
        val isUnavailable = unavailableTimeSlots.contains(timeSlot)

        // Update the appearance based on selection state and availability
        if (isUnavailable) {
            // Set unavailable state
            holder.timeSlotCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.light_gray)
            )
            holder.timeSlotCard.strokeColor = 
                ContextCompat.getColor(holder.itemView.context, R.color.light_gray)
            holder.timeSlotText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
            )
            holder.selectedIndicator.visibility = View.GONE
            holder.timeSlotCard.isEnabled = false
            holder.timeSlotCard.alpha = 0.5f
        } else if (position == selectedPosition) {
            // Set selected state
            holder.timeSlotCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.primary_blue_light)
            )
            holder.timeSlotCard.strokeColor = 
                ContextCompat.getColor(holder.itemView.context, R.color.primary_blue)
            holder.timeSlotText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.primary_blue)
            )
            holder.selectedIndicator.visibility = View.VISIBLE
            holder.timeSlotCard.isEnabled = true
            holder.timeSlotCard.alpha = 1.0f
        } else {
            // Set unselected state
            holder.timeSlotCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )
            holder.timeSlotCard.strokeColor = 
                ContextCompat.getColor(holder.itemView.context, R.color.light_gray)
            holder.timeSlotText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.text_primary)
            )
            holder.selectedIndicator.visibility = View.GONE
            holder.timeSlotCard.isEnabled = true
            holder.timeSlotCard.alpha = 1.0f
        }
    }

    override fun getItemCount(): Int = timeSlots.size

    fun updateTimeSlots(newTimeSlots: List<String>) {
        timeSlots = newTimeSlots
        selectedPosition = -1
        notifyDataSetChanged()
    }

    /**
     * Set the list of unavailable time slots that should be disabled
     * @param unavailableSlots Set of time slots that are unavailable
     */
    fun setUnavailableTimeSlots(unavailableSlots: Set<String>) {
        unavailableTimeSlots = unavailableSlots
        notifyDataSetChanged()
    }
}
