package com.example.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.AdminActivityLogItem
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.AdminActivityLogRepository
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.util.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminActivityLogViewModel @Inject constructor(
    private val repository: AdminActivityLogRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _logsState = MutableStateFlow<Resource<List<AdminActivityLogItem>>>(Resource.Loading)
    val logsState: StateFlow<Resource<List<AdminActivityLogItem>>> = _logsState.asStateFlow()

    init {
        observeLogs()
    }

    private fun observeLogs() {
        if (!PermissionChecker.isSuperAdmin(sessionManager.state.value.profile)) {
            _logsState.value = Resource.Error("Only super admin can view activity logs")
            return
        }

        viewModelScope.launch {
            repository.observeRecentLogs(limit = 20).collectLatest { result ->
                _logsState.value = result
            }
        }
    }
}
