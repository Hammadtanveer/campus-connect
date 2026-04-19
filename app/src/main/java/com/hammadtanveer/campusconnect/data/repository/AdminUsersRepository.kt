package com.hammadtanveer.campusconnect.data.repository

import android.util.Log
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.data.models.UserProfile
import com.hammadtanveer.campusconnect.security.PermissionManager
import com.hammadtanveer.campusconnect.util.UserProfileMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminUsersRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun observeUsers(): Flow<Resource<List<UserProfile>>> = callbackFlow {
        trySend(Resource.Loading)

        val registration: ListenerRegistration = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load users"))
                    return@addSnapshotListener
                }

                val users = snapshot?.documents
                    ?.mapNotNull { doc ->
                        try {
                            UserProfileMapper.fromDocument(doc)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    ?.distinctBy { it.email.lowercase().trim() }
                    ?.sortedBy { it.displayName.lowercase() }
                    ?: emptyList()

                Log.d("ADMIN_DEBUG", "Realtime users update: count=${users.size}")

                trySend(Resource.Success(users))
            }

        awaitClose { registration.remove() }
    }

    fun observeUserById(userId: String): Flow<Resource<UserProfile>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(Resource.Error("Invalid user id"))
            close()
            return@callbackFlow
        }

        trySend(Resource.Loading)
        val registration = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to load user"))
                    return@addSnapshotListener
                }

                val user = doc?.let { UserProfileMapper.fromDocument(it) }
                if (user == null) {
                    trySend(Resource.Error("User not found"))
                } else {
                    Log.d("ADMIN_DEBUG", "Realtime user update: userId=$userId permissions=${user.permissions}")
                    trySend(Resource.Success(user))
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun updateUserPermissions(
        userId: String,
        permissions: Map<String, Boolean>,
        actorUserId: String
    ): Resource<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "permissions" to permissions.filterValues { it }.keys
                            .map { PermissionManager.normalizePermission(it) }
                            .distinct(),
                        "lastModifiedBy" to actorUserId,
                        "lastModifiedAt" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()

            Log.d("ADMIN_DEBUG", "Firestore permissions updated = ${permissions.filterValues { it }.keys.toList()}")
            val snap = firestore.collection("users").document(userId).get().await()
            Log.d("FINAL_DB", "${snap.data}")

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update permissions")
        }
    }


    suspend fun deleteUser(userId: String): Resource<Unit> {
        if (userId.isBlank()) return Resource.Error("Invalid user id")

        return try {
            firestore.collection("users").document(userId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete user")
        }
    }
}

