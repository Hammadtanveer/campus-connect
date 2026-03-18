package com.example.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.AdminAnalyticsRepository
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.util.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAnalyticsViewModel @Inject constructor(
    private val repository: AdminAnalyticsRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val currentUser = sessionManager.state
        .map { it.profile }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = sessionManager.state.value.profile
        )

    private val _countsState = MutableStateFlow<Resource<AdminAnalyticsRepository.AnalyticsCounts>>(Resource.Loading)
    val countsState: StateFlow<Resource<AdminAnalyticsRepository.AnalyticsCounts>> = _countsState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (!PermissionChecker.isSuperAdmin(currentUser.value)) {
            _countsState.value = Resource.Error("Only super admin can access analytics")
            return
        }

        viewModelScope.launch {
            _countsState.value = Resource.Loading
            _countsState.value = repository.fetchCounts()
        }
    }
}

