package com.example.campusconnect.util

import com.example.campusconnect.data.models.Permission
import com.example.campusconnect.data.models.UserProfile

/**
 * Centralized utility for checking user permissions in the RBAC system.
 *
 * This is the single source of truth for permission validation in the app.
 * All permission checks should go through this class.
 *
 * Features:
 * - Super admin has all permissions automatically
 * - Wildcard permission support (events:*:* grants all event permissions)
 * - Scope checking (own vs department vs all)
 * - Null-safe with defensive programming
 *
 * Usage:
 * ```kotlin
 * if (PermissionChecker.hasPermission(currentUser, Permissions.EVENTS_CREATE)) {
 *     // Show create event button
 * }
 * ```
 */
object PermissionChecker {

    /**
     * Checks if a user has a specific permission.
     *
     * Logic:
     * 1. Super admin has all permissions
     * 2. Check exact permission match
     * 3. Check wildcard permissions (events:*:* grants events:create:own)
     * 4. Check resource-level wildcards (events:*:* grants any event permission)
     *
     * @param user The user to check permissions for
     * @param permission The permission string to check (e.g., "events:create:own")
     * @return true if user has the permission, false otherwise
     */
    fun hasPermission(user: UserProfile?, permission: String): Boolean {
        // Null safety: no user = no permission
        if (user == null) return false

        // Super admin has all permissions
        if (user.role == "super_admin") return true

        // User must be active to have permissions
        if (user.status != "active") return false

        // Parse the required permission
        val requiredPerm = Permission.fromString(permission)

        // Check each permission the user has
        for (userPermString in user.permissions) {
            val userPerm = Permission.fromString(userPermString)

            // Exact match
            if (userPerm.toString() == requiredPerm.toString()) return true

            // Wildcard match
            if (userPerm.matches(requiredPerm)) return true
        }

        // Check legacy roles field for backward compatibility
        if (user.roles.contains(permission)) return true

        // Check if user has wildcard for this resource
        val resourceWildcard = "${requiredPerm.resource}:*:*"
        if (user.permissions.contains(resourceWildcard) || user.roles.contains(resourceWildcard)) {
            return true
        }

        return false
    }

    /**
     * Checks if user has ANY of the specified permissions.
     * Useful for showing UI elements that require at least one permission.
     *
     * @param user The user to check
     * @param permissions List of permission strings
     * @return true if user has at least one permission
     */
    fun hasAnyPermission(user: UserProfile?, permissions: List<String>): Boolean {
        if (user == null || permissions.isEmpty()) return false
        return permissions.any { hasPermission(user, it) }
    }

    /**
     * Checks if user has ALL of the specified permissions.
     * Useful for actions that require multiple permissions.
     *
     * @param user The user to check
     * @param permissions List of permission strings
     * @return true if user has all permissions
     */
    fun hasAllPermissions(user: UserProfile?, permissions: List<String>): Boolean {
        if (user == null || permissions.isEmpty()) return false
        return permissions.all { hasPermission(user, it) }
    }

    /**
     * Checks if user can perform an action on a resource with scope validation.
     *
     * Scope logic:
     * - "own": Can only act on own resources (targetUserId must match user.id)
     * - "department": Can act on resources in same department
     * - "all": Can act on any resource
     *
     * @param user The user attempting the action
     * @param action The permission action (e.g., "events:delete")
     * @param targetUserId The owner of the resource being acted upon (null for create actions)
     * @param targetDepartment The department of the resource (null if not department-scoped)
     * @return true if user can perform the action
     */
    fun canPerformAction(
        user: UserProfile?,
        action: String,
        targetUserId: String? = null,
        targetDepartment: String? = null
    ): Boolean {
        if (user == null) return false

        // Super admin can do anything
        if (user.role == "super_admin") return true

        // Check "all" scope first (least restrictive)
        if (hasPermission(user, "$action:all")) return true

        // Check "department" scope
        if (targetDepartment != null &&
            user.department == targetDepartment &&
            hasPermission(user, "$action:department")) {
            return true
        }

        // Check "own" scope (most restrictive)
        if (targetUserId != null &&
            user.id == targetUserId &&
            hasPermission(user, "$action:own")) {
            return true
        }

        return false
    }

