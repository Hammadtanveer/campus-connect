package com.example.campusconnect.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.SocietyEvent
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.data.repository.SocietyEventRepository
import com.example.campusconnect.security.PermissionManager
import com.example.campusconnect.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: SocietyEventRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _societyEventsState = MutableStateFlow<Resource<List<SocietyEvent>>>(Resource.Loading)
    val societyEventsState: StateFlow<Resource<List<SocietyEvent>>> = _societyEventsState.asStateFlow()

    private var eventsJob: Job? = null
    private var activeSocietyId: String? = null

    var addEventStatus by mutableStateOf<Resource<String>?>(null)
        private set

    var updateEventStatus by mutableStateOf<Resource<Unit>?>(null)
        private set

    var deleteEventStatus by mutableStateOf<Resource<Unit>?>(null)
        private set

    var selectedEvent by mutableStateOf<SocietyEvent?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val currentUserProfileFlow: Flow<UserProfile?> = sessionManager.state.map { it.profile }

    fun observeSocietyEvents(societyId: String): Flow<Resource<List<SocietyEvent>>> {
        return repository.observeEventsBySociety(societyId)
    }

    fun startObservingSocietyEvents(societyId: String) {
        if (activeSocietyId == societyId && eventsJob?.isActive == true) return

        eventsJob?.cancel()
        activeSocietyId = societyId

        eventsJob = viewModelScope.launch {
            repository.observeEventsBySociety(societyId).collect { newState ->
                if (newState != _societyEventsState.value) {
                    _societyEventsState.value = newState
                }
            }
        }
    }

    fun stopObservingSocietyEvents(societyId: String? = null) {
        if (societyId == null || activeSocietyId == societyId) {
            eventsJob?.cancel()
            eventsJob = null
            activeSocietyId = null
        }
    }

    fun getSocietyEvent(societyId: String, eventId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.getEventById(societyId, eventId)) {
                is Resource.Success -> selectedEvent = result.data
                is Resource.Error -> errorMessage = result.message
                is Resource.Loading -> Unit
            }
            isLoading = false
        }
    }

    fun canCreateSocietyEvent(profile: UserProfile?): Boolean {
        return PermissionManager.canManageSociety(profile)
    }

    fun canEditSocietyEvent(profile: UserProfile?): Boolean = canCreateSocietyEvent(profile)

    fun canDeleteSocietyEvent(profile: UserProfile?): Boolean {
        return PermissionManager.canDeleteSocietyEvent(profile)
    }

    fun createEvent(
        societyId: String,
        societyName: String,
        name: String,
        date: String,
        time: String,
        venue: String,
        coordinator: String,
        convener: String,
        register: String,
        posterUrl: String,
        posterPublicId: String,
        profile: UserProfile?
    ) {
        if (!canCreateSocietyEvent(profile)) {
            addEventStatus = Resource.Error("Only admin and super admin can create events")
            return
        }

        viewModelScope.launch {
            addEventStatus = Resource.Loading
            val event = SocietyEvent(
                societyId = societyId,
                societyName = societyName,
                name = name,
                date = date,
                time = time,
                venue = venue,
                coordinator = coordinator,
                convener = convener,
                register = register,
                posterUrl = posterUrl,
                posterPublicId = posterPublicId,
                createdBy = profile?.id.orEmpty(),
                createdByRole = PermissionManager.normalizeRole(profile?.role).ifBlank { "user" }
            )
            addEventStatus = repository.addEvent(societyId, event)
        }
    }

    fun updateEvent(societyId: String, eventId: String, updated: SocietyEvent, profile: UserProfile?) {
        if (!canEditSocietyEvent(profile)) {
            updateEventStatus = Resource.Error("Only admin and super admin can edit events")
            return
        }

        viewModelScope.launch {
            updateEventStatus = Resource.Loading
            updateEventStatus = repository.updateEvent(societyId, eventId, updated)
            if (updateEventStatus is Resource.Success) {
                selectedEvent = updated.copy(id = eventId, societyId = societyId)
            }
        }
    }

    fun deleteEvent(societyId: String, eventId: String, profile: UserProfile?) {
        if (!canDeleteSocietyEvent(profile)) {
            deleteEventStatus = Resource.Error("Only super admin can delete events")
            return
        }

        viewModelScope.launch {
            deleteEventStatus = Resource.Loading
            deleteEventStatus = repository.deleteEvent(societyId, eventId)
        }
    }

    fun uploadPoster(
        societyId: String,
        file: File,
        onResult: (url: String?, publicId: String?, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val uploadResult = repository.uploadPoster(societyId, file)) {
                is Resource.Success -> onResult(uploadResult.data.secureUrl, uploadResult.data.publicId, null)
                is Resource.Error -> onResult(null, null, uploadResult.message ?: "Failed to upload poster")
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearSelectedEvent() {
        selectedEvent = null
    }

    fun resetStatus() {
        addEventStatus = null
        updateEventStatus = null
        deleteEventStatus = null
    }

    override fun onCleared() {
        eventsJob?.cancel()
        super.onCleared()
    }
}
