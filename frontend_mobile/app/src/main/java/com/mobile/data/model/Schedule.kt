package com.mobile.data.model

data class Schedule(
    val id: String,
    val tutorId: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val isAvailable: Boolean = true
) 