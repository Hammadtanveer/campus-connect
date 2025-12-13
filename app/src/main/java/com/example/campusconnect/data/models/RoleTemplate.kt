package com.example.campusconnect.data.models

/**
 * Represents a pre-configured admin role template.
 * Templates make it easy to assign common permission sets to new admins.
 *
 * Example: Society Admin template includes event creation permissions
 *
 * @property id Unique identifier for the template
 * @property name Display name for the role
 * @property description Brief explanation of what this role can do
 * @property permissions List of permission strings granted by this role
 * @property icon Material icon name for UI display
 * @property color Hex color code for role badge/chip
 */
data class RoleTemplate(
    val id: String,
    val name: String,
    val description: String,
    val permissions: List<String>,
    val icon: String = "admin_panel_settings",
    val color: String = "#6200EE"
)

/**
 * Predefined role templates for CampusConnect administrators.
 * These templates represent common admin roles in a campus environment.
 *
 * Usage:
 * - Super Admin creates new admin using a template
 * - Template auto-populates permissions
 * - Custom permissions can still be added/removed after template selection
 */
object RoleTemplates {

    /**
     * SOCIETY ADMIN
     * For student societies, clubs, and cultural organizations
     *
     * Capabilities:
     * - Create and manage events for their society
     * - Upload notes related to their activities
     * - View analytics for their events
     *
     * Use Case: IEEE, ACM, Drama Society, etc.
     */
    val SOCIETY_ADMIN = RoleTemplate(
        id = "society_admin",
        name = "Society Admin",
        description = "Manage events and share notes for student societies and clubs",
        permissions = listOf(
            Permissions.EVENTS_CREATE,
            Permissions.EVENTS_EDIT_OWN,
            Permissions.EVENTS_DELETE_OWN,
            Permissions.NOTES_UPLOAD,
            Permissions.NOTES_EDIT_OWN,
            Permissions.ANALYTICS_VIEW_OWN
        ),
        icon = "groups",
        color = "#FF6B6B"
    )

    /**
     * ACADEMIC MODERATOR
     * For faculty members, teaching assistants, and academic staff
     *
     * Capabilities:
     * - Upload and moderate study materials
     * - Verify senior alumni profiles
     * - Manage placement records
     * - View academic analytics
     *
     * Use Case: Professors, TAs, Department Heads
     */
    val ACADEMIC_MODERATOR = RoleTemplate(
        id = "academic_moderator",
        name = "Academic Moderator",
        description = "Manage academic content, notes moderation, and senior verification",
        permissions = listOf(
            Permissions.NOTES_UPLOAD,
            Permissions.NOTES_EDIT_OWN,
            Permissions.NOTES_MODERATE,
            Permissions.NOTES_FEATURE,
            Permissions.SENIORS_VERIFY,
            Permissions.PLACEMENTS_ADD,
            Permissions.PLACEMENTS_EDIT,
            Permissions.ANALYTICS_VIEW_DEPARTMENT,
            Permissions.ANALYTICS_VIEW_ALL
        ),
        icon = "school",
        color = "#4ECDC4"
    )

    /**
     * PLACEMENT COORDINATOR
     * For Training & Placement (T&P) cell members
     *
     * Capabilities:
     * - Full access to placement data
     * - Add and manage senior profiles
     * - View placement analytics
     * - Track student placement status
     *
     * Use Case: T&P Coordinator, Career Services
     */
    val PLACEMENT_COORDINATOR = RoleTemplate(
        id = "placement_coordinator",
        name = "Placement Coordinator",
        description = "Full access to placement records and senior alumni management",
        permissions = listOf(
            Permissions.PLACEMENTS_ADD,
            Permissions.PLACEMENTS_EDIT,
            Permissions.PLACEMENTS_DELETE,
            Permissions.SENIORS_ADD,
            Permissions.SENIORS_EDIT,
            Permissions.SENIORS_VERIFY,
            Permissions.USERS_VIEW,
            Permissions.ANALYTICS_VIEW_DEPARTMENT,
            Permissions.ANALYTICS_VIEW_ALL,
            Permissions.REPORTS_VIEW
        ),
        icon = "business_center",
        color = "#95E1D3"
    )

    /**
     * EVENT MANAGER
     * For student council, event management teams
     *
     * Capabilities:
     * - Create and manage campus-wide events
     * - Feature important events
     * - Edit any event regardless of creator
     * - View event analytics
     *
     * Use Case: Student Council, Fest Coordinators
     */
    val EVENT_MANAGER = RoleTemplate(
        id = "event_manager",
        name = "Event Manager",
        description = "Manage campus-wide events with full editing and featuring powers",
        permissions = listOf(
            Permissions.EVENTS_CREATE,
            Permissions.EVENTS_EDIT_OWN,
            Permissions.EVENTS_EDIT_ALL,
            Permissions.EVENTS_DELETE_ALL,
            Permissions.EVENTS_FEATURE,
            Permissions.EVENTS_MODERATE,
            Permissions.ANALYTICS_VIEW_ALL
        ),
        icon = "event",
        color = "#F38181"
    )

