package com.hammadtanveer.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.hammadtanveer.campusconnect.data.models.Note
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.data.models.UserProfile
import com.hammadtanveer.campusconnect.data.repository.NotesRepository
import com.hammadtanveer.campusconnect.security.PermissionManager
import com.hammadtanveer.campusconnect.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ContentModerationViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val firestore: FirebaseFirestore,
    sessionManager: SessionManager
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _notesState = MutableStateFlow<Resource<List<Note>>>(Resource.Loading)
    val notesState: StateFlow<Resource<List<Note>>> = _notesState.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState.asStateFlow()

    private val _updatingNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val updatingNoteIds: StateFlow<Set<String>> = _updatingNoteIds.asStateFlow()
    private var notesObserverJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.state.map { it.profile }.collectLatest { profile ->
                _currentUser.value = profile
                observeNotes()
            }
        }
    }

    private fun observeNotes() {
        notesObserverJob?.cancel()
        if (!canModerateNotes()) {
            _notesState.value = Resource.Error("You do not have permission to moderate notes")
            return
        }

        notesObserverJob = viewModelScope.launch {
            repository.observeNotesForModeration().collectLatest { result ->
                _notesState.value = result
            }
        }
    }

    fun getFilteredNotes(status: String): List<com.hammadtanveer.campusconnect.data.models.Note> {
        val allNotes = (_notesState.value as? Resource.Success)?.data ?: emptyList()
        return when (status) {
            "All" -> allNotes
            "Pending" -> allNotes.filter { it.moderationStatus == "pending" }
            "Reported" -> allNotes.filter { it.moderationStatus == "reported" }
            "Approved" -> allNotes.filter { it.moderationStatus == "approved" }
            "Rejected" -> allNotes.filter { it.moderationStatus == "rejected" }
            else -> allNotes
        }
    }

    fun approveNote(noteId: String) {
        updateModeration(noteId = noteId, status = "approved")
    }

    fun rejectNote(noteId: String) {
        viewModelScope.launch {
            try {
                val noteDoc = firestore.collection("notes")
                    .document(noteId)
                    .get()
                    .await()
                val cloudinaryPublicId = noteDoc.getString("cloudinaryPublicId")

                updateModeration(noteId = noteId, status = "rejected")
                repository.deleteNote(noteId, cloudinaryPublicId ?: "")
            } catch (e: Exception) {
                android.util.Log.e("MODERATION", "Failed to reject+delete note", e)
            }
        }
    }

    private fun updateModeration(noteId: String, status: String) {
        val profile = currentUser.value
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
            _actionState.value = repository.updateModerationStatus(
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
        return PermissionManager.canModerateContent(currentUser.value)
    }
}

