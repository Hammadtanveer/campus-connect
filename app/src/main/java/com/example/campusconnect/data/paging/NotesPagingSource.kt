package com.example.campusconnect.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.campusconnect.data.models.Note
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * PagingSource for notes to enable efficient pagination.
 *
 * Loads notes in pages of 20 items from Firestore.
 */
class NotesPagingSource(
    private val firestore: FirebaseFirestore,
    private val subject: String? = null,
    private val semester: String? = null,
    private val searchQuery: String? = null
) : PagingSource<DocumentSnapshot, Note>() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Note> {
        return try {
            var query: Query = firestore.collection("notes")
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())

            // Apply filters
            subject?.let { query = query.whereEqualTo("subject", it) }
            semester?.let { query = query.whereEqualTo("semester", it) }

            // Apply pagination
            params.key?.let { query = query.startAfter(it) }

            val snapshot = query.get().await()

            val notes = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Note::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }

            // Filter by search query if provided
            val filteredNotes = if (!searchQuery.isNullOrBlank()) {
                notes.filter { note ->
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.description.contains(searchQuery, ignoreCase = true)
                }
            } else {
                notes
            }

            LoadResult.Page(
                data = filteredNotes,
                prevKey = null, // Only forward pagination
                nextKey = if (snapshot.documents.isNotEmpty() && filteredNotes.size >= PAGE_SIZE) {
                    snapshot.documents.lastOrNull()
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Note>): DocumentSnapshot? {
        // Return null to always start from the beginning on refresh
        return null
    }
}

