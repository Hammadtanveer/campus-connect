package com.example.campusconnect.security

import android.util.Log
import com.example.campusconnect.data.models.UserProfile
import com.google.firebase.Timestamp

object PermissionManager {
    private const val TAG = "PERM_DEBUG"
    private const val PM_TAG = "PERM_DEBUG_PM"

    private fun normalize(value: String): String = value.trim().lowercase()

    fun normalizeRole(role: String?): String = normalize(role.orEmpty())

    fun normalizePermission(permission: String): String {
        return when (val normalized = normalize(permission)) {
            "manage_society" -> "society:manage"
            "can_manage_society" -> "society:manage"
            "manage_senior", "manage_seniors", "senior:manage" -> "seniors:add:all"
            "manage_placement",
            "manage_placements",
            "can_manage_placements",
            "placements:add:all",
            "placements:edit:all",
            "placements:delete:all",
            "placements:manage",
            "placement:manage" -> "placements:manage"
            else -> normalized
        }
    }

    fun hasActiveAccess(profile: UserProfile?): Boolean {
        if (profile == null) return false
        if (normalize(profile.status) != "active") return false
        val expiry = profile.expiresAt
        return expiry == null || expiry >= Timestamp.now()
    }

    fun effectivePermissions(profile: UserProfile?): Set<String> {
        if (profile == null) return emptySet()
        // Strict mode: ONLY permissions is authoritative.
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
        Log.d(TAG, "feature=$feature allowed=$allowed userId=$uid permissions=$perms")
    }

    fun isSuperAdmin(profile: UserProfile?): Boolean {
        val allowed = hasPermission(profile, "*:*:*") || hasPermission(profile, "admin:super")
        logDecision("isSuperAdmin", profile, allowed)
        return allowed
    }

    fun canCreateEvents(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "event:create") ||
            hasPermission(profile, "events:create:own") ||
            hasPermission(profile, "events:create:all") ||
            hasPermission(profile, "can_manage_events")
        logDecision("canCreateEvents", profile, allowed)
        return allowed
    }

    fun canUploadNotes(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "notes:upload") ||
            hasPermission(profile, "notes:upload:own") ||
            hasPermission(profile, "notes:upload:all") ||
            hasPermission(profile, "can_manage_notes")
        logDecision("canUploadNotes", profile, allowed)
        return allowed
    }

    fun canManagePlacements(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val perms = effectivePermissions(profile).sorted()
        Log.d(PM_TAG, "Checking placements: perms=$perms")
        val allowed = hasPermission(profile, "placements:manage")
        logDecision("canManagePlacements", profile, allowed)
        return allowed
    }

    fun canManageSeniors(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "seniors:add:all") ||
            hasPermission(profile, "seniors:edit:all") ||
            hasPermission(profile, "seniors:verify:all") ||
            hasPermission(profile, "seniors:delete:all")
        logDecision("canManageSeniors", profile, allowed)
        return allowed
    }

    fun canManageSociety(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "society:manage") ||
            hasPermission(profile, "can_manage_society")
        logDecision("canManageSociety", profile, allowed)
        return allowed
    }

    fun canDeleteSocietyEvent(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "society:delete:all") || hasPermission(profile, "society:event:delete:all")
        logDecision("canDeleteSocietyEvent", profile, allowed)
        return allowed
    }

    fun canManageUsers(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "admin:users:manage")
        logDecision("canManageUsers", profile, allowed)
        return allowed
    }

    fun canViewAnalytics(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "admin:analytics:view")
        logDecision("canViewAnalytics", profile, allowed)
        return allowed
    }

    fun canSendNotifications(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "notifications:send:all")
        logDecision("canSendNotifications", profile, allowed)
        return allowed
    }

    fun canViewActivityLog(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "admin:activity_log:view")
        logDecision("canViewActivityLog", profile, allowed)
        return allowed
    }

    fun canModerateContent(profile: UserProfile?): Boolean {
        if (!hasActiveAccess(profile)) return false
        val allowed = hasPermission(profile, "notes:moderate:all") || canUploadNotes(profile)
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
            hasPermission(profile, "admin:panel:view")
        logDecision("canAccessAdminPanel", profile, allowed)
        return allowed
    }

    fun logProfileSnapshot(userId: String, role: String?, permissions: Collection<String>) {
        val normalizedRole = normalizeRole(role)
        val normalizedPermissions = permissions.map(::normalizePermission).distinct().sorted()
        Log.d(TAG, "userId=$userId, role=$normalizedRole, permissions=$normalizedPermissions")
    }

    fun logProfileSnapshot(profile: UserProfile?, source: String) {
        if (profile == null) {
            Log.d(TAG, "source=$source, profile=null")
            return
        }
        logProfileSnapshot(
            userId = profile.id,
            role = profile.role,
            permissions = effectivePermissions(profile)
        )
    }
}





