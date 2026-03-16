package com.example.campusconnect.data.repository

import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.campusconnect.data.Senior
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.util.Constants
import com.google.firebase.auth.FirebaseAuth
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
    private val mediaManager: MediaManager,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "SeniorsRepository"
    }

    fun observeSeniors(): Flow<Resource<List<Senior>>> = callbackFlow {
        trySend(Resource.Loading)
        var lastEmitted: List<Senior>? = null

        val seniorsCollection = db.collection("seniors")
        Log.d(TAG, "observeSeniors: executing query on collection=seniors; filters=none")

        val registration = seniorsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeSeniors: snapshot error", error)
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w(TAG, "observeSeniors: snapshot is null")
                    return@addSnapshotListener
                }

                Log.d(TAG, "observeSeniors: documents returned count=${snapshot.size()}")

                val seniors = snapshot.documents.mapNotNull { doc ->
                    try {
                        Log.d(TAG, "observeSeniors: mapping docId=${doc.id}, keys=${doc.data?.keys}")
                        val mapped = doc.toObject(Senior::class.java)
                        if (mapped == null) {
                            Log.w(TAG, "observeSeniors: mapping returned null for docId=${doc.id}")
                            null
                        } else {
                            mapped.copy(id = doc.id)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "observeSeniors: mapping failed for docId=${doc.id}", e)
                        null
                    }
                }.sortedBy { it.name.lowercase() }

                Log.d(TAG, "observeSeniors: mapped seniors count=${seniors.size}")

                if (seniors != lastEmitted) {
                    lastEmitted = seniors
                    Log.d(TAG, "observeSeniors: emitting seniors count=${seniors.size}")
                    trySend(Resource.Success(seniors))
                } else {
                    Log.d(TAG, "observeSeniors: no state change, skip emit")
                }
            }
        awaitClose { registration.remove() }
    }

    fun addSenior(senior: Senior, onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onResult(false, "Not authenticated")
            return
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val role = userDoc.getString("role")?.trim()?.lowercase()
                val isAdmin = userDoc.getBoolean("isAdmin") == true
                val permissions = (userDoc.get("permissions") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                val canCreateSenior = isAdmin ||
                    role in listOf("admin", "super_admin", "superadmin") ||
                    permissions.contains("*:*:*") ||
                    permissions.contains("seniors:add:all")

                if (!canCreateSenior) {
                    onResult(false, "Only admin and super admin can add seniors")
                    return@addOnSuccessListener
                }

                val docRef = if (senior.id.isBlank()) {
                    db.collection("seniors").document()
                } else {
                    db.collection("seniors").document(senior.id)
                }
                val seniorToSave = senior.copy(id = docRef.id)
                docRef.set(seniorToSave)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { onResult(false, it.message) }
            }
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
        mediaManager.upload(file.absolutePath)
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

    fun deleteSenior(seniorId: String, onResult: (Boolean, String?) -> Unit) {
        if (seniorId.isBlank()) {
            onResult(false, "Invalid Senior ID")
            return
        }
        db.collection("seniors").document(seniorId)
            .delete()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }
}
