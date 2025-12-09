package com.example.campusconnect.session

import com.example.campusconnect.data.models.UserProfile

/**
 * Lightweight snapshot of the currently authenticated user.
 */
data class SessionState(
    val userId: String? = null,
    val email: String? = null,
    val profile: UserProfile? = null
)
