package com.example.campusconnect.data.repository

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.campusconnect.data.Senior
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeniorsRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val mediaManager: MediaManager
) {
    fun observeSeniors(): Flow<Resource<List<Senior>>> = callbackFlow {
        trySend(Resource.Loading)
        val registration = db.collection("seniors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val seniors = snapshot.documents.mapNotNull { doc ->
                        try {
                            val s = doc.toObject(Senior::class.java)
                            s?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(seniors))
                }
            }
        awaitClose { registration.remove() }
    }

    fun addSenior(senior: Senior, onResult: (Boolean, String?) -> Unit) {
        val docRef = if (senior.id.isBlank()) db.collection("seniors").document() else db.collection("seniors").document(senior.id)
        val seniorToSave = senior.copy(id = docRef.id)
        docRef.set(seniorToSave)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun updateSenior(senior: Senior, onResult: (Boolean, String?) -> Unit) {
        if (senior.id.isBlank()) {
            onResult(false, "Invalid Senior ID")
            return
        }
        db.collection("seniors").document(senior.id)
            .set(senior)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun uploadSeniorImage(file: File, onResult: (String?) -> Unit) {
        val requestId = mediaManager.upload(file.absolutePath)
            .option("folder", "${Constants.CLOUDINARY_BASE_FOLDER}/seniors")
            .option("resource_type", "image")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    onResult(url)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    onResult(null)
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
}
