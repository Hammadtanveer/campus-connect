package com.example.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.AdminActivityLogItem
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.data.repository.AdminActivityLogRepository
import com.example.campusconnect.security.PermissionManager
import com.example.campusconnect.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminActivityLogViewModel @Inject constructor(
    private val repository: AdminActivityLogRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _logsState = MutableStateFlow<Resource<List<AdminActivityLogItem>>>(Resource.Loading)
    val logsState: StateFlow<Resource<List<AdminActivityLogItem>>> = _logsState.asStateFlow()
    private var logsObserverJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.state.map { it.profile }.collectLatest { profile ->
                _currentUser.value = profile
                observeLogs()
            }
        }
    }

    private fun observeLogs() {
        logsObserverJob?.cancel()
        if (!PermissionManager.canViewActivityLog(currentUser.value)) {
            _logsState.value = Resource.Error("Only super admin can view activity logs")
            return
        }

        logsObserverJob = viewModelScope.launch {
            repository.observeRecentLogs(limit = 20).collectLatest { result ->
                _logsState.value = result
            }
        }
    }
}
