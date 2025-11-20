package com.example.campusconnect.data.models

import com.google.firebase.Timestamp

data class MentorshipRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val status: String = "pending",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val connectionNotes: String = ""
)
