package com.example.campusconnect.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.OnlineEvent
import com.example.campusconnect.data.models.EventCategory
import com.example.campusconnect.data.models.EventRegistration
import com.example.campusconnect.data.models.RegistrationStatus
import java.util.Date

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
                            // Always ensure the in-memory id matches the Firestore document id
                            e?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
                        } catch (ex: Exception) {
                            null
                        }
                    }
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
        // We generate a UUID here and use it both as the object id and the Firestore document id.
        // This keeps navigation by event.id consistent with Firestore lookups by document id.
        val id = java.util.UUID.randomUUID().toString()
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
        // Registration ids are independent; the important part is that eventId here
        // matches an OnlineEvent.id / Firestore document id.
        val id = java.util.UUID.randomUUID().toString()
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
                            // Align in-memory id with Firestore document id if missing
                            r?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
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
