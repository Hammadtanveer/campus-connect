package com.example.campusconnect.data.repository

import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.util.UserProfileMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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
                    ?.sortedBy { it.displayName.lowercase() }
                    ?: emptyList()

                trySend(Resource.Success(users))
            }

        awaitClose { registration.remove() }
    }

    suspend fun updateUserPermissions(
        userId: String,
        permissions: Map<String, Boolean>,
        actorUserId: String
    ): Resource<Unit> {
        return try {
            val currentDoc = firestore.collection("users").document(userId).get().await()
            val currentUser = UserProfileMapper.fromDocument(currentDoc)
                ?: return Resource.Error("User not found")

            val isAdminFlag = permissions["is_admin"] == true
            val nextRole = when {
                currentUser.role == "super_admin" -> "super_admin"
                isAdminFlag -> "admin"
                else -> "user"
            }

            val updatePayload = mapOf(
                "permissions" to permissions,
                "isAdmin" to (isAdminFlag || currentUser.role == "super_admin"),
                "role" to nextRole,
                "lastModifiedBy" to actorUserId,
                "lastModifiedAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(userId)
                .set(updatePayload, SetOptions.merge())
                .await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update permissions")
        }
    }

    suspend fun getUserById(userId: String): Resource<UserProfile> {
        if (userId.isBlank()) return Resource.Error("Invalid user id")

        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val user = UserProfileMapper.fromDocument(doc)
            if (user != null) {
                Resource.Success(user)
            } else {
                Resource.Error("User not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load user")
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

