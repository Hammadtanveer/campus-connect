package com.hammadtanveer.campusconnect.data.repository

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.hammadtanveer.campusconnect.data.Senior
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.security.PermissionManager
import com.hammadtanveer.campusconnect.util.CloudinaryConfig
import com.hammadtanveer.campusconnect.util.Constants
import com.hammadtanveer.campusconnect.util.DbgLog
import com.hammadtanveer.campusconnect.util.FirestoreErrorMapper
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
    fun observeSeniors(): Flow<Resource<List<Senior>>> = callbackFlow {
        DbgLog.d("Repo", "observeSeniors start")
        trySend(Resource.Loading)

        if (auth.currentUser == null) {
            trySend(Resource.Error("Please sign in to view seniors."))
            close()
            return@callbackFlow
        }

        var lastEmitted: List<Senior>? = null

        val seniorsCollection = db.collection("seniors")

        val registration = seniorsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    DbgLog.e("Repo", "observeSeniors snapshot error", error)
                    trySend(Resource.Error(FirestoreErrorMapper.toUserMessage(error, auth.currentUser != null)))
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    DbgLog.d("Repo", "observeSeniors snapshot null")
                    return@addSnapshotListener
                }

                DbgLog.d(
                    "Repo",
                    "observeSeniors snapshot docs=${snapshot.documents.size} fromCache=${snapshot.metadata.isFromCache} pendingWrites=${snapshot.metadata.hasPendingWrites()}"
                )

                val seniors = snapshot.documents.mapNotNull { doc ->
                    try {
                        val mapped = doc.toObject(Senior::class.java)
                        if (mapped == null) {
                            null
                        } else {
                            mapped.copy(id = doc.id)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }.sortedBy { it.name.lowercase() }

                if (seniors != lastEmitted) {
                    lastEmitted = seniors
                    DbgLog.d("Repo", "observeSeniors emit success count=${seniors.size}")
                    trySend(Resource.Success(seniors))
                }
            }
        awaitClose {
            DbgLog.d("Repo", "observeSeniors close")
            registration.remove()
        }
    }

    fun addSenior(senior: Senior, onResult: (Boolean, String?) -> Unit) {
        DbgLog.d("Repo", "addSenior start incomingId=${senior.id}")
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            DbgLog.d("Repo", "addSenior blocked unauthenticated")
            onResult(false, "Not authenticated")
            return
        }

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val permissions = (userDoc.get("permissions") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?: emptyList()

                val normalizedPermissions = permissions.map { PermissionManager.normalizePermission(it) }
                val canCreateSenior = normalizedPermissions.contains("*:*:*") ||
                    normalizedPermissions.contains("seniors:manage")

                if (!canCreateSenior) {
                    DbgLog.d("Repo", "addSenior blocked by role/permissions")
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
                    .addOnSuccessListener {
                        DbgLog.d("Repo", "addSenior success savedId=${docRef.id}")
                        onResult(true, null)
                    }
                    .addOnFailureListener {
                        DbgLog.e("Repo", "addSenior set failed", it)
                        onResult(false, FirestoreErrorMapper.toUserMessage(it, auth.currentUser != null))
                    }
            }
            .addOnFailureListener {
                DbgLog.e("Repo", "addSenior permissions lookup failed", it)
                onResult(false, FirestoreErrorMapper.toUserMessage(it, auth.currentUser != null))
            }
    }

    fun updateSenior(senior: Senior, onResult: (Boolean, String?) -> Unit) {
        DbgLog.d("Repo", "updateSenior start id=${senior.id}")
        if (senior.id.isBlank()) {
            onResult(false, "Invalid Senior ID")
            return
        }
        db.collection("seniors").document(senior.id)
            .set(senior)
            .addOnSuccessListener {
                DbgLog.d("Repo", "updateSenior success id=${senior.id}")
                onResult(true, null)
            }
            .addOnFailureListener {
                DbgLog.e("Repo", "updateSenior failed id=${senior.id}", it)
                onResult(false, FirestoreErrorMapper.toUserMessage(it, auth.currentUser != null))
            }
    }

    fun uploadSeniorImage(file: File, onResult: (String?) -> Unit) {
        val fileSize = file.length()
        if (fileSize > 20 * 1024 * 1024) {
            // return error "File size exceeds 20MB limit"
            onResult(null)
            return
        }

        val extension = file.extension.lowercase()
        val allowed = listOf("pdf", "jpg", "jpeg", "png")
        if (extension !in allowed) {
            // return error "Only PDF, JPG, PNG files allowed"
            onResult(null)
            return
        }

        mediaManager.upload(file.absolutePath)
            .unsigned(CloudinaryConfig.getUploadPreset())
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
        DbgLog.d("Repo", "deleteSenior start id=$seniorId")
        if (seniorId.isBlank()) {
            onResult(false, "Invalid Senior ID")
            return
        }
        db.collection("seniors").document(seniorId)
            .delete()
            .addOnSuccessListener {
                DbgLog.d("Repo", "deleteSenior success id=$seniorId")
                onResult(true, null)
            }
            .addOnFailureListener {
                DbgLog.e("Repo", "deleteSenior failed id=$seniorId", it)
                onResult(false, FirestoreErrorMapper.toUserMessage(it, auth.currentUser != null))
            }
    }
}
