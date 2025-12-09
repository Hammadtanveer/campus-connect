package com.example.campusconnect.sync

import com.example.campusconnect.data.local.NoteEntity
import com.example.campusconnect.data.models.Note
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConflictResolverTest {

    private lateinit var resolver: ConflictResolver

    @Before
    fun setup() {
        resolver = ConflictResolver()
    }

    @Test
    fun `serverWins strategy always prefers server data`() = runTest {
        // Given
        val local = createLocalNote(lastModified = 1000L, isDirty = true)
        val remote = createRemoteNote(timestamp = 500L)

        // When
        val result = resolver.resolveNoteConflict(
            local, remote, ConflictResolver.Strategy.SERVER_WINS
        )

        // Then
        assertEquals(remote.id, result.id)
        assertFalse(result.isDirty)
        assertNotNull(result.lastSynced)
    }

    @Test
    fun `clientWins strategy always prefers local data`() = runTest {
        // Given
        val local = createLocalNote(lastModified = 500L, isDirty = true)
        val remote = createRemoteNote(timestamp = 1000L)

        // When
        val result = resolver.resolveNoteConflict(
            local, remote, ConflictResolver.Strategy.CLIENT_WINS
        )

        // Then
        assertEquals(local.id, result.id)
        assertTrue(result.isDirty)
    }

    @Test
    fun `lastWriteWins prefers newer timestamp`() = runTest {
        // Given
        val local = createLocalNote(lastModified = 2000L, isDirty = true)
        val remote = createRemoteNote(timestamp = 1000L)

        // When
        val result = resolver.resolveNoteConflict(
            local, remote, ConflictResolver.Strategy.LAST_WRITE_WINS
        )

        // Then - should prefer local (newer)
        assertEquals(local.id, result.id)
        assertTrue(result.isDirty)
    }

    @Test
    fun `mergeNotes combines local and remote correctly`() = runTest {
        // Given
        val local = listOf(
            createLocalNote("1", isDirty = true),
            createLocalNote("2", isDirty = false)
        )
        val remote = listOf(
            createRemoteNoteEntity("2"),
            createRemoteNoteEntity("3")
        )

        // When
        val result = resolver.mergeNotes(local, remote)

        // Then
        assertEquals(3, result.size) // 1 (local-only), 2 (merged), 3 (remote-only)
        assertTrue(result.any { it.id == "1" }) // Local-only note preserved
        assertTrue(result.any { it.id == "3" }) // Remote-only note added
    }

    private fun createLocalNote(
        id: String = "local-1",
        lastModified: Long = 1000L,
        isDirty: Boolean = false
    ) = NoteEntity(
        id = id,
        title = "Local Note",
        description = "Description",
        subject = "Math",
        semester = "Semester 1",
        fileName = "test.pdf",
        fileSize = 1024,
        fileType = "pdf",
        fileUrl = "https://example.com/test.pdf",
        uploaderId = "user1",
        uploaderName = "User One",
        uploadedAt = lastModified,
        downloads = 0,
        views = 0,
        lastModified = lastModified,
        isDirty = isDirty
    )

    private fun createRemoteNote(
        id: String = "remote-1",
        timestamp: Long = 1000L
    ) = Note(
        id = id,
        title = "Remote Note",
        description = "Description",
        subject = "Math",
        semester = "Semester 1",
        fileName = "test.pdf",
        fileSize = 1024,
        fileType = "pdf",
        fileUrl = "https://example.com/test.pdf",
        uploaderId = "user1",
        uploaderName = "User One",
        uploadedAt = Timestamp(timestamp / 1000, 0),
        downloads = 0,
        views = 0
    )

    private fun createRemoteNoteEntity(
        id: String = "remote-1",
        uploadedAt: Long = 1000L
    ) = NoteEntity(
        id = id,
        title = "Remote Note",
        description = "Description",
        subject = "Math",
        semester = "Semester 1",
        fileName = "test.pdf",
        fileSize = 1024,
        fileType = "pdf",
        fileUrl = "https://example.com/test.pdf",
        uploaderId = "user1",
        uploaderName = "User One",
        uploadedAt = uploadedAt,
        downloads = 0,
        views = 0
    )
}

