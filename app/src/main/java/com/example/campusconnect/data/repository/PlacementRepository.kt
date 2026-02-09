package com.example.campusconnect.data.repository
import com.google.firebase.firestore.FirebaseFirestore
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.Placement
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class PlacementRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    fun observePlacements(): Flow<Resource<List<Placement>>> = callbackFlow {
        trySend(Resource.Loading)
        val registration = db.collection("placements")
            .orderBy("postedDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
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
    suspend fun addPlacement(placement: Placement): Resource<String> {
        return try {
            val docRef = db.collection("placements").add(placement).await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message)
        }
    }
    suspend fun deletePlacement(placementId: String): Resource<Unit> {
        return try {
            db.collection("placements").document(placementId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message)
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
            Resource.Error(e.message)
        }
    }
}