    /**
     * CONTENT MODERATOR
     * For community managers and content reviewers
     *
     * Capabilities:
     * - Review and moderate all content
     * - Suspend problematic users
     * - Delete inappropriate content
     * - View moderation reports
     *
     * Use Case: Community Managers, Safety Officers
     */
    val CONTENT_MODERATOR = RoleTemplate(
        id = "content_moderator",
        name = "Content Moderator",
        description = "Enforce community guidelines and moderate platform content",
        permissions = listOf(
            Permissions.NOTES_MODERATE,
            Permissions.NOTES_DELETE,
            Permissions.EVENTS_MODERATE,
            Permissions.USERS_VIEW,
            Permissions.USERS_SUSPEND,
            Permissions.ANALYTICS_VIEW_ALL,
            Permissions.REPORTS_VIEW
        ),
        icon = "shield",
        color = "#AA96DA"
    )

    /**
     * ANALYTICS VIEWER
     * For data analysts, administrators who need view-only access
     *
     * Capabilities:
     * - View all platform analytics
     * - Generate and view reports
     * - No modification permissions
     *
     * Use Case: Management, Data Analysts, Observers
     */
    val ANALYTICS_VIEWER = RoleTemplate(
        id = "analytics_viewer",
        name = "Analytics Viewer",
        description = "View-only access to analytics and reports without modification rights",
        permissions = listOf(
            Permissions.ANALYTICS_VIEW_ALL,
            Permissions.REPORTS_VIEW,
            Permissions.REPORTS_GENERATE,
            Permissions.USERS_VIEW
        ),
        icon = "analytics",
        color = "#FCBAD3"
    )

    /**
     * DEPARTMENT HEAD
     * For department administrators
     *
     * Capabilities:
     * - Manage department-specific content
     * - View department analytics
     * - Moderate department notes
     * - Verify department seniors
     *
     * Use Case: HOD, Department Coordinators
     */
    val DEPARTMENT_HEAD = RoleTemplate(
        id = "department_head",
        name = "Department Head",
        description = "Department-level administration with content and analytics access",
        permissions = listOf(
            Permissions.NOTES_UPLOAD,
            Permissions.NOTES_MODERATE,
            Permissions.NOTES_FEATURE,
            Permissions.SENIORS_ADD,
            Permissions.SENIORS_EDIT,
            Permissions.SENIORS_VERIFY,
            Permissions.PLACEMENTS_ADD,
            Permissions.PLACEMENTS_EDIT,
            Permissions.ANALYTICS_VIEW_DEPARTMENT,
            Permissions.ANALYTICS_VIEW_ALL,
            Permissions.USERS_VIEW,
            Permissions.REPORTS_VIEW
        ),
        icon = "manage_accounts",
        color = "#A8E6CF"
    )

    /**
     * Returns all available role templates.
     * Used in admin creation UI to show template options.
     *
     * @return List of all predefined role templates
     */
    fun getAll(): List<RoleTemplate> = listOf(
        SOCIETY_ADMIN,
        ACADEMIC_MODERATOR,
        PLACEMENT_COORDINATOR,
        EVENT_MANAGER,
        CONTENT_MODERATOR,
        ANALYTICS_VIEWER,
        DEPARTMENT_HEAD
    )

    /**
     * Finds a role template by its ID.
     *
     * @param id The template ID to search for
     * @return The matching RoleTemplate, or null if not found
     */
    fun findById(id: String): RoleTemplate? = getAll().find { it.id == id }

    /**
     * Returns templates grouped by category for better UI organization.
     *
     * @return Map of category name to list of templates
     */
    fun getGrouped(): Map<String, List<RoleTemplate>> = mapOf(
        "Student Organizations" to listOf(SOCIETY_ADMIN, EVENT_MANAGER),
        "Academic Staff" to listOf(ACADEMIC_MODERATOR, DEPARTMENT_HEAD),
        "Administration" to listOf(PLACEMENT_COORDINATOR, CONTENT_MODERATOR),
        "View-Only Roles" to listOf(ANALYTICS_VIEWER)
    )

    /**
     * Returns recommended templates based on user context.
     *
     * @param userEmail Email domain can suggest appropriate templates
     * @return List of recommended templates
     */
    fun getRecommended(userEmail: String): List<RoleTemplate> {
        val domain = userEmail.substringAfter("@").lowercase()

        return when {
            // Faculty email domains
            domain.contains("faculty") || domain.contains("staff") ->
                listOf(ACADEMIC_MODERATOR, DEPARTMENT_HEAD)

            // Student email domains
            domain.contains("student") || domain.contains("ug") || domain.contains("pg") ->
                listOf(SOCIETY_ADMIN, EVENT_MANAGER)

            // Placement cell
            domain.contains("placement") || domain.contains("career") ->
                listOf(PLACEMENT_COORDINATOR)

            // Admin/Management
            domain.contains("admin") || domain.contains("management") ->
                listOf(DEPARTMENT_HEAD, CONTENT_MODERATOR, ANALYTICS_VIEWER)

            // Default recommendations
            else -> listOf(SOCIETY_ADMIN, EVENT_MANAGER)
        }
    }

    /**
     * Validates if a custom permission set is similar to any template.
     * Useful for suggesting templates during custom admin creation.
     *
     * @param permissions List of permission strings
     * @return Template with highest match percentage, or null if no good match
     */
    fun findSimilarTemplate(permissions: List<String>): Pair<RoleTemplate, Float>? {
        if (permissions.isEmpty()) return null

        return getAll()
            .map { template ->
                val matchCount = permissions.count { it in template.permissions }
                val similarity = matchCount.toFloat() / template.permissions.size.coerceAtLeast(1)
                template to similarity
            }
            .filter { it.second >= 0.5f } // At least 50% match
            .maxByOrNull { it.second }
    }
}

