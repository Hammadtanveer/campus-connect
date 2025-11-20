package com.example.campusconnect.data.models

data class UserProfile(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val course: String = "",
    val branch: String = "",
    val year: String = "",
    val bio: String = "",
    val profilePictureUrl: String = "",
    val eventCount: Int = 0,

    // Mentorship fields (new)
    val isMentor: Boolean = false,
    val mentorshipBio: String = "",
    val expertise: List<String> = emptyList(),
    val mentorshipStatus: String = "available", // values: "available", "busy", "unavailable"

    // New data enhancements
    val mentorshipRating: Double? = null,
    val totalConnections: Int = 0
)
