package com.example.campusconnect.data.models

/**
 * Represents a granular permission in the RBAC (Role-Based Access Control) system.
 *
 * Permissions follow the format: resource:action:scope
 *
 * Examples:
 * - "events:create:own" - User can create their own events
 * - "notes:delete:all" - User can delete any notes
 * - "placements:edit:department" - User can edit placements within their department
 *
 * @property resource The resource being accessed (events, notes, seniors, placements, etc.)
 * @property action The action being performed (create, edit, delete, moderate, etc.)
 * @property scope The scope of access (own, department, all)
 */
data class Permission(
    val resource: String,
    val action: String,
    val scope: String
) {
    /**
     * Converts permission to string format for storage in Firestore.
     * Example: Permission("events", "create", "own") -> "events:create:own"
     */
    override fun toString(): String = "$resource:$action:$scope"

    /**
     * Checks if this permission matches or includes another permission.
     * Supports wildcard matching: "events:*:*" matches "events:create:own"
     *
     * @param other The permission to check against
     * @return true if this permission grants access to the other permission
     */
    fun matches(other: Permission): Boolean {
        return (resource == "*" || resource == other.resource) &&
               (action == "*" || action == other.action) &&
               (scope == "*" || scope == other.scope)
    }

    companion object {
        /**
         * Parses a permission string into a Permission object.
         * Example: "events:create:own" -> Permission("events", "create", "own")
         *
         * @param str The permission string to parse
         * @return Parsed Permission object with defaults for missing parts
         */
        fun fromString(str: String): Permission {
            val parts = str.split(":")
            return Permission(
                resource = parts.getOrNull(0) ?: "",
                action = parts.getOrNull(1) ?: "",
                scope = parts.getOrNull(2) ?: "own"
            )
        }

        /**
         * Creates a wildcard permission for a specific resource.
         * Example: forResource("events") -> Permission("events", "*", "*")
         * This grants all actions and scopes for the resource.
         */
        fun forResource(resource: String) = Permission(resource, "*", "*")

        /**
         * Super admin wildcard permission - grants access to everything.
         */
        val SUPER_ADMIN = Permission("*", "*", "*")
    }
}

/**
 * Predefined permission constants for the CampusConnect application.
 * These permissions should match those checked in Firestore security rules
 * and Cloud Functions for consistent authorization.
 *
 * Naming convention: RESOURCE_ACTION
 * Scope is appended when needed (_OWN, _DEPARTMENT, _ALL)
 */
object Permissions {

    // ========== MEETINGS & ANNOUNCEMENTS ==========

    /** Manage meetings and announcements */
    const val MEETINGS_MANAGE = "meetings:manage"

    // Legacy aliases kept for compatibility with existing templates/usages.
    const val EVENTS_CREATE = MEETINGS_MANAGE
    const val EVENTS_EDIT_OWN = MEETINGS_MANAGE
    const val EVENTS_EDIT_ALL = MEETINGS_MANAGE
    const val EVENTS_DELETE_OWN = MEETINGS_MANAGE
    const val EVENTS_DELETE_ALL = MEETINGS_MANAGE

    /** Feature events on homepage */
    const val EVENTS_FEATURE = "events:feature:all"

    /** Moderate and review reported events */
    const val EVENTS_MODERATE = "events:moderate:all"

    // ========== NOTES MANAGEMENT ==========

    /** Manage notes (upload/edit/delete) */
    const val NOTES_MANAGE = "notes:manage"

    // Legacy aliases kept for compatibility with existing templates/usages.
    const val NOTES_UPLOAD = NOTES_MANAGE
    const val NOTES_EDIT_OWN = NOTES_MANAGE
    const val NOTES_MODERATE = NOTES_MANAGE
    const val NOTES_DELETE = NOTES_MANAGE

    /** Feature quality notes on homepage */
    const val NOTES_FEATURE = "notes:feature:all"

    // ========== SENIORS MANAGEMENT ==========

    /** Manage seniors (add/edit/delete) */
    const val SENIORS_MANAGE = "seniors:manage"

    // Legacy aliases kept for compatibility with existing templates/usages.
    const val SENIORS_ADD = SENIORS_MANAGE
    const val SENIORS_EDIT = SENIORS_MANAGE
    const val SENIORS_DELETE = SENIORS_MANAGE
    const val SENIORS_VERIFY = SENIORS_MANAGE

    // ========== PLACEMENTS MANAGEMENT ==========

    /** Manage placements (create/edit/delete) */
    const val PLACEMENTS_MANAGE = "placements:manage"

    // Legacy aliases kept for compatibility with existing templates/usages.
    const val PLACEMENTS_ADD = PLACEMENTS_MANAGE
    const val PLACEMENTS_EDIT = PLACEMENTS_MANAGE
    const val PLACEMENTS_DELETE = PLACEMENTS_MANAGE

    // ========== USER MANAGEMENT ==========

    /** View user profiles and data */
    const val USERS_VIEW = "users:view:all"

    /** Edit user information */
    const val USERS_EDIT = "users:edit:all"

    /** Suspend user accounts temporarily */
    const val USERS_SUSPEND = "users:suspend:all"

    /** Permanently delete user accounts */
    const val USERS_DELETE = "users:delete:all"

