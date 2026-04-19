package com.hammadtanveer.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.data.repository.AdminAnalyticsRepository
import com.hammadtanveer.campusconnect.data.models.UserProfile
import com.hammadtanveer.campusconnect.security.PermissionManager
import com.hammadtanveer.campusconnect.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminAnalyticsViewModel @Inject constructor(
    private val repository: AdminAnalyticsRepository,
    sessionManager: SessionManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _countsState = MutableStateFlow<Resource<AdminAnalyticsRepository.AnalyticsCounts>>(Resource.Loading)
    val countsState: StateFlow<Resource<AdminAnalyticsRepository.AnalyticsCounts>> = _countsState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.state.map { it.profile }.collectLatest { profile ->
                _currentUser.value = profile
                refresh()
            }
        }
    }

    fun refresh() {
        if (!PermissionManager.canViewAnalytics(currentUser.value)) {
            _countsState.value = Resource.Error("Only super admin can access analytics")
            return
        }

        viewModelScope.launch {
            _countsState.value = Resource.Loading
            _countsState.value = repository.fetchCounts()
        }
    }
}

