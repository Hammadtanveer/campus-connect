package com.example.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Note
import com.example.campusconnect.data.models.NoteFilter
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.NotesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotesViewModel : ViewModel() {

    private val repository = NotesRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _allNotesState = MutableStateFlow(NotesUiState())
    val allNotesState: StateFlow<NotesUiState> = _allNotesState.asStateFlow()

    private val _myNotesState = MutableStateFlow(NotesUiState())
    val myNotesState: StateFlow<NotesUiState> = _myNotesState.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()

    private val _selectedSemester = MutableStateFlow<String?>(null)
    val selectedSemester: StateFlow<String?> = _selectedSemester.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    private val _deleteInProgress = MutableStateFlow<String?>(null)
    val deleteInProgress: StateFlow<String?> = _deleteInProgress.asStateFlow()

    init {
        loadAllNotes()
        loadMyNotes()
    }

    /**
     * Load all notes with current filters
     */
    fun loadAllNotes() {
        viewModelScope.launch {
            repository.observeNotes(
                subject = _selectedSubject.value,
                semester = _selectedSemester.value,
                searchQuery = _searchQuery.value
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _allNotesState.value = _allNotesState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _allNotesState.value = NotesUiState(
                            notes = resource.data ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _allNotesState.value = NotesUiState(
                            notes = emptyList(),
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Load my uploaded notes
     */
    fun loadMyNotes() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            repository.observeMyNotes(userId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _myNotesState.value = _myNotesState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _myNotesState.value = NotesUiState(
                            notes = resource.data ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _myNotesState.value = NotesUiState(
                            notes = emptyList(),
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Set subject filter
     */
    fun setSubjectFilter(subject: String?) {
        _selectedSubject.value = subject
        loadAllNotes()
    }

    /**
     * Set semester filter
     */
    fun setSemesterFilter(semester: String?) {
        _selectedSemester.value = semester
        loadAllNotes()
    }

    /**
     * Set search query
     */
    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
        loadAllNotes()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _selectedSubject.value = null
        _selectedSemester.value = null
        _searchQuery.value = null
        loadAllNotes()
    }

    /**
     * Delete note
     */
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            _deleteInProgress.value = note.id

            val result = repository.deleteNote(note.id, note.cloudinaryPublicId)

            when (result) {
                is Resource.Error -> {
                    // Show error (UI will handle this)
                    _allNotesState.value = _allNotesState.value.copy(
                        error = result.message
                    )
                }
                else -> {
                    // Success - note will be removed automatically via Flow
                }
            }

            _deleteInProgress.value = null
        }
    }

    /**
     * Increment download count
     */
    fun recordDownload(noteId: String) {
        viewModelScope.launch {
            repository.incrementDownloads(noteId)
        }
    }

    /**
     * Increment view count
     */
    fun recordView(noteId: String) {
        viewModelScope.launch {
            repository.incrementViews(noteId)
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _allNotesState.value = _allNotesState.value.copy(error = null)
        _myNotesState.value = _myNotesState.value.copy(error = null)
    }
}

