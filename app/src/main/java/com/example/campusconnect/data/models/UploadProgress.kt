package com.example.campusconnect.data.models

/**
 * Represents upload progress states
 */
sealed class UploadProgress {
    object Idle : UploadProgress()
    object Validating : UploadProgress()
    data class Uploading(val progress: Int) : UploadProgress()
    data class Success(val noteId: String) : UploadProgress()
    data class Error(val message: String) : UploadProgress()
}


