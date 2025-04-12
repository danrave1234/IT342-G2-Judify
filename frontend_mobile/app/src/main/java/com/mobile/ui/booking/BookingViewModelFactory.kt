package com.mobile.ui.booking

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BookingViewModelFactory(
    private val application: Application,
    private val tutorId: Long,
    private val courseId: Long = -1,
    private val courseTitle: String? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookingViewModel(application, tutorId, courseId, courseTitle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 