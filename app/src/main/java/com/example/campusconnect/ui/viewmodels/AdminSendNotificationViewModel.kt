package com.example.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.data.repository.AdminNotificationRepository
import com.example.campusconnect.notifications.NotificationTopics
import com.example.campusconnect.security.PermissionManager
import com.example.campusconnect.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminSendNotificationViewModel @Inject constructor(
    private val repository: AdminNotificationRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    data class TopicOption(
        val label: String,
        val topic: String,
        val type: String
    )

    val topicOptions = listOf(
        TopicOption("All Students", NotificationTopics.ALL_STUDENTS, "general"),
        TopicOption("Events", NotificationTopics.EVENTS, "events"),
        TopicOption("Placements", NotificationTopics.PLACEMENTS, "placements"),
        TopicOption("Society Updates", NotificationTopics.SOCIETY_UPDATES, "societies"),
        TopicOption("Notes", NotificationTopics.NOTES, "notes")
    )

    private val _sendState = MutableStateFlow<Resource<Unit>?>(null)
    val sendState: StateFlow<Resource<Unit>?> = _sendState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.state.map { it.profile }.collectLatest { profile ->
                _currentUser.value = profile
            }
        }
    }

    fun sendNotification(topic: TopicOption, title: String, body: String) {
        val profile = currentUser.value

        if (!PermissionManager.canSendNotifications(profile)) {
            _sendState.value = Resource.Error("Only super admin can send notifications")
            return
        }

        if (title.isBlank() || body.isBlank()) {
            _sendState.value = Resource.Error("Title and message are required")
            return
        }

        val createdByUid = profile?.id.orEmpty()
        if (createdByUid.isBlank()) {
            _sendState.value = Resource.Error("Not authenticated")
            return
        }

        viewModelScope.launch {
            _sendState.value = Resource.Loading
            _sendState.value = repository.queueTopicNotification(
                topic = topic.topic,
                title = title.trim(),
                body = body.trim(),
                type = topic.type,
                createdByUid = createdByUid,
                createdByName = profile?.displayName.orEmpty().ifBlank { "Super Admin" }
            )
        }
    }

    fun resetSendState() {
        _sendState.value = null
    }
}

