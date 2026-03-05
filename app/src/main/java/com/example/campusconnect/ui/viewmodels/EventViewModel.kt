package com.example.campusconnect.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.SocietyEvent
import com.example.campusconnect.data.repository.SocietyEventRepository
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.util.PermissionChecker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: SocietyEventRepository,
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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

    var currentUserRole by mutableStateOf<String?>(null)
        private set

    var isRoleLoading by mutableStateOf(false)
        private set

    init {
        refreshUserRole()
    }

    private fun normalizeRole(role: String?): String {
        return role.orEmpty().trim().lowercase()
    }

    private fun isAdminRole(role: String?): Boolean {
        val normalized = normalizeRole(role)
        return normalized == "admin" || normalized == "superadmin" || normalized == "super_admin"
    }

    private fun isSuperAdminRole(role: String?): Boolean {
        val normalized = normalizeRole(role)
        return normalized == "superadmin" || normalized == "super_admin"
    }

    fun refreshUserRole() {
        val uid = auth.currentUser?.uid ?: run {
            currentUserRole = null
            return
        }

        isRoleLoading = true
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                currentUserRole = doc.getString("role")
                isRoleLoading = false
            }
            .addOnFailureListener {
                currentUserRole = null
                isRoleLoading = false
            }
    }

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

    fun canCreateSocietyEvent(): Boolean {
        val firestoreRoleAllowed = isAdminRole(currentUserRole)
        val sessionFallback = PermissionChecker.isAdminAccessValid(sessionManager.state.value.profile)
        return firestoreRoleAllowed || sessionFallback
    }

    fun canEditSocietyEvent(): Boolean = canCreateSocietyEvent()

    fun canDeleteSocietyEvent(): Boolean {
        val firestoreRoleAllowed = isSuperAdminRole(currentUserRole)
        val sessionFallback = PermissionChecker.isSuperAdmin(sessionManager.state.value.profile)
        return firestoreRoleAllowed || sessionFallback
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
        posterPublicId: String
    ) {
        val profile = sessionManager.state.value.profile
        if (!canCreateSocietyEvent()) {
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
                createdByRole = normalizeRole(currentUserRole).ifBlank { profile?.role ?: "user" }
            )
            addEventStatus = repository.addEvent(societyId, event)
        }
    }

    fun updateEvent(societyId: String, eventId: String, updated: SocietyEvent) {
        if (!canEditSocietyEvent()) {
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

    fun deleteEvent(societyId: String, eventId: String) {
        if (!canDeleteSocietyEvent()) {
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
