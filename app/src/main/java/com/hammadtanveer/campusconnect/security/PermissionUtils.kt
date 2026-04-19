package com.hammadtanveer.campusconnect.security

import com.hammadtanveer.campusconnect.data.models.UserProfile

/**
 * Centralized permission keys and helpers for RBAC.
 * These strings should match Firestore Rules document-permission checks.
 */
object Permissions {
    const val ADMIN_ACCESS = "admin:access"
    const val MEETINGS_MANAGE = "meetings:manage"
    const val NOTES_MANAGE = "notes:manage"
    const val PLACEMENTS_MANAGE = "placements:manage"
    const val SENIORS_MANAGE = "seniors:manage"
    const val SOCIETY_MANAGE_ALL = "society:*:manage"

    val DEFAULT_ADMIN_PERMISSIONS = listOf(
        MEETINGS_MANAGE,
        NOTES_MANAGE,
        PLACEMENTS_MANAGE,
        SOCIETY_MANAGE_ALL
    )

    fun societyManage(societyId: String): String = PermissionManager.societyManagePermission(societyId)
}

fun UserProfile.canCreateEvent(): Boolean = PermissionManager.canCreateEvents(this)

fun UserProfile.canUploadNotes(): Boolean = PermissionManager.canUploadNotes(this)

fun UserProfile.canAddSenior(): Boolean {
    return PermissionManager.canManageSeniors(this)
}

fun UserProfile.canUpdateSenior(): Boolean = PermissionManager.canManageSeniors(this)

fun UserProfile.canManageSociety(): Boolean = PermissionManager.canManageSociety(this)

fun UserProfile.canManageSociety(societyId: String): Boolean =
    PermissionManager.canManageSociety(this, societyId)