    // ========== ADMIN MANAGEMENT (Super Admin Only) ==========

    /** Create new admin accounts */
    const val ADMINS_CREATE = "admins:create:all"

    /** Edit admin permissions */
    const val ADMINS_EDIT = "admins:edit:all"

    /** Delete admin accounts */
    const val ADMINS_DELETE = "admins:delete:all"

    /** Assign permissions to other admins */
    const val ADMINS_ASSIGN_PERMISSIONS = "admins:assign_permissions:all"

    /** Revoke admin access */
    const val ADMINS_REVOKE = "admins:revoke:all"

    // ========== ANALYTICS & REPORTING ==========

    /** View own analytics and statistics */
    const val ANALYTICS_VIEW_OWN = "analytics:view:own"

    /** View department-level analytics */
    const val ANALYTICS_VIEW_DEPARTMENT = "analytics:view:department"

    /** View all platform analytics */
    const val ANALYTICS_VIEW_ALL = "analytics:view:all"

    /** Generate system reports */
    const val REPORTS_GENERATE = "reports:generate:all"

    /** View generated reports */
    const val REPORTS_VIEW = "reports:view:all"

    // ========== SYSTEM SETTINGS ==========

    /** Modify application configuration */
    const val SETTINGS_APP_CONFIG = "settings:app_config:all"

    /** Manage security settings */
    const val SETTINGS_SECURITY = "settings:security:all"

    /** View system logs */
    const val LOGS_VIEW = "logs:view:all"

    /**
     * Returns all available permissions grouped by category.
     * Useful for admin UI when selecting permissions.
     *
     * @return Map of category name to list of permission strings
     */
    fun getAllGrouped(): Map<String, List<String>> = mapOf(
        "Meetings & Announcements" to listOf(
            MEETINGS_MANAGE,
            EVENTS_FEATURE,
            EVENTS_MODERATE
        ),
        "Notes" to listOf(
            NOTES_MANAGE,
            NOTES_FEATURE
        ),
        "Seniors" to listOf(
            SENIORS_MANAGE
        ),
        "Placements" to listOf(
            PLACEMENTS_MANAGE
        ),
        "Users" to listOf(
            USERS_VIEW,
            USERS_EDIT,
            USERS_SUSPEND,
            USERS_DELETE
        ),
        "Admins" to listOf(
            ADMINS_CREATE,
            ADMINS_EDIT,
            ADMINS_DELETE,
            ADMINS_ASSIGN_PERMISSIONS,
            ADMINS_REVOKE
        ),
        "Analytics" to listOf(
            ANALYTICS_VIEW_OWN,
            ANALYTICS_VIEW_DEPARTMENT,
            ANALYTICS_VIEW_ALL
        ),
        "Reports" to listOf(
            REPORTS_GENERATE,
            REPORTS_VIEW
        ),
        "System" to listOf(
            SETTINGS_APP_CONFIG,
            SETTINGS_SECURITY,
            LOGS_VIEW
        )
    )

    /**
     * Returns a human-readable description for a permission.
     * Used in UI to explain what each permission allows.
     *
     * @param permission The permission string to describe
     * @return Human-readable description
     */
    fun getDescription(permission: String): String = when (permission) {
        // Events
        MEETINGS_MANAGE -> "Manage Meetings & Announcements"
        EVENTS_FEATURE -> "Feature important events on the homepage"
        EVENTS_MODERATE -> "Review and moderate reported events"

        // Notes
        NOTES_MANAGE -> "Manage Notes"
        NOTES_FEATURE -> "Highlight quality notes for students"

        // Seniors
        SENIORS_MANAGE -> "Manage senior profile data"

        // Placements
        PLACEMENTS_MANAGE -> "Manage placement records and updates"

        // Users
        USERS_VIEW -> "View detailed user profiles and statistics"
        USERS_EDIT -> "Modify user account information"
        USERS_SUSPEND -> "Temporarily suspend user accounts"
        USERS_DELETE -> "Permanently delete user accounts"

        // Admins
        ADMINS_CREATE -> "Create new administrator accounts"
        ADMINS_EDIT -> "Modify admin user permissions"
        ADMINS_DELETE -> "Remove administrator accounts"
        ADMINS_ASSIGN_PERMISSIONS -> "Grant permissions to other admins"
        ADMINS_REVOKE -> "Revoke admin access from users"

        // Analytics
        ANALYTICS_VIEW_OWN -> "View your own activity statistics"
        ANALYTICS_VIEW_DEPARTMENT -> "View analytics for your department"
        ANALYTICS_VIEW_ALL -> "View platform-wide analytics and metrics"

        // Reports
        REPORTS_GENERATE -> "Generate custom system reports"
        REPORTS_VIEW -> "Access generated reports and exports"

        // System
        SETTINGS_APP_CONFIG -> "Modify application settings and configuration"
        SETTINGS_SECURITY -> "Manage security and privacy settings"
        LOGS_VIEW -> "View system activity and audit logs"

        // Fallback for unknown permissions
        else -> permission.split(":").joinToString(" ") {
            it.replace("_", " ").replaceFirstChar { char -> char.uppercase() }
        }
    }
}

