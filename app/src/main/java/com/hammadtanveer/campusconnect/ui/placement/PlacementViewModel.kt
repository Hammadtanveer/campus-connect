package com.hammadtanveer.campusconnect.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hammadtanveer.campusconnect.data.models.Placement
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.data.models.UserProfile
import com.hammadtanveer.campusconnect.data.repository.PlacementRepository
import com.hammadtanveer.campusconnect.security.PermissionManager
import com.hammadtanveer.campusconnect.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlacementViewModel @Inject constructor(
    private val repository: PlacementRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentUserProfile: Flow<UserProfile?> = sessionManager.state
        .map { it.profile }

    private val _placements = MutableStateFlow<Resource<List<Placement>>>(Resource.Loading)
    val placements: StateFlow<Resource<List<Placement>>> = _placements

    private val _deletePlacementStatus = MutableStateFlow<Resource<Unit>?>(null)
    val deletePlacementStatus = _deletePlacementStatus.asStateFlow()

    private val _savePlacementStatus = MutableStateFlow<Resource<Unit>?>(null)
    val savePlacementStatus = _savePlacementStatus.asStateFlow()

    init {
        loadPlacements()
    }

    fun loadPlacements() {
        viewModelScope.launch {
            repository.observePlacements().collect {
                _placements.value = it
            }
        }
    }

    fun addPlacement(placement: Placement) {
        val profile = sessionManager.state.value.profile
        val actorUserId = profile?.id.orEmpty()
        val actorUserName = profile?.displayName.orEmpty()

        if (!PermissionManager.canManagePlacements(profile)) {
            _savePlacementStatus.value = Resource.Error("No permission to post placements")
            return
        }

        if (actorUserId.isBlank()) {
            _savePlacementStatus.value = Resource.Error("Not authenticated")
            return
        }

        viewModelScope.launch {
            _savePlacementStatus.value = Resource.Loading
            when (
                val result = repository.addPlacement(
                    placement = placement,
                    actorUserId = actorUserId,
                    actorUserName = actorUserName
                )
            ) {
                is Resource.Success -> _savePlacementStatus.value = Resource.Success(Unit)
                is Resource.Error -> _savePlacementStatus.value = Resource.Error(result.message ?: "Failed to post placement")
                is Resource.Loading -> Unit
            }
        }
    }

    fun updatePlacement(
        placementId: String,
        placement: Placement
    ) {
        viewModelScope.launch {
            _savePlacementStatus.value = Resource.Loading
            _savePlacementStatus.value = repository.updatePlacement(placementId, placement)
        }
    }

    fun deletePlacement(
        id: String
    ) {
        val profile = sessionManager.state.value.profile
        if (!PermissionManager.canManagePlacements(profile)) {
            _deletePlacementStatus.value = Resource.Error("Only Admin/Super Admin can delete jobs")
            return
        }

        viewModelScope.launch {
            _deletePlacementStatus.value = Resource.Loading
            when (val result = repository.deletePlacement(id)) {
                is Resource.Success -> {
                    // Immediate UI update while Firestore listener syncs source-of-truth.
                    val current = _placements.value
                    if (current is Resource.Success) {
                        _placements.value = Resource.Success(current.data.filterNot { it.id == id })
                    }
                    _deletePlacementStatus.value = Resource.Success(Unit)
                }
                is Resource.Error -> _deletePlacementStatus.value = Resource.Error(result.message ?: "Delete failed")
                is Resource.Loading -> Unit
            }
        }
    }

    fun resetDeletePlacementStatus() {
        _deletePlacementStatus.value = null
    }

    fun resetSavePlacementStatus() {
        _savePlacementStatus.value = null
    }

    suspend fun getPlacement(id: String): Resource<Placement> {
        return repository.getPlacement(id)
    }
}
