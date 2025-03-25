package com.mobile.data.model

import java.util.Date

data class Booking(
    val id: String,
    val learnerId: String,
    val mentorId: String,
    val scheduleId: String,
    val status: String,
    val createdAt: Date
) 