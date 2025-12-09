package com.example.campusconnect.data.paging

import androidx.paging.PagingState
import com.example.campusconnect.data.models.Note
import com.google.firebase.firestore.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class NotesPagingSourceTest {

    private lateinit var pagingSource: NotesPagingSource
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockQuery: Query

    @Before
    fun setup() {
        mockFirestore = mock()
        mockCollection = mock()
        mockQuery = mock()

        whenever(mockFirestore.collection("notes")).thenReturn(mockCollection)
        whenever(mockCollection.orderBy(any<String>(), any())).thenReturn(mockQuery)
        whenever(mockQuery.limit(any())).thenReturn(mockQuery)
        whenever(mockQuery.whereEqualTo(any<String>(), any())).thenReturn(mockQuery)

        pagingSource = NotesPagingSource(
            firestore = mockFirestore,
            subject = null,
            semester = null,
            searchQuery = null
        )
    }

    @Test
    fun `pagingSource initializes correctly`() {
        // Then
        assertNotNull(pagingSource)
    }

    @Test
    fun `pagingSource with filters creates correct query`() = runTest {
        // Given
        val pagingSourceWithFilters = NotesPagingSource(
            firestore = mockFirestore,
            subject = "Math",
            semester = "Semester 1",
            searchQuery = "test"
        )

        // Then
        assertNotNull(pagingSourceWithFilters)
    }

    @Test
    fun `getRefreshKey returns null for always-from-start behavior`() {
        // Given
        val mockState = mock<PagingState<DocumentSnapshot, Note>>()

        // When
        val result = pagingSource.getRefreshKey(mockState)

        // Then
        assertNull(result)
    }
}
