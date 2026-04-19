package com.hammadtanveer.campusconnect.util

import android.util.Log
import com.hammadtanveer.campusconnect.data.models.UserProfile
import com.hammadtanveer.campusconnect.security.PermissionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

object UserProfileMapper {

    private fun normalize(value: String): String = value.trim().lowercase()

    fun fromDocument(doc: DocumentSnapshot): UserProfile? {
        val raw = doc.data ?: return null
        val snapshotPermissions = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val merged = raw.toMutableMap()
        merged["permissions"] = snapshotPermissions
        Log.d("FINAL_DB", "snapshot=${doc.data}")
        return fromMap(doc.id, merged)
    }

    fun fromMap(id: String, data: Map<String, Any>): UserProfile {
        val firestorePermissions = (data["permissions"] as? List<*>)
            ?.filterIsInstance<String>()
            ?: emptyList()
        val normalizedPermissions = firestorePermissions
            .map { PermissionManager.normalizePermission(it) }
            .filter { it.isNotBlank() }
            .distinct()

        Log.d("FINAL_DB", "snapshot=$data")
        Log.d("PERM_DEBUG_PM", "Firestore permissions(userId=$id)=$normalizedPermissions")

        fun string(key: String): String = data[key] as? String ?: ""
        fun stringOrNull(key: String): String? = data[key] as? String
        fun bool(key: String): Boolean = data[key] as? Boolean ?: false
        fun timestamp(key: String): Timestamp? = data[key] as? Timestamp

        val normalizedRole = PermissionManager.normalizeRole(string("role"))
        val hasAdminModulePermission = normalizedPermissions.any {
            it == "meetings:manage" ||
                it == "notes:manage" ||
                it == "placements:manage" ||
                it == "admin:access" ||
                (it.startsWith("society:") && it.endsWith(":manage"))
        }

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
            role = normalizedRole.ifBlank {
                if (hasAdminModulePermission) "admin" else "user"
            },
            permissions = normalizedPermissions,
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
        ).also { mapped ->
            PermissionManager.logProfileSnapshot(mapped, "UserProfileMapper")
        }
    }
}

