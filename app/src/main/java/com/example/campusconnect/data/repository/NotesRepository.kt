package com.example.campusconnect.data.repository

import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.campusconnect.data.models.Note
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.util.Constants
import com.example.campusconnect.util.FileUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NotesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val notesCollection = firestore.collection("notes")

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
    ): Resource<String> = suspendCoroutine { continuation ->

        try {
            val fileType = FileUtils.getFileType(file.name)

            // Configure upload options for Cloudinary - PDF ONLY
            val folder = "${Constants.CLOUDINARY_BASE_FOLDER}/${semester.replace(" ", "_")}/${subject.replace(" ", "_")}"
            val uploadOptions = mapOf(
                "folder" to folder,
                "resource_type" to "auto", // Auto-detect file type
                "allowed_formats" to "pdf", // PDF files only
                "public_id" to "${System.currentTimeMillis()}_${file.nameWithoutExtension}",
                "use_filename" to true,
                "unique_filename" to true,
                "overwrite" to false
            )

            Log.d("NotesRepository", "Starting upload to Cloudinary: ${file.name}")

            // Upload to Cloudinary
            MediaManager.get()
                .upload(file.absolutePath)
                .options(uploadOptions)
                .callback(object : UploadCallback {

                    override fun onStart(requestId: String) {
                        Log.d("NotesRepository", "Upload started: $requestId")
                        onProgress(0)
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = ((bytes * 100) / totalBytes).toInt()
                        Log.d("NotesRepository", "Upload progress: $progress%")
                        onProgress(progress)
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        Log.d("NotesRepository", "Upload successful: $resultData")

                        val fileUrl = resultData["secure_url"] as? String ?: ""
                        val publicId = resultData["public_id"] as? String ?: ""

                        // Save metadata to Firestore
                        val noteMetadata = hashMapOf(
                            "title" to title,
                            "description" to description,
                            "subject" to subject,
                            "semester" to semester,
                            "fileName" to file.name,
                            "fileSize" to file.length(),
                            "fileType" to fileType,
                            "fileUrl" to fileUrl,
                            "cloudinaryPublicId" to publicId,
                            "uploaderId" to userId,
                            "uploaderName" to userName,
                            "uploadedAt" to Timestamp.now(),
                            "downloads" to 0,
                            "views" to 0
                        )

                        notesCollection.add(noteMetadata)
                            .addOnSuccessListener { documentReference ->
                                Log.d("NotesRepository", "Metadata saved: ${documentReference.id}")
                                continuation.resume(Resource.Success(documentReference.id))
                            }
                            .addOnFailureListener { e ->
                                Log.e("NotesRepository", "Failed to save metadata", e)
                                continuation.resume(Resource.Error(e.message ?: "Failed to save metadata"))
                            }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("NotesRepository", "Upload error: ${error.description}")
                        continuation.resume(Resource.Error(error.description ?: "Upload failed"))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w("NotesRepository", "Upload rescheduled: ${error.description}")
                        continuation.resume(Resource.Error("Upload rescheduled: ${error.description}"))
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            Log.e("NotesRepository", "Exception during upload", e)
            continuation.resume(Resource.Error(e.message ?: "Upload failed"))
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
                Log.e("NotesRepository", "Error observing notes", error)
                trySend(Resource.Error(error.message ?: "Failed to load notes"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val notes = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Note::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("NotesRepository", "Error parsing note", e)
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
                Log.d("NotesRepository", "Deleted from Cloudinary: $cloudinaryPublicId")
            } catch (e: Exception) {
                Log.e("NotesRepository", "Failed to delete from Cloudinary", e)
                // Continue even if Cloudinary deletion fails
            }

            // Delete metadata from Firestore
            notesCollection.document(noteId).delete().await()
            Log.d("NotesRepository", "Deleted from Firestore: $noteId")

            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Failed to delete note", e)
            Resource.Error(e.message ?: "Failed to delete note")
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
            Log.d("NotesRepository", "Download count incremented for: $noteId")
        } catch (e: Exception) {
            Log.e("NotesRepository", "Failed to increment downloads", e)
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
        } catch (e: Exception) {
            Log.e("NotesRepository", "Failed to increment views", e)
            // Silently fail - not critical
        }
    }

    /**
     * Get single note by ID
     */
    suspend fun getNoteById(noteId: String): Resource<Note> {
        return try {
            val document = notesCollection.document(noteId).get().await()
            val note = document.toObject(Note::class.java)?.copy(id = document.id)

            if (note != null) {
                Resource.Success(note)
            } else {
                Resource.Error("Note not found")
            }
        } catch (e: Exception) {
            Log.e("NotesRepository", "Failed to get note", e)
            Resource.Error(e.message ?: "Failed to load note")
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
}
