package com.example.campusconnect.session

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
        _state.value = _state.value.copy(userId = userId, email = email)
    }

    fun updateProfile(profile: UserProfile?) {
        _state.value = _state.value.copy(profile = profile)
    }
}
