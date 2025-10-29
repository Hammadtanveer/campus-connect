package com.example.campusconnect.data

/**
 * Data classes representing a senior mentor profile.
 */

data class ContactInfo(
    val email: String,
    val linkedin: String,
    val github: String
)

data class Senior(
    val id: Int,
    val name: String,
    val expertise: String,
    val bio: String,
    val technicalStack: List<String>,
    val availability: String,
    val contact: ContactInfo
)

