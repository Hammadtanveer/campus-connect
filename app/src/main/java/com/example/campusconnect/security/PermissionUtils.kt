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

private fun UserProfile.hasAnyPermission(vararg keys: String): Boolean {
    if (permissions.contains("*:*:*")) return true
    return keys.any { key -> permissions.contains(key) || roles.contains(key) }
}

private fun UserProfile.hasAdminRole(): Boolean {
    val normalizedRole = role.trim().lowercase()
    return isAdmin || normalizedRole in listOf("admin", "super_admin", "superadmin")
}

fun UserProfile.canCreateEvent(): Boolean = hasAdminRole() || hasAnyPermission(
    Permissions.EVENT_CREATE,
    "events:create:own",
    "events:create:all"
)

fun UserProfile.canUploadNotes(): Boolean = hasAdminRole() || hasAnyPermission(
    Permissions.NOTES_UPLOAD,
    "notes:upload:own",
    "notes:upload:all"
)

fun UserProfile.canAddSenior(): Boolean {
    val normalizedRole = role.trim().lowercase()
    if (normalizedRole == "admin" || normalizedRole == "super_admin" || normalizedRole == "superadmin") {
        return true
    }
    return permissions.contains("*:*:*") ||
        permissions.contains("seniors:add:all") ||
        roles.contains("seniors:add:all")
}

fun UserProfile.canUpdateSenior(): Boolean = hasAdminRole() || hasAnyPermission(
    Permissions.SENIOR_UPDATE,
    "seniors:edit:all",
    "seniors:verify:all"
)

fun UserProfile.canManageSociety(): Boolean = hasAdminRole() || hasAnyPermission(
    Permissions.SOCIETY_MANAGE,
    "society:manage"
)
