package com.example.campusconnect.security

import com.example.campusconnect.data.models.UserProfile

/**
 * Centralized permission keys and helpers for RBAC.
 * These strings should be mirrored in backend custom claims or Firestore allow lists.
 */
object Permissions {
    const val EVENT_CREATE = "event:create"
    const val NOTES_UPLOAD = "notes:upload"
    const val SENIOR_ADD = "senior:add"
    const val SENIOR_UPDATE = "senior:update"
    const val SOCIETY_MANAGE = "society:manage"
}

fun UserProfile.canCreateEvent(): Boolean = PermissionManager.canCreateEvents(this)

fun UserProfile.canUploadNotes(): Boolean = PermissionManager.canUploadNotes(this)

fun UserProfile.canAddSenior(): Boolean {
    return PermissionManager.canManageSeniors(this)
}

fun UserProfile.canUpdateSenior(): Boolean = PermissionManager.canManageSeniors(this)

fun UserProfile.canManageSociety(): Boolean = PermissionManager.canManageSociety(this)
