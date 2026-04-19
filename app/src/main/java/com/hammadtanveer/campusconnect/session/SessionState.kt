package com.hammadtanveer.campusconnect.session

import com.hammadtanveer.campusconnect.data.models.UserProfile

/**
 * Lightweight snapshot of the currently authenticated user.
 */
data class SessionState(
    val userId: String? = null,
    val email: String? = null,
    val profile: UserProfile? = null,
    val profileRevision: Long = 0L
)
