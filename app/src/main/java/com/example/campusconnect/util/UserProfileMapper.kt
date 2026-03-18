package com.example.campusconnect.util

import com.example.campusconnect.data.models.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

object UserProfileMapper {

    fun fromDocument(doc: DocumentSnapshot): UserProfile? {
        return fromMap(doc.id, doc.data ?: return null)
    }

    fun fromMap(id: String, data: Map<String, Any>): UserProfile {
        val permissionsField = data["permissions"]
        val permissionsMap = when (permissionsField) {
            is Map<*, *> -> permissionsField.entries
                .mapNotNull { (k, v) -> (k as? String)?.let { key -> key to (v as? Boolean ?: false) } }
                .toMap()
            is List<*> -> permissionsField
                .filterIsInstance<String>()
                .associateWith { true }
            else -> emptyMap()
        }

        val legacyPermissions = when (val raw = data["permissionsList"]) {
            is List<*> -> raw.filterIsInstance<String>()
            else -> permissionsMap.filterValues { it }.keys.toList()
        }

        fun string(key: String): String = data[key] as? String ?: ""
        fun stringOrNull(key: String): String? = data[key] as? String
        fun bool(key: String): Boolean = data[key] as? Boolean ?: false
        fun timestamp(key: String): Timestamp? = data[key] as? Timestamp

        return UserProfile(
            id = if (string("id").isBlank()) id else string("id"),
            displayName = string("displayName"),
            email = string("email"),
            course = string("course"),
            branch = string("branch"),
            year = string("year"),
            bio = string("bio"),
            profilePictureUrl = string("profilePictureUrl"),
            eventCount = (data["eventCount"] as? Number)?.toInt() ?: 0,
            isAdmin = bool("isAdmin") || permissionsMap["is_admin"] == true,
            roles = (data["roles"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            role = string("role").ifBlank { if (permissionsMap["is_admin"] == true) "admin" else "user" },
            permissions = permissionsMap,
            permissionsList = legacyPermissions,
            department = stringOrNull("department"),
            status = string("status").ifBlank { "active" },
            createdBy = stringOrNull("createdBy"),
            expiresAt = timestamp("expiresAt"),
            lastModifiedBy = stringOrNull("lastModifiedBy"),
            lastModifiedAt = timestamp("lastModifiedAt"),
            roleTemplate = stringOrNull("roleTemplate"),
            suspendedBy = stringOrNull("suspendedBy"),
            suspendedAt = timestamp("suspendedAt"),
            suspendedUntil = timestamp("suspendedUntil"),
            suspensionReason = stringOrNull("suspensionReason"),
            revokedBy = stringOrNull("revokedBy"),
            revokedAt = timestamp("revokedAt"),
            revocationReason = stringOrNull("revocationReason")
        )
    }
}

