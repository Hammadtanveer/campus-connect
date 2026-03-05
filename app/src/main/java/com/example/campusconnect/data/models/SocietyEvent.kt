package com.example.campusconnect.data.models

import com.google.firebase.Timestamp

data class SocietyEvent(
    val id: String = "",
    val societyId: String = "",
    val societyName: String = "",
    val name: String = "",
    val date: String = "",
    val time: String = "",
    val venue: String = "",
    val coordinator: String = "",
    val convener: String = "",
    val register: String = "",
    val posterUrl: String = "",
    val posterPublicId: String = "",
    val createdBy: String = "",
    val createdByRole: String = "user",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
