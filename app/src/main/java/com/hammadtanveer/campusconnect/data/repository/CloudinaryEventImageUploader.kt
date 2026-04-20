package com.hammadtanveer.campusconnect.data.repository

import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.util.CloudinaryConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Dedicated uploader for society event posters.
 * Isolated from repository persistence logic to keep MVVM layers clean.
 */
@Singleton
class CloudinaryEventImageUploader @Inject constructor(
    private val mediaManager: MediaManager
) {
    data class UploadResult(
        val secureUrl: String,
        val publicId: String
    )

    suspend fun uploadPoster(file: File, societyId: String): Resource<UploadResult> =
        run {
            val fileSize = file.length()
            if (fileSize > 20 * 1024 * 1024) {
                return Resource.Error("File size exceeds 20MB limit")
            }

            val extension = file.extension.lowercase()
            val allowed = listOf("pdf", "jpg", "jpeg", "png")
            if (extension !in allowed) {
                return Resource.Error("Only PDF, JPG, PNG files allowed")
            }

            val sanitizedName = file.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val publicId = "${System.currentTimeMillis()}_$sanitizedName"
            val folder = "society_events/$societyId"

            suspendCancellableCoroutine { continuation ->
                mediaManager.upload(file.absolutePath)
                .unsigned(CloudinaryConfig.getUploadPreset())
                .option("folder", folder)
                .option("resource_type", "image")
                .option("public_id", publicId)
                .option("overwrite", false)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) = Unit
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) = Unit
                    override fun onReschedule(requestId: String, error: ErrorInfo) = Unit

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        if (!continuation.isActive) return
                        val secureUrl = resultData["secure_url"] as? String
                        val uploadedPublicId = resultData["public_id"] as? String
                        if (secureUrl.isNullOrBlank() || uploadedPublicId.isNullOrBlank()) {
                            continuation.resume(Resource.Error("Image upload succeeded but URL is missing"))
                        } else {
                            continuation.resume(Resource.Success(UploadResult(secureUrl, uploadedPublicId)))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        if (!continuation.isActive) return
                        continuation.resume(Resource.Error(error.description ?: "Failed to upload event poster"))
                    }
                })
                .dispatch()
            }
        }
}

