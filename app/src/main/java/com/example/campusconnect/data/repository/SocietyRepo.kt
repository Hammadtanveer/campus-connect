package com.example.campusconnect.data.repository

import com.example.campusconnect.data.models.SocietyEvent
import com.example.campusconnect.data.models.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocietyEventRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun addEvent(event: SocietyEvent): Resource<String> {
        return try {
            val docRef = db.collection("events").add(event).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }

    suspend fun getEventById(eventId: String): Resource<SocietyEvent> {
        return try {
            val document = db.collection("events").document(eventId).get().await()
            val event = document.toObject(SocietyEvent::class.java)
            if (event != null) {
                Resource.Success(event.copy(id = document.id))
            } else {
                Resource.Error("Event not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
}
