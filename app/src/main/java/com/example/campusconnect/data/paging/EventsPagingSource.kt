package com.example.campusconnect.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.campusconnect.data.models.OnlineEvent
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * PagingSource for events to enable efficient pagination.
 *
 * Loads events in pages of 20 items from Firestore.
 */
class EventsPagingSource(
    private val firestore: FirebaseFirestore,
    private val category: String? = null,
    private val upcomingOnly: Boolean = false
) : PagingSource<DocumentSnapshot, OnlineEvent>() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, OnlineEvent> {
        return try {
            var query: Query = firestore.collection("events")
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .limit(params.loadSize.toLong())

            // Apply category filter
            category?.let { query = query.whereEqualTo("category", it) }

            // Apply pagination
            params.key?.let { query = query.startAfter(it) }

            val snapshot = query.get().await()

            var events = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(OnlineEvent::class.java)?.let { event ->
                        if (event.id.isBlank()) event.copy(id = doc.id) else event
                    }
                } catch (e: Exception) {
                    null
                }
            }

            // Filter for upcoming events if requested
            if (upcomingOnly) {
                val now = System.currentTimeMillis()
                events = events.filter { event ->
                    (event.dateTime?.seconds ?: 0) * 1000 > now
                }
            }

            LoadResult.Page(
                data = events,
                prevKey = null,
                nextKey = if (snapshot.documents.isNotEmpty() && events.size >= PAGE_SIZE) {
                    snapshot.documents.lastOrNull()
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, OnlineEvent>): DocumentSnapshot? {
        return null
    }
}

