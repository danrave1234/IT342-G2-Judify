package com.mobile.ui.booking

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BookingViewModelFactory(
    private val application: Application,
    private val tutorId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookingViewModel(application, tutorId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 