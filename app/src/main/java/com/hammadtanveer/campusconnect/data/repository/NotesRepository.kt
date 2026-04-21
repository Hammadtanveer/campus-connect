package com.hammadtanveer.campusconnect.data.repository

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.hammadtanveer.campusconnect.data.local.NotesDao
import com.hammadtanveer.campusconnect.data.local.toEntity
import com.hammadtanveer.campusconnect.data.models.Note
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.util.Constants
import com.hammadtanveer.campusconnect.util.CloudinaryConfig
import com.hammadtanveer.campusconnect.util.DbgLog
import com.hammadtanveer.campusconnect.util.FileUtils
import com.hammadtanveer.campusconnect.util.FirestoreErrorMapper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Repository for managing note operations.
 *
 * Uses dependency injection for Firebase and Cloudinary instances.
 * All methods return Resource wrapper for consistent error handling.
 */
@Singleton
class NotesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val mediaManager: MediaManager,
    private val notesDao: NotesDao,
    private val adminActivityLogRepository: AdminActivityLogRepository,
    private val auth: FirebaseAuth
) {

    private val notesCollection = firestore.collection("notes")

    // Simple helper to extract firebase console index URL from an error message
    private fun extractFirestoreIndexUrl(message: String?): String? {
        if (message.isNullOrBlank()) return null
        val regex = "(https://console.firebase.google.com[^\\s]+)".toRegex()
        return regex.find(message)?.groups?.get(1)?.value
    }

    /**
     * Upload note file to Cloudinary and save metadata to Firestore
     */
    suspend fun uploadNote(
        title: String,
        description: String,
        subject: String,
        semester: String,
        file: File,
        userId: String,
        userName: String,
        onProgress: (Int) -> Unit = {}
    ): Resource<String> {

        try {
            val fileSize = file.length()
            if (fileSize > 20 * 1024 * 1024) {
                return Resource.Error("File size exceeds 20MB limit")
            }

            val extension = file.extension.lowercase()
            val allowed = listOf("pdf", "jpg", "jpeg", "png")
            if (extension !in allowed) {
                return Resource.Error("Only PDF, JPG, PNG files allowed")
            }

            val resourceType = if (extension == "pdf") "raw" else "image"

            val fileType = FileUtils.getFileType(file.name)
            val sanitizedSubject = subject
                .replace(" ", "_")
                .replace("(", "")
                .replace(")", "")
                .replace("[", "")
                .replace("]", "")

            val folder = "${Constants.CLOUDINARY_BASE_FOLDER}/${semester.replace(" ", "_")}/$sanitizedSubject"

            val uploadOptions = mapOf(
                "folder" to folder,
                "resource_type" to resourceType,
                "allowed_formats" to "pdf,jpg,jpeg,png"
            )

            return suspendCoroutine { continuation ->
                mediaManager
                    .upload(file.absolutePath)
                    .unsigned(CloudinaryConfig.getUploadPreset())
                    .options(uploadOptions)
                    .callback(object : UploadCallback {

                    override fun onStart(requestId: String) {
                        onProgress(0)
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = ((bytes * 100) / totalBytes).toInt()
                        onProgress(progress)
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val uploadedPublicId = resultData["public_id"] as? String ?: ""
                        val secureUrl = resultData["secure_url"] as? String ?: ""
                        
                        // Save metadata to Firestore
                        val noteMetadata = hashMapOf(
                            "title" to title,
                            "description" to description,
                            "subject" to subject,
                            "semester" to semester,
                            "fileName" to file.name,
                            "fileSize" to file.length(),
                            "fileType" to fileType,
                            "fileUrl" to secureUrl,
                            "cloudinaryPublicId" to uploadedPublicId,
                            "uploaderId" to userId,
                            "uploaderName" to userName,
                            "uploadedAt" to Timestamp.now(),
                            "downloads" to 0,
                            "views" to 0,
                            "moderationStatus" to "pending"
                        )

                        notesCollection.add(noteMetadata)
                            .addOnSuccessListener { documentReference ->
                                adminActivityLogRepository.logActionAsync(
                                    action = "Note uploaded: $title",
                                    userName = userName,
                                    type = "note_uploaded",
                                    userId = userId
                                )
                                continuation.resume(Resource.Success(documentReference.id))
                            }
                            .addOnFailureListener { e ->
                                continuation.resume(Resource.Error(e.message ?: "Failed to save metadata"))
                            }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(Resource.Error(error.description ?: "Upload failed"))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resume(Resource.Error("Upload rescheduled: ${error.description}"))
                    }
                })
                .dispatch()
            }
        } catch (e: Exception) {
            return Resource.Error(e.message ?: "Upload failed")
        }
    }

    /**
     * Observe all notes with real-time updates
     */
    fun observeNotes(
        subject: String? = null,
        semester: String? = null,
        uploaderId: String? = null,
        searchQuery: String? = null
    ): Flow<Resource<List<Note>>> = callbackFlow {
        trySend(Resource.Loading)

        if (auth.currentUser == null) {
            trySend(Resource.Error("Please sign in to view notes."))
            close()
            return@callbackFlow
        }

        var query: Query = notesCollection

        // Apply filters
        subject?.let { s ->
            query = query.whereEqualTo("subject", s)
        }

        semester?.let { sem ->
            query = query.whereEqualTo("semester", sem)
        }

        uploaderId?.let { uid ->
            query = query.whereEqualTo("uploaderId", uid)
        }

        // Order by upload date (newest first)
        query = query.orderBy("uploadedAt", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                val raw = FirestoreErrorMapper.toUserMessage(error, auth.currentUser != null)
                val idx = extractFirestoreIndexUrl(raw)
                val message = if (!idx.isNullOrBlank()) "$raw\n\nCreate required index here:\n$idx" else raw
                trySend(Resource.Error(message))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val notes = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Note::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                }

                // Apply search filter if provided
                val filteredNotes = if (!searchQuery.isNullOrBlank()) {
                    notes.filter { note ->
                        note.title.contains(searchQuery, ignoreCase = true) ||
                        note.description.contains(searchQuery, ignoreCase = true) ||
                        note.subject.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    notes
                }

                trySend(Resource.Success(filteredNotes))
            }
        }

        awaitClose { registration.remove() }
    }

    /**
     * Get my uploaded notes
     */
    fun observeMyNotes(uploaderId: String): Flow<Resource<List<Note>>> {
        return observeNotes(uploaderId = uploaderId)
    }

    fun observeNotesForModeration(): Flow<Resource<List<Note>>> = callbackFlow {
        trySend(Resource.Loading)

        if (auth.currentUser == null) {
            trySend(Resource.Error("Please sign in to continue."))
            close()
            return@callbackFlow
        }

        val registration = notesCollection
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(FirestoreErrorMapper.toUserMessage(error, auth.currentUser != null)))
                    return@addSnapshotListener
                }

                val notes = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Note::class.java)?.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(Resource.Success(notes))
            }

        awaitClose { registration.remove() }
    }

    suspend fun updateModerationStatus(
        noteId: String,
        status: String,
        reviewerId: String,
        reviewerName: String
    ): Resource<Unit> {
        if (noteId.isBlank()) return Resource.Error("Invalid note id")
        if (status != "approved" && status != "rejected") {
            return Resource.Error("Invalid moderation status")
        }

        return try {
            notesCollection.document(noteId)
                .update(
                    mapOf(
                        "moderationStatus" to status,
                        "moderatedAt" to Timestamp.now(),
                        "moderatedBy" to reviewerId,
                        "moderatedByName" to reviewerName
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null))
        }
    }

    /**
     * Delete note (from both Cloudinary and Firestore)
     */
    suspend fun deleteNote(noteId: String, cloudinaryPublicId: String): Resource<Unit> {
        return try {
            // Delete from Cloudinary
            try {
                MediaManager.get()
                    .cloudinary
                    .uploader()
                    .destroy(cloudinaryPublicId, mapOf("resource_type" to "auto"))
            } catch (_: Exception) {
                // Continue even if Cloudinary deletion fails
            }

            // Delete metadata from Firestore
            notesCollection.document(noteId).delete().await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null))
        }
    }

    /**
     * Increment download count
     */
    suspend fun incrementDownloads(noteId: String) {
        try {
            notesCollection.document(noteId)
                .update("downloads", FieldValue.increment(1))
                .await()
        } catch (_: Exception) {
            // Silently fail - not critical
        }
    }

    /**
     * Increment view count
     */
    suspend fun incrementViews(noteId: String) {
        try {
            notesCollection.document(noteId)
                .update("views", FieldValue.increment(1))
                .await()
        } catch (_: Exception) {
            // Silently fail - not critical
        }
    }

    /**
     * Get single note by ID
     */
    suspend fun getNoteById(noteId: String): Resource<Note> {
        return try {
            val document = notesCollection.document(noteId).get().await()
            val note = document.toObject(Note::class.java)
                ?.copy(id = document.id)

            if (note != null) {
                Resource.Success(note)
            } else {
                Resource.Error("Note not found")
            }
        } catch (e: Exception) {
            Resource.Error(FirestoreErrorMapper.toUserMessage(e, auth.currentUser != null))
        }
    }

    /**
     * Backward-compatible shim for legacy callers expecting to upload raw PDF bytes
     */
    suspend fun uploadCompressedPdf(
        uploaderId: String,
        title: String,
        pdfBytes: ByteArray,
        @Suppress("UNUSED_PARAMETER") compressedBytes: ByteArray
    ): Resource<Unit> {
        return try {
            // Write original PDF bytes to a temporary file
            val temp = File.createTempFile("note_", ".pdf")
            FileOutputStream(temp).use { it.write(pdfBytes) }

            val user = FirebaseAuth.getInstance().currentUser
            val userName = user?.displayName ?: user?.email ?: uploaderId

            // Delegate to the new Cloudinary-based uploader (PDF-only enforced elsewhere)
            val result = uploadNote(
                title = title,
                description = "",
                subject = "General",
                semester = "Semester 1",
                file = temp,
                userId = uploaderId,
                userName = userName,
                onProgress = {}
            )

            temp.delete()

            when (result) {
                is Resource.Success -> Resource.Success(Unit)
                is Resource.Error -> Resource.Error("Upload failed: ${result.message ?: "Unknown error"}")
                is Resource.Loading -> Resource.Error("Unexpected state: Loading")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to upload PDF")
        }
    }

    /**
     * Sync notes from Firestore to local Room database
     */
    suspend fun syncNotes(subject: String? = null, semester: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                DbgLog.d("Repo", "syncNotes start subject=$subject semester=$semester")
                val beforeCount = notesDao.countNotesLogged()
                DbgLog.d("Repo", "syncNotes local count before=$beforeCount")
                // Fetch latest remote notes (one-shot)
                var query: Query = notesCollection.orderBy("uploadedAt", Query.Direction.DESCENDING)
                subject?.let { query = query.whereEqualTo("subject", it) }
                semester?.let { query = query.whereEqualTo("semester", it) }
                val snap = query.get().await()
                DbgLog.d("Repo", "syncNotes remote docs=${snap.documents.size}")
                val entities = snap.documents.mapNotNull { doc ->
                    doc.toObject(Note::class.java)?.copy(id = doc.id)?.toEntity()
                }
                notesDao.insertNotesLogged(entities)
                val afterCount = notesDao.countNotesLogged()
                DbgLog.d("Repo", "syncNotes local count after=$afterCount")
            } catch (error: Exception) {
                DbgLog.e("Repo", "syncNotes failed", error)
            }
        }
    }
}
