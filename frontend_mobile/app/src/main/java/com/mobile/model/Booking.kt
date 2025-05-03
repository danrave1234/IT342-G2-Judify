package com.mobile.model

import java.util.Date

data class Booking(
    val id: String,
    val learnerId: String,
    val tutorId: String,
    val scheduleId: String,
    val status: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val subject: String? = null,
    val sessionType: String? = null,
    val notes: String? = null,
    val createdAt: Date
) 