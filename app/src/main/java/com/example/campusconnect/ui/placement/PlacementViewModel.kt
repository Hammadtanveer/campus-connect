package com.example.campusconnect.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Placement
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.PlacementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlacementViewModel @Inject constructor(
    private val repository: PlacementRepository
) : ViewModel() {

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
        viewModelScope.launch {
            _savePlacementStatus.value = Resource.Loading
            when (val result = repository.addPlacement(placement)) {
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

    private fun isAdminOrSuperAdmin(role: String?, isAdminFlag: Boolean): Boolean {
        if (isAdminFlag) return true
        return when (role.orEmpty().trim().lowercase()) {
            "admin", "super_admin", "superadmin" -> true
            else -> false
        }
    }

    fun deletePlacement(
        id: String,
        currentUserRole: String?,
        isAdminFlag: Boolean
    ) {
        if (!isAdminOrSuperAdmin(currentUserRole, isAdminFlag)) {
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