    /**
     * Checks if user is a super admin.
     * Super admins have unrestricted access to all features.
     *
     * @param user The user to check
     * @return true if user is super admin
     */
    fun isSuperAdmin(user: UserProfile?): Boolean {
        return user?.role == "super_admin"
    }

    /**
     * Checks if user is any type of admin (super admin or regular admin).
     *
     * @param user The user to check
     * @return true if user has admin role
     */
    fun isAdmin(user: UserProfile?): Boolean {
        return user?.role in listOf("super_admin", "admin") || user?.isAdmin == true
    }

    /**
     * Checks if user's admin access is currently valid.
     * Considers status and expiry date.
     *
     * @param user The user to check
     * @return true if admin access is active and not expired
     */
    fun isAdminAccessValid(user: UserProfile?): Boolean {
        if (user == null || !isAdmin(user)) return false

        // Check status
        if (user.status != "active") return false

        // Check expiry (if set)
        val expiresAt = user.expiresAt
        if (expiresAt != null) {
            val now = com.google.firebase.Timestamp.now()
            if (expiresAt < now) return false
        }

        return true
    }

    /**
     * Gets all permissions a user has, including those from wildcards.
     * Useful for displaying permission lists in UI.
     *
     * @param user The user to get permissions for
     * @return Set of all effective permissions
     */
    fun getEffectivePermissions(user: UserProfile?): Set<String> {
        if (user == null) return emptySet()

        // Super admin has all permissions
        if (user.role == "super_admin") {
            return setOf("*:*:*")
        }

        val effectivePerms = mutableSetOf<String>()

        // Add explicit permissions
        effectivePerms.addAll(user.permissions)

        // Add legacy roles
        effectivePerms.addAll(user.roles)

        return effectivePerms
    }

    /**
     * Validates if a permission string is properly formatted.
     *
     * @param permission The permission string to validate
     * @return true if permission has valid format (resource:action:scope)
     */
    fun isValidPermission(permission: String): Boolean {
        val parts = permission.split(":")
        return parts.size == 3 && parts.all { it.isNotBlank() }
    }

    /**
     * Checks if user can manage other admins.
     * Only super admins can create/edit/delete other admins.
     *
     * @param user The user to check
     * @return true if user can manage admins
     */
    fun canManageAdmins(user: UserProfile?): Boolean {
        return isSuperAdmin(user)
    }

    /**
     * Checks if user can modify a specific admin.
     *
     * Rules:
     * - Super admin can modify any admin (except other super admins)
     * - Regular admins cannot modify anyone
     * - Cannot modify yourself through this check
     *
     * @param actor The user attempting the modification
     * @param target The admin being modified
     * @return true if modification is allowed
     */
    fun canModifyAdmin(actor: UserProfile?, target: UserProfile?): Boolean {
        if (actor == null || target == null) return false

        // Cannot modify yourself
        if (actor.id == target.id) return false

        // Only super admin can modify admins
        if (!isSuperAdmin(actor)) return false

        // Cannot modify other super admins
        if (isSuperAdmin(target)) return false

        return true
    }

    /**
     * Gets a human-readable reason why permission was denied.
     * Useful for showing meaningful error messages to users.
     *
     * @param user The user whose permission was checked
     * @param permission The permission that was denied
     * @return Human-readable explanation
     */
    fun getPermissionDeniedReason(user: UserProfile?, permission: String): String {
        if (user == null) {
            return "You must be signed in to perform this action"
        }

        if (user.status == "suspended") {
            return "Your account has been suspended. Contact an administrator."
        }

        if (user.status == "expired" || user.status == "revoked") {
            return "Your admin access has ${user.status}. Contact an administrator."
        }

        val expiresAt = user.expiresAt
        if (expiresAt != null && expiresAt < com.google.firebase.Timestamp.now()) {
            return "Your admin access has expired. Contact an administrator to renew."
        }

        if (!isAdmin(user)) {
            return "You don't have admin privileges for this action"
        }

        return "You don't have the required permission: $permission"
    }
}

