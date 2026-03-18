package com.example.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Note
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.NotesRepository
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
class ContentModerationViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _notesState = MutableStateFlow<Resource<List<Note>>>(Resource.Loading)
    val notesState: StateFlow<Resource<List<Note>>> = _notesState.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState.asStateFlow()

    private val _updatingNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val updatingNoteIds: StateFlow<Set<String>> = _updatingNoteIds.asStateFlow()

    init {
        observeNotes()
    }

    private fun observeNotes() {
        if (!canModerateNotes()) {
            _notesState.value = Resource.Error("You do not have permission to moderate notes")
            return
        }

        viewModelScope.launch {
            notesRepository.observeNotesForModeration().collectLatest { result ->
                _notesState.value = result
            }
        }
    }

    fun approveNote(noteId: String) {
        updateModeration(noteId = noteId, status = "approved")
    }

    fun rejectNote(noteId: String) {
        updateModeration(noteId = noteId, status = "rejected")
    }

    private fun updateModeration(noteId: String, status: String) {
        val profile = sessionManager.state.value.profile
        val reviewerId = profile?.id.orEmpty()
        val reviewerName = profile?.displayName.orEmpty()

        if (!canModerateNotes()) {
            _actionState.value = Resource.Error("You do not have permission to moderate notes")
            return
        }

        if (reviewerId.isBlank()) {
            _actionState.value = Resource.Error("Not authenticated")
            return
        }

        viewModelScope.launch {
            _updatingNoteIds.value = _updatingNoteIds.value + noteId
            _actionState.value = Resource.Loading
            _actionState.value = notesRepository.updateModerationStatus(
                noteId = noteId,
                status = status,
                reviewerId = reviewerId,
                reviewerName = reviewerName
            )
            _updatingNoteIds.value = _updatingNoteIds.value - noteId
        }
    }

    fun resetActionState() {
        _actionState.value = null
    }

    private fun canModerateNotes(): Boolean {
        val profile = sessionManager.state.value.profile
        return PermissionChecker.isSuperAdmin(profile) || profile?.permissions?.get("can_manage_notes") == true
    }
}

