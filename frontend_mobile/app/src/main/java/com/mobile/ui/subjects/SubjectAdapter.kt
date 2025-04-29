package com.mobile.ui.subjects

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.R
import com.mobile.data.model.Subject

class SubjectAdapter(
    private val subjects: MutableList<Subject> = mutableListOf(),
    private val onSubjectClicked: (Subject) -> Unit
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subjectNameTextView: TextView = itemView.findViewById(R.id.subjectNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun getItemCount(): Int = subjects.size

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjects[position]
        holder.subjectNameTextView.text = subject.name
        
        holder.itemView.setOnClickListener {
            onSubjectClicked(subject)
        }
    }

    fun updateSubjects(newSubjects: List<Subject>) {
        subjects.clear()
        subjects.addAll(newSubjects)
        notifyDataSetChanged()
    }
} 