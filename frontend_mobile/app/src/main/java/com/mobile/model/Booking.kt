package com.mobile.model

import java.util.Date

data class Booking(
    val id: String,
    val studentId: String,
    val tutorId: String,
    val scheduleId: String,
    val status: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val subject: String? = null,
    val sessionType: String? = null,
    val notes: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val locationData: String? = null,
    val meetingLink: String? = null,
    val createdAt: Date
) 
