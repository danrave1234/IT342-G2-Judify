package com.mobile.data.model

data class Schedule(
    val id: String,
    val mentorId: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val isAvailable: Boolean = true
) 