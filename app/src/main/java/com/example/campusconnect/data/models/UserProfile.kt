package com.example.campusconnect.data.models

data class UserProfile(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val course: String = "",
    val branch: String = "",
    val year: String = "",
    val bio: String = "",
    val profilePictureUrl: String = "",
    val eventCount: Int = 0,


    // New data enhancements

    // Enhanced RBAC fields for hierarchical admin system
    val role: String = "user", // "super_admin" | "admin" | "user"
    val permissions: List<String> = emptyList(), // single source of truth from Firestore users/{uid}.permissions
    val department: String? = null, // department/organization (e.g., "Computer Science", "IEEE")
    val status: String = "active", // "active" | "suspended" | "expired" | "revoked"

    // Admin metadata (audit trail)
    val createdBy: String? = null, // UID of super admin who created this admin
    val expiresAt: com.google.firebase.Timestamp? = null, // optional expiry date for temporary admin access
    val lastModifiedBy: String? = null, // UID of super admin who last modified permissions
    val lastModifiedAt: com.google.firebase.Timestamp? = null, // timestamp of last permission change
    val roleTemplate: String? = null, // original role template used (e.g., "society_admin")

    // Suspension/Revocation metadata
    val suspendedBy: String? = null, // UID of super admin who suspended this admin
    val suspendedAt: com.google.firebase.Timestamp? = null, // timestamp of suspension
    val suspendedUntil: com.google.firebase.Timestamp? = null, // when suspension expires
    val suspensionReason: String? = null, // reason for suspension
    val revokedBy: String? = null, // UID of super admin who revoked admin access
    val revokedAt: com.google.firebase.Timestamp? = null, // timestamp of revocation
    val revocationReason: String? = null // reason for revocation
)
