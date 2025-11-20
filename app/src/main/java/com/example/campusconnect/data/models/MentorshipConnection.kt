package com.example.campusconnect.data.models

import com.google.firebase.Timestamp

data class MentorshipConnection(
    val id: String = "",
    val mentorId: String = "",
    val menteeId: String = "",
    val participants: List<String> = emptyList(),
    val connectedAt: Timestamp = Timestamp.now(),
    val notes: String = ""
)

