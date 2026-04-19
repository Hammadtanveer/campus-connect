package com.hammadtanveer.campusconnect.data.repository

import com.hammadtanveer.campusconnect.data.models.AdminActivityLogItem
import com.hammadtanveer.campusconnect.data.models.Resource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminActivityLogRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("activity_logs")

    fun observeRecentLogs(limit: Long = 20): Flow<Resource<List<AdminActivityLogItem>>> = callbackFlow {
        trySend(Resource.Loading)

        val registration = collection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load activity logs"))
                    return@addSnapshotListener
                }

                val logs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(AdminActivityLogItem::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Resource.Success(logs))
            }

        awaitClose { registration.remove() }
    }

    suspend fun logAction(
        action: String,
        userName: String,
        type: String,
        userId: String = ""
    ): Resource<Unit> {
        return try {
            val payload = mapOf(
                "action" to action,
                "userName" to userName,
                "type" to type,
                "userId" to userId,
                "timestamp" to Timestamp.now()
            )
            collection.add(payload).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to log activity")
        }
    }

    fun logActionAsync(
        action: String,
        userName: String,
        type: String,
        userId: String = ""
    ) {
        val payload = mapOf(
            "action" to action,
            "userName" to userName,
            "type" to type,
            "userId" to userId,
            "timestamp" to Timestamp.now()
        )
        collection.add(payload)
    }
}

