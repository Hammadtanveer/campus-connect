package com.example.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Paging 3 - Optional (requires library sync)
// import androidx.paging.Pager
// import androidx.paging.PagingConfig
// import androidx.paging.PagingData
// import androidx.paging.cachedIn
import com.example.campusconnect.data.models.Note
import com.example.campusconnect.data.models.Resource
// import com.example.campusconnect.data.paging.NotesPagingSource
import com.example.campusconnect.data.repository.NotesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.campusconnect.ui.state.UiState
import android.util.Log

/**
 * ViewModel for notes list and filtering functionality.
 *
 * Uses Hilt for dependency injection of repository and auth.
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _allNotesState = MutableStateFlow<UiState<List<Note>>>(UiState.Loading)
    val allNotesState: StateFlow<UiState<List<Note>>> = _allNotesState.asStateFlow()

    private val _myNotesState = MutableStateFlow<UiState<List<Note>>>(UiState.Loading)
    val myNotesState: StateFlow<UiState<List<Note>>> = _myNotesState.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()

    private val _selectedSemester = MutableStateFlow<String?>(null)
    val selectedSemester: StateFlow<String?> = _selectedSemester.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    private val _deleteInProgress = MutableStateFlow<String?>(null)
    val deleteInProgress: StateFlow<String?> = _deleteInProgress.asStateFlow()

    // Paging support for large lists (Optional - requires Paging 3 library)
    // private val _notesPagingFlow = MutableStateFlow<Flow<PagingData<Note>>?>(null)
    // val notesPagingFlow: StateFlow<Flow<PagingData<Note>>?> = _notesPagingFlow.asStateFlow()

    init {
        loadAllNotes()
        loadMyNotes()
        startPeriodicSync()
        // setupPaging() // Uncomment when Paging 3 is available
    }

    private fun startPeriodicSync() {
        viewModelScope.launch {
            while (true) {
                try { repository.syncNotes() } catch (_: Exception) {}
                delay(5 * 60 * 1000) // every 5 minutes
            }
        }
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
                    is Resource.Loading -> _allNotesState.value = UiState.Loading
                    is Resource.Success -> _allNotesState.value = UiState.Success(resource.data)
                    is Resource.Error -> {
                        _allNotesState.value = UiState.Error(resource.message ?: "Error loading notes")
                        Log.e("NotesViewModel", "loadAllNotes error: ${resource.message}")
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
                    is Resource.Loading -> _myNotesState.value = UiState.Loading
                    is Resource.Success -> _myNotesState.value = UiState.Success(resource.data)
                    is Resource.Error -> {
                        _myNotesState.value = UiState.Error(resource.message ?: "Error loading my notes")
                        // Log full error so it appears in Logcat (helps copy the Firebase Console URL)
                        Log.e("NotesViewModel", "loadMyNotes error: ${resource.message}")
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

            if (result is Resource.Error) {
                _allNotesState.value = UiState.Error(result.message ?: "Delete failed")
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
        if (allNotesState.value is UiState.Error) _allNotesState.value = UiState.Success(emptyList())
        if (myNotesState.value is UiState.Error) _myNotesState.value = UiState.Success(emptyList())
    }

    /*
    // Paging 3 methods - Uncomment when library is available

    /**
     * Set up paging for notes list
     */
    private fun setupPaging() {
        _notesPagingFlow.value = Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                NotesPagingSource(
                    firestore = firestore,
                    subject = _selectedSubject.value,
                    semester = _selectedSemester.value,
                    searchQuery = _searchQuery.value
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    /**
     * Refresh paging data when filters change
     */
    fun refreshPaging() {
        setupPaging()
    }
    */
}
