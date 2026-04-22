package com.hammadtanveer.campusconnect.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

enum class EventCategory {
    ACADEMIC, WORKSHOP, CULTURAL, SOCIAL
}

enum class RegistrationStatus {
    REGISTERED, CANCELLED
}

enum class EventType {
    ONLINE, OFFLINE
}

data class OnlineEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Timestamp? = null,
    val duration: Int = 60,
    val organizerId: String = "",
    val meetLink: String = "",
    val venue: String = "",
    val eventType: EventType = EventType.ONLINE,
    val category: EventCategory = EventCategory.SOCIAL,
    val maxParticipants: Int = 0,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val organizerName: String = "",
    val createdBy: String = "",
    val createdByRole: String = "user",
    val participantCount: Int = 0
)

data class EventRegistration(
    val id: String = "",
    val userId: String = "",
    val eventId: String = "",
    val registeredAt: Timestamp? = null,
    val status: RegistrationStatus = RegistrationStatus.REGISTERED
)
