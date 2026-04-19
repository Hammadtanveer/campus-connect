package com.hammadtanveer.campusconnect.data.repository

import com.hammadtanveer.campusconnect.data.models.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminNotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun queueTopicNotification(
        topic: String,
        title: String,
        body: String,
        type: String,
        createdByUid: String,
        createdByName: String
    ): Resource<Unit> {
        return try {
            val payload = hashMapOf(
                "topic" to topic,
                "title" to title,
                "body" to body,
                "type" to type,
                "status" to "queued",
                "createdAt" to Timestamp.now(),
                "createdByUid" to createdByUid,
                "createdByName" to createdByName,
                "source" to "admin_panel"
            )
            firestore.collection("notification_queue")
                .add(payload)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to queue notification")
        }
    }
}
