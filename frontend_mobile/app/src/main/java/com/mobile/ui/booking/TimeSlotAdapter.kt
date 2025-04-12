package com.mobile.ui.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R

class TimeSlotAdapter(
    private var timeSlots: List<String> = emptyList(),
    private val onTimeSlotSelected: (String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private var selectedPosition = -1

    inner class TimeSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeSlotCard: CardView = itemView.findViewById(R.id.timeSlotCard)
        val timeSlotText: TextView = itemView.findViewById(R.id.timeSlotTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        holder.timeSlotText.text = timeSlot
        
        // Update the appearance based on selection state
        if (position == selectedPosition) {
            holder.timeSlotCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.primary_blue)
            )
            holder.timeSlotText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )
        } else {
            holder.timeSlotCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )
            holder.timeSlotText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.primary_blue)
            )
        }
    }

    override fun getItemCount(): Int = timeSlots.size

    fun updateTimeSlots(newTimeSlots: List<String>) {
        timeSlots = newTimeSlots
        selectedPosition = -1
        notifyDataSetChanged()
    }
}