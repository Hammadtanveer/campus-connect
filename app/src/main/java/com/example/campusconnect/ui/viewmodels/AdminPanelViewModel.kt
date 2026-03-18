package com.example.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.data.repository.AdminUsersRepository
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.util.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    private val adminUsersRepository: AdminUsersRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    data class AdminFeatureVisibility(
        val analytics: Boolean,
        val userManagement: Boolean,
        val contentModeration: Boolean,
        val sendNotification: Boolean,
        val societyManagement: Boolean,
        val activityLog: Boolean
    )

    private var usersObserverJob: Job? = null

    companion object {
        val MANAGED_PERMISSION_KEYS = listOf(
            "is_admin",
            "can_manage_placements",
            "can_manage_events",
            "can_manage_notes",
            "can_manage_society_csss",
            "can_manage_society_tech_club"
        )
    }

    val currentUser = sessionManager.state
        .map { it.profile }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = sessionManager.state.value.profile
        )

    private val _usersState = MutableStateFlow<Resource<List<UserProfile>>>(Resource.Loading)
    val usersState: StateFlow<Resource<List<UserProfile>>> = _usersState.asStateFlow()

    private val _permissionUpdateStatus = MutableStateFlow<Resource<Unit>?>(null)
    val permissionUpdateStatus: StateFlow<Resource<Unit>?> = _permissionUpdateStatus.asStateFlow()

    private val _selectedUserState = MutableStateFlow<Resource<UserProfile>?>(null)
    val selectedUserState: StateFlow<Resource<UserProfile>?> = _selectedUserState.asStateFlow()

    private val _deleteUserStatus = MutableStateFlow<Resource<Unit>?>(null)
    val deleteUserStatus: StateFlow<Resource<Unit>?> = _deleteUserStatus.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser.collectLatest { user ->
                if (PermissionChecker.isSuperAdmin(user)) {
                    observeAllUsers()
                } else {
                    _usersState.value = Resource.Success(emptyList())
                }
            }
        }
    }

    private fun observeAllUsers() {
        usersObserverJob?.cancel()
        usersObserverJob = viewModelScope.launch {
            adminUsersRepository.observeUsers().collectLatest { result ->
                _usersState.value = result
            }
        }
    }

    fun canCurrentUserManageUsers(): Boolean = PermissionChecker.isSuperAdmin(currentUser.value)

    fun updatePermissionsForUser(userId: String, permissions: Map<String, Boolean>) {
        val actor = currentUser.value
        val actorId = actor?.id.orEmpty()

        if (!canCurrentUserManageUsers()) {
            _permissionUpdateStatus.value = Resource.Error("Only super admin can manage users")
            return
        }
        if (actorId.isBlank()) {
            _permissionUpdateStatus.value = Resource.Error("Not authenticated")
            return
        }

        viewModelScope.launch {
            _permissionUpdateStatus.value = Resource.Loading
            _permissionUpdateStatus.value = adminUsersRepository.updateUserPermissions(
                userId = userId,
                permissions = permissions,
                actorUserId = actorId
            )
        }
    }

    fun resetPermissionUpdateStatus() {
        _permissionUpdateStatus.value = null
    }

    fun hasPermission(permissionKey: String): Boolean {
        val user = currentUser.value ?: return false
        if (PermissionChecker.isSuperAdmin(user)) return true
        return user.permissions[permissionKey] == true
    }

    fun getUserById(userId: String): UserProfile? {
        val users = (usersState.value as? Resource.Success)?.data ?: return null
        return users.firstOrNull { it.id == userId }
    }

    fun loadUserById(userId: String) {
        if (!canCurrentUserManageUsers()) {
            _selectedUserState.value = Resource.Error("Only super admin can manage users")
            return
        }

        viewModelScope.launch {
            _selectedUserState.value = Resource.Loading
            _selectedUserState.value = adminUsersRepository.getUserById(userId)
        }
    }

    fun deleteUser(userId: String) {
        if (!canCurrentUserManageUsers()) {
            _deleteUserStatus.value = Resource.Error("Only super admin can delete users")
            return
        }

        viewModelScope.launch {
            _deleteUserStatus.value = Resource.Loading
            _deleteUserStatus.value = adminUsersRepository.deleteUser(userId)
        }
    }

    fun resetDeleteUserStatus() {
        _deleteUserStatus.value = null
    }

    fun getFeatureVisibility(user: UserProfile?): AdminFeatureVisibility {
        val isSuperAdmin = PermissionChecker.isSuperAdmin(user)
        val permissions = user?.permissions ?: emptyMap()

        val canModerateContent = isSuperAdmin || permissions["can_manage_notes"] == true

        val canManageSociety = isSuperAdmin ||
            permissions["can_manage_society"] == true ||
            permissions.keys.any { key ->
                key.startsWith("can_manage_society_") && permissions[key] == true
            }

        return AdminFeatureVisibility(
            analytics = isSuperAdmin,
            userManagement = isSuperAdmin,
            contentModeration = canModerateContent,
            sendNotification = isSuperAdmin,
            societyManagement = canManageSociety,
            activityLog = isSuperAdmin
        )
    }
}




