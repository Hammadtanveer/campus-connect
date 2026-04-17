package com.example.campusconnect.security

import android.util.Log
import com.example.campusconnect.data.models.UserProfile
import com.google.firebase.Timestamp

object PermissionManager {
    private const val TAG = "PERM_DEBUG"
    private const val PM_TAG = "PERM_DEBUG_PM"
    private val managedSocietyIds = setOf(
        "csss",
        "hobbies_club",
        "tech_club",
        "sports_club",
        "cultural_society",
        "literary_society"
    )

    private fun normalize(value: String): String = value.trim().lowercase()

    private fun safeLogDebug(tag: String, message: String) {
        runCatching { Log.d(tag, message) }
    }

    fun normalizeRole(role: String?): String = normalize(role.orEmpty())

    fun normalizePermission(permission: String): String {
        val normalized = normalize(permission)

        if (normalized.startsWith("society:") && normalized.endsWith(":manage")) {
            val societyId = normalized.removePrefix("society:").removeSuffix(":manage")
            if (societyId == "*") return "society:*:manage"
            return societyManagePermission(normalizeSocietyId(societyId))
        }

        return normalized
    }

    private fun normalizeSocietyId(societyId: String): String = normalize(societyId)

    fun societyManagePermission(societyId: String): String {
        val normalizedSocietyId = normalizeSocietyId(societyId)
        return "society:$normalizedSocietyId:manage"
    }

    fun managedSocietyPermissionKeys(): List<String> {
        return managedSocietyIds.map(::societyManagePermission)
    }

    fun hasActiveAccess(profile: UserProfile?): Boolean {
        if (profile == null) return false
        if (normalize(profile.status) != "active") return false
        val expiry = profile.expiresAt
        return expiry == null || expiry >= Timestamp.now()
    }

    fun effectivePermissions(profile: UserProfile?): Set<String> {
        if (profile == null) return emptySet()
        return profile.permissions
            .map(::normalizePermission)
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun hasPermission(profile: UserProfile?, permission: String): Boolean {
        val perms = effectivePermissions(profile)
        return perms.contains("*:*:*") || perms.contains(normalizePermission(permission))
    }

    private fun logDecision(feature: String, profile: UserProfile?, allowed: Boolean) {
        val uid = profile?.id ?: "unknown"
        val perms = effectivePermissions(profile).sorted()
        safeLogDebug(TAG, "feature=$feature allowed=$allowed userId=$uid permissions=$perms")
    }

    fun isSuperAdmin(profile: UserProfile?): Boolean {
        val allowed = hasPermission(profile, "*:*:*")
        logDecision("isSuperAdmin", profile, allowed)
        return allowed
    }

    fun canCreateEvents(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "meetings:manage")
        logDecision("canCreateEvents", profile, allowed)
        return allowed
    }

    fun canUploadNotes(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "notes:manage")
        logDecision("canUploadNotes", profile, allowed)
        return allowed
    }

    fun canManagePlacements(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val perms = effectivePermissions(profile).sorted()
        safeLogDebug(PM_TAG, "Checking placements: perms=$perms")
        val allowed = hasPermission(profile, "placements:manage")
        logDecision("canManagePlacements", profile, allowed)
        return allowed
    }

    fun canManageSeniors(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "seniors:manage")
        logDecision("canManageSeniors", profile, allowed)
        return allowed
    }

    fun canManageSociety(profile: UserProfile?, societyId: String): Boolean {
        if (!hasActiveAccess(profile)) return false
        val normalizedSocietyPermission = societyManagePermission(societyId)
        val allowed = hasPermission(profile, "society:*:manage") || hasPermission(profile, normalizedSocietyPermission)
        safeLogDebug(PM_TAG, "Checking society: societyId=$societyId permission=$normalizedSocietyPermission allowed=$allowed")
        logDecision("canManageSociety:$societyId", profile, allowed)
        return allowed
    }

    fun canManageSociety(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val perms = effectivePermissions(profile)
        val allowed = perms.contains("*:*:*") ||
            perms.contains("society:*:manage") ||
            perms.any { it.startsWith("society:") && it.endsWith(":manage") }
        logDecision("canManageSociety", profile, allowed)
        return allowed
    }

    fun canDeleteSocietyEvent(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "society:*:manage")
        logDecision("canDeleteSocietyEvent", profile, allowed)
        return allowed
    }

    fun canManageUsers(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "admin:access")
        logDecision("canManageUsers", profile, allowed)
        return allowed
    }

    fun canViewAnalytics(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "admin:access")
        logDecision("canViewAnalytics", profile, allowed)
        return allowed
    }

    fun canSendNotifications(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "admin:access")
        logDecision("canSendNotifications", profile, allowed)
        return allowed
    }

    fun canViewActivityLog(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "admin:access")
        logDecision("canViewActivityLog", profile, allowed)
        return allowed
    }

    fun canModerateContent(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "notes:manage")
        logDecision("canModerateContent", profile, allowed)
        return allowed
    }

    fun canAccessAdminPanel(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed =
            canManageSociety(profile) ||
            canManagePlacements(profile) ||
            canManageSeniors(profile) ||
            canUploadNotes(profile) ||
            canCreateEvents(profile) ||
            canManageUsers(profile) ||
            canViewAnalytics(profile) ||
            canSendNotifications(profile) ||
            canViewActivityLog(profile) ||
            hasPermission(profile, "admin:access")
        logDecision("canAccessAdminPanel", profile, allowed)
        return allowed
    }

    fun logProfileSnapshot(userId: String, role: String?, permissions: Collection<String>) {
        val normalizedRole = normalizeRole(role)
        val normalizedPermissions = permissions.map(::normalizePermission).distinct().sorted()
        safeLogDebug(TAG, "userId=$userId, role=$normalizedRole, permissions=$normalizedPermissions")
    }

    fun logProfileSnapshot(profile: UserProfile?, source: String) {
        if (profile == null) {
            safeLogDebug(TAG, "source=$source, profile=null")
            return
        }
        logProfileSnapshot(
            userId = profile.id,
            role = profile.role,
            permissions = effectivePermissions(profile)
        )
    }
}





