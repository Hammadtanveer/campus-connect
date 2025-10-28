package com.example.campusconnect

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.util.Date
import java.util.UUID

class EventsRepository(private val db: FirebaseFirestore) {

    fun observeEvents(): Flow<Resource<List<OnlineEvent>>> = callbackFlow {
        trySend(Resource.Loading)
        val registration: ListenerRegistration = db.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val events = snapshot.documents.mapNotNull { doc ->
                        try {
                            val e = doc.toObject(OnlineEvent::class.java)
                            if (e != null && e.id.isBlank()) {
                                // ensure id present
                                e.copy(id = doc.id)
                            } else e
                        } catch (ex: Exception) {
                            null
                        }
                    }.map { ev -> if (ev.id.isBlank()) ev.copy(id = UUID.randomUUID().toString()) else ev }
                    trySend(Resource.Success(events))
                }
            }

        awaitClose { registration.remove() }
    }

    fun createEvent(
        title: String,
        description: String,
        dateTime: Timestamp,
        durationMinutes: Long,
        organizerId: String,
        organizerName: String,
        category: EventCategory,
        maxParticipants: Int = 0,
        meetLink: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        val id = UUID.randomUUID().toString()
        val event = OnlineEvent(
            id = id,
            title = title,
            description = description,
            dateTime = dateTime,
            durationMinutes = durationMinutes,
            organizerId = organizerId,
            meetLink = meetLink,
            category = category,
            maxParticipants = maxParticipants,
            createdAt = Timestamp(Date()),
            organizerName = organizerName
        )
        db.collection("events").document(id)
            .set(event)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun registerForEvent(userId: String, eventId: String, onResult: (Boolean, String?) -> Unit) {
        val id = UUID.randomUUID().toString()
        val reg = EventRegistration(
            id = id,
            userId = userId,
            eventId = eventId,
            registeredAt = Timestamp(Date()),
            status = RegistrationStatus.REGISTERED
        )
        db.collection("registrations").document(id)
            .set(reg)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun observeMyRegistrations(userId: String): Flow<Resource<List<EventRegistration>>> = callbackFlow {
        trySend(Resource.Loading)
        val registration = db.collection("registrations")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val regs = snapshot.documents.mapNotNull { doc ->
                        try {
                            val r = doc.toObject(EventRegistration::class.java)
                            if (r != null && r.id.isBlank()) r.copy(id = doc.id) else r
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(regs))
                }
            }
        awaitClose { registration.remove() }
    }
}

