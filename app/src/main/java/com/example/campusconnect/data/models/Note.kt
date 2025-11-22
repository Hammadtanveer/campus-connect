package com.example.campusconnect.data.models

import com.google.firebase.Timestamp

/**
 * Note metadata stored in Firestore
 * Actual files are stored in Cloudinary
 */
data class Note(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val semester: String = "",

    // File information
    val fileName: String = "",
    val fileSize: Long = 0L,
    val fileType: String = "", // pdf, image, document, etc.
    val fileUrl: String = "", // Cloudinary secure URL
    val cloudinaryPublicId: String = "", // For deletion

    // Upload information
    val uploaderId: String = "",
    val uploaderName: String = "",
    val uploadedAt: Timestamp? = null,

    // Engagement metrics
    val downloads: Int = 0,
    val views: Int = 0
)
