package com.example.campusconnect.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Placement
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.PlacementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlacementViewModel @Inject constructor(
    private val repository: PlacementRepository
) : ViewModel() {

    private val _placements = MutableStateFlow<Resource<List<Placement>>>(Resource.Loading)
    val placements: StateFlow<Resource<List<Placement>>> = _placements

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
            repository.addPlacement(placement)
        }
    }

    fun deletePlacement(id: String) {
        viewModelScope.launch {
            repository.deletePlacement(id)
        }
    }

    suspend fun getPlacement(id: String): Resource<Placement> {
        return repository.getPlacement(id)
    }
}
