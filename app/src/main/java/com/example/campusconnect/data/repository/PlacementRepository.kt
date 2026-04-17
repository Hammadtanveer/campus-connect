package com.example.campusconnect.data.repository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.Placement
import com.example.campusconnect.util.FirestoreErrorMapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class PlacementRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val adminActivityLogRepository: AdminActivityLogRepository,
    private val auth: FirebaseAuth
) {
    fun observePlacements(): Flow<Resource<List<Placement>>> = callbackFlow {
        trySend(Resource.Loading)

        if (auth.currentUser == null) {
            trySend(Resource.Error("Please sign in to view placement updates."))
            close()
            return@callbackFlow
        }

        val registration = db.collection("placements")
            .orderBy("postedDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(FirestoreErrorMapper.toUserMessage(error, auth.currentUser != null)))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val placements = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Placement::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(placements))
                }
            }
        awaitClose { registration.remove() }
    }
    suspend fun addPlacement(
        placement: Placement,
        actorUserId: String,
        actorUserName: String
    ): Resource<String> {
        if (actorUserId.isBlank()) {
            return Resource.Error("Missing authenticated user")
        }
        return try {
            val docRef = db.collection("placements").add(placement).await()
            adminActivityLogRepository.logActionAsync(
                action = "Job posted: ${placement.role.ifBlank { placement.companyName }}",
                userName = actorUserName.ifBlank { "Unknown" },
                type = "job_posted",
                userId = actorUserId
            )
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null))
        }
    }
    suspend fun deletePlacement(placementId: String): Resource<Unit> {
        if (placementId.isBlank()) {
            return Resource.Error("Invalid placement ID")
        }
        return try {
            db.collection("placements").document(placementId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null))
        }
    }

    suspend fun updatePlacement(placementId: String, placement: Placement): Resource<Unit> {
        if (placementId.isBlank()) {
            return Resource.Error("Invalid placement ID")
        }
        return try {
            db.collection("placements")
                .document(placementId)
                .set(placement.copy(id = placementId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null))
        }
    }

    suspend fun getPlacement(placementId: String): Resource<Placement> {
        return try {
            val doc = db.collection("placements").document(placementId).get().await()
            val placement = doc.toObject(Placement::class.java)?.copy(id = doc.id)
            if (placement != null) {
                Resource.Success(placement)
            } else {
                Resource.Error("Placement not found")
            }
        } catch (e: Exception) {
            Resource.Error(FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null))
        }
    }
}
