package com.example.campusconnect.session

import android.util.Log
import com.example.campusconnect.data.models.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the current session information shared across ViewModels.
 */
@Singleton
class SessionManager @Inject constructor() {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    fun updateAuth(userId: String?, email: String?) {
        Log.d("PERM_DEBUG", "SessionManager.updateAuth userId=${userId ?: "null"} emailPresent=${!email.isNullOrBlank()}")
        _state.value = _state.value.copy(userId = userId, email = email)
    }

    fun updateProfile(profile: UserProfile?) {
        val normalizedPermissions = profile?.permissions?.map { it.trim().lowercase() } ?: emptyList()
        val copiedProfile = profile?.copy(
            role = profile.role.trim().lowercase(),
            permissions = normalizedPermissions
        )
        Log.d(
            "PERM_DEBUG",
            "SessionManager.updateProfile userId=${copiedProfile?.id ?: "null"} role=${copiedProfile?.role ?: ""} permissions=$normalizedPermissions"
        )
        Log.d("PERM_DEBUG_UI", "profile perms=${copiedProfile?.permissions ?: emptyList<String>()}")
        _state.value = _state.value.copy(
            profile = copiedProfile,
            profileRevision = _state.value.profileRevision + 1
        )
    }
}
