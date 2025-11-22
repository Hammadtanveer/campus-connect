package com.example.campusconnect.data.models

/**
 * Filter options for notes used by NotesRepository.observeNotes
 */
data class NoteFilter(
    val subject: String? = null,
    val semester: String? = null,
    val searchQuery: String? = null,
    val uploaderId: String? = null
)

