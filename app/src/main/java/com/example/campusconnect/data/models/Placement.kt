package com.example.campusconnect.data.models

import com.google.firebase.Timestamp
import java.util.Date

data class Placement(
    val id: String = "",
    val companyName: String = "",
    val role: String = "",
    val description: String = "",
    val salary: String = "", // e.g., "12 LPA"
    val location: String = "",
    val applyLink: String = "",
    val deadline: Date? = null,
    val postedDate: Date = Date(),
    val eligibilityCriteria: String = ""
)
