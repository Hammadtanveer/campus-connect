package com.hammadtanveer.campusconnect.data.repository

import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.data.models.SocietyEvent
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocietyEventRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val cloudinaryUploader: CloudinaryEventImageUploader
) {
    private fun eventsCollection(societyId: String) =
        db.collection("societies").document(societyId).collection("events")

    fun observeEventsBySociety(societyId: String): Flow<Resource<List<SocietyEvent>>> = callbackFlow {
        trySend(Resource.Loading)

        var lastEmitted: List<SocietyEvent>? = null

        val registration = eventsCollection(societyId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load society events"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val events = snapshot.documents.mapNotNull { document ->
                        try {
                            document.toObject(SocietyEvent::class.java)?.copy(id = document.id, societyId = societyId)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (events != lastEmitted) {
                        lastEmitted = events
                        trySend(Resource.Success(events))
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun addEvent(societyId: String, event: SocietyEvent): Resource<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val now = Timestamp.now()
            val eventToSave = event.copy(id = id, societyId = societyId, createdAt = now, updatedAt = now)

            // Keep legacy aliases for older readers while preserving current model fields.
            eventsCollection(societyId).document(id).set(
                mapOf(
                    "id" to eventToSave.id,
                    "societyId" to eventToSave.societyId,
                    "societyName" to eventToSave.societyName,
                    "name" to eventToSave.name,
                    "eventTitle" to eventToSave.name,
                    "description" to "",
                    "date" to eventToSave.date,
                    "time" to eventToSave.time,
                    "venue" to eventToSave.venue,
                    "location" to eventToSave.venue,
                    "coordinator" to eventToSave.coordinator,
                    "studentCoordinator" to eventToSave.coordinator,
                    "convener" to eventToSave.convener,
                    "facultyConvener" to eventToSave.convener,
                    "register" to eventToSave.register,
                    "registrationLink" to eventToSave.register,
                    "posterUrl" to eventToSave.posterUrl,
                    "posterPublicId" to eventToSave.posterPublicId,
                    "createdBy" to eventToSave.createdBy,
                    "createdByRole" to eventToSave.createdByRole,
                    "createdAt" to eventToSave.createdAt,
                    "updatedAt" to eventToSave.updatedAt
                )
            ).await()

            Resource.Success(id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    suspend fun updateEvent(societyId: String, eventId: String, event: SocietyEvent): Resource<Unit> {
        return try {
            val existing = eventsCollection(societyId).document(eventId).get().await().toObject(SocietyEvent::class.java)
            val eventToSave = event.copy(
                id = eventId,
                societyId = societyId,
                createdAt = existing?.createdAt ?: event.createdAt,
                updatedAt = Timestamp.now()
            )

            eventsCollection(societyId).document(eventId).set(
                mapOf(
                    "id" to eventToSave.id,
                    "societyId" to eventToSave.societyId,
                    "societyName" to eventToSave.societyName,
                    "name" to eventToSave.name,
                    "eventTitle" to eventToSave.name,
                    "description" to "",
                    "date" to eventToSave.date,
                    "time" to eventToSave.time,
                    "venue" to eventToSave.venue,
                    "location" to eventToSave.venue,
                    "coordinator" to eventToSave.coordinator,
                    "studentCoordinator" to eventToSave.coordinator,
                    "convener" to eventToSave.convener,
                    "facultyConvener" to eventToSave.convener,
                    "register" to eventToSave.register,
                    "registrationLink" to eventToSave.register,
                    "posterUrl" to eventToSave.posterUrl,
                    "posterPublicId" to eventToSave.posterPublicId,
                    "createdBy" to eventToSave.createdBy,
                    "createdByRole" to eventToSave.createdByRole,
                    "createdAt" to eventToSave.createdAt,
                    "updatedAt" to eventToSave.updatedAt
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    suspend fun deleteEvent(societyId: String, eventId: String): Resource<Unit> {
        return try {
            eventsCollection(societyId).document(eventId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    suspend fun getEventById(societyId: String, eventId: String): Resource<SocietyEvent> {
        return try {
            val document = eventsCollection(societyId).document(eventId).get().await()
            val event = document.toObject(SocietyEvent::class.java)
            if (event != null) {
                Resource.Success(event.copy(id = document.id, societyId = societyId))
            } else {
                Resource.Error("Event not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    suspend fun uploadPoster(societyId: String, file: File): Resource<CloudinaryEventImageUploader.UploadResult> {
        return cloudinaryUploader.uploadPoster(file, societyId)
    }
}
