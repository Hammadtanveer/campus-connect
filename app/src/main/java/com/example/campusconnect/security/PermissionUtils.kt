package com.example.campusconnect.security

import com.example.campusconnect.data.models.UserProfile

/**
 * Centralized permission keys and helpers for RBAC.
 * These strings should be mirrored in backend custom claims or Firestore allow lists.
 */
object Permissions {
    const val EVENT_CREATE = "event:create"
    const val NOTES_UPLOAD = "notes:upload"
    const val SENIOR_UPDATE = "senior:update"
    const val SOCIETY_MANAGE = "society:manage"
}

fun UserProfile.canCreateEvent(): Boolean = isAdmin || roles.contains(Permissions.EVENT_CREATE)
fun UserProfile.canUploadNotes(): Boolean = isAdmin || roles.contains(Permissions.NOTES_UPLOAD)
fun UserProfile.canUpdateSenior(): Boolean = isAdmin || roles.contains(Permissions.SENIOR_UPDATE)
fun UserProfile.canManageSociety(): Boolean = isAdmin || roles.contains(Permissions.SOCIETY_MANAGE)

