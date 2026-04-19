package com.hammadtanveer.campusconnect.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.data.models.OnlineEvent
import com.hammadtanveer.campusconnect.data.models.EventCategory
import com.hammadtanveer.campusconnect.data.models.EventRegistration
import com.hammadtanveer.campusconnect.data.models.RegistrationStatus
import com.hammadtanveer.campusconnect.data.models.EventType
import com.hammadtanveer.campusconnect.util.FirestoreErrorMapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing event operations.
 *
 * Uses dependency injection for Firestore instance.
 */
@Singleton
class EventsRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val adminActivityLogRepository: AdminActivityLogRepository,
    private val auth: FirebaseAuth
) {

    fun observeEvents(): Flow<Resource<List<OnlineEvent>>> = callbackFlow {
        trySend(Resource.Loading)

        if (auth.currentUser == null) {
            trySend(Resource.Error("Please sign in to view meetings and announcements."))
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = db.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(FirestoreErrorMapper.toUserMessage(error, auth.currentUser != null)))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val events = snapshot.documents.mapNotNull { doc ->
                        try {
                            val e = doc.toObject(OnlineEvent::class.java)
                            // Always ensure the in-memory id matches the Firestore document id
                            e?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
                        } catch (_: Exception) {
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
        organizerRole: String,
        category: EventCategory,
        eventType: EventType,
        venue: String,
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
            venue = venue,
            category = category,
            eventType = eventType,
            maxParticipants = maxParticipants,
            createdAt = Timestamp(Date()),
            organizerName = organizerName,
            createdBy = organizerId,
            createdByRole = organizerRole
        )

        db.collection("events").document(id)
            .set(event)
            .addOnSuccessListener {
                adminActivityLogRepository.logActionAsync(
                    action = "Event created: $title",
                    userName = organizerName,
                    type = "event_created",
                    userId = organizerId
                )
                onResult(true, null)
            }
            .addOnFailureListener { e -> onResult(false, FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null)) }
    }

    fun updateEvent(
        eventId: String,
        title: String,
        description: String,
        durationMinutes: Long,
        eventType: EventType,
        venue: String,
        maxParticipants: Int,
        meetLink: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val updates = mapOf(
            "title" to title,
            "description" to description,
            "durationMinutes" to durationMinutes,
            "eventType" to eventType.name,
            "venue" to venue,
            "maxParticipants" to maxParticipants,
            "meetLink" to meetLink
        )

        db.collection("events").document(eventId)
            .update(updates)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null)) }
    }

    fun deleteEvent(eventId: String, onResult: (Boolean, String?) -> Unit) {
        db.collection("events").document(eventId)
            .delete()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null)) }
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
            .addOnFailureListener { e -> onResult(false, FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null)) }
    }

    fun observeMyRegistrations(userId: String): Flow<Resource<List<EventRegistration>>> = callbackFlow {
        trySend(Resource.Loading)
        val registration = db.collection("registrations")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(FirestoreErrorMapper.toUserMessage(error, auth.currentUser != null)))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val regs = snapshot.documents.mapNotNull { doc ->
                        try {
                            val r = doc.toObject(EventRegistration::class.java)
                            // Align in-memory id with Firestore document id if missing
                            r?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(regs))
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getEvent(eventId: String): Resource<OnlineEvent> {
        return try {
            val doc = db.collection("events").document(eventId).get().await()
            val event = doc.toObject(OnlineEvent::class.java)?.copy(id = doc.id)
            if (event != null) {
                Resource.Success(event)
            } else {
                Resource.Error("Event not found")
            }
        } catch (e: Exception) {
            Resource.Error(FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null))
        }
    }

    suspend fun getParticipantCount(eventId: String): Int {
        return try {
            val snapshot = db.collection("registrations").whereEqualTo("eventId", eventId).get().await()
            snapshot.size()
        } catch (_: Exception) {
            0
        }
    }
}
