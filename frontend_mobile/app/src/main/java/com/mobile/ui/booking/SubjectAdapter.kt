package com.mobile.ui.booking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R

class SubjectAdapter(
    private val listener: OnSubjectSelectedListener
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    private val subjects = mutableListOf<Subject>()
    private var selectedPosition = -1

    interface OnSubjectSelectedListener {
        fun onSubjectSelected(subject: Subject)
    }

    data class Subject(
        val id: String,
        val name: String
    )

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subjectCard: CardView = itemView as CardView
        val subjectNameTextView: TextView = itemView.findViewById(R.id.subjectNameTextView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val previousPosition = selectedPosition
                    selectedPosition = position

                    if (previousPosition != -1) {
                        notifyItemChanged(previousPosition)
                    }

                    notifyItemChanged(selectedPosition)
                    listener.onSubjectSelected(subjects[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjects[position]
        holder.subjectNameTextView.text = subject.name

        // Visual indication of selection
        if (position == selectedPosition) {
            holder.subjectCard.setCardBackgroundColor(holder.itemView.context.getColor(R.color.primary_blue))
            holder.subjectNameTextView.setTextColor(holder.itemView.context.getColor(android.R.color.white))
        } else {
            holder.subjectCard.setCardBackgroundColor(holder.itemView.context.getColor(android.R.color.white))
            holder.subjectNameTextView.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        }
    }

    override fun getItemCount(): Int = subjects.size

    fun updateSubjects(newSubjects: List<Subject>) {
        subjects.clear()
        subjects.addAll(newSubjects)
        selectedPosition = -1
        notifyDataSetChanged()
    }

    fun getSelectedSubject(): Subject? {
        return if (selectedPosition != -1 && selectedPosition < subjects.size) {
            subjects[selectedPosition]
        } else {
            null
        }
    }
} 
