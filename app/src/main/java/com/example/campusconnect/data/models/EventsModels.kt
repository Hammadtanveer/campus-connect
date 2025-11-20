package com.example.campusconnect.data.models

import com.google.firebase.Timestamp

enum class EventCategory {
    ACADEMIC, WORKSHOP, CULTURAL, SOCIAL
}

enum class RegistrationStatus {
    REGISTERED, CANCELLED
}

data class OnlineEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Timestamp? = null,
    val durationMinutes: Long = 0,
    val organizerId: String = "",
    val meetLink: String = "",
    val category: EventCategory = EventCategory.SOCIAL,
    val maxParticipants: Int = 0,
    val createdAt: Timestamp? = null,
    val organizerName: String = ""
)

data class EventRegistration(
    val id: String = "",
    val userId: String = "",
    val eventId: String = "",
    val registeredAt: Timestamp? = null,
    val status: RegistrationStatus = RegistrationStatus.REGISTERED
)


