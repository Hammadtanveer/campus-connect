package com.example.campusconnect.sync

import com.example.campusconnect.data.local.NoteEntity
import com.example.campusconnect.data.models.Note

/**
 * Handles conflict resolution for offline-first sync.
 */
class ConflictResolver {

    enum class Strategy {
        SERVER_WINS,      // Server data always takes precedence
        CLIENT_WINS,      // Local data always takes precedence
        LAST_WRITE_WINS,  // Most recent modification wins
        MANUAL            // Throw exception for manual resolution
    }

    /**
     * Resolve conflict between local and remote note.
     */
    suspend fun resolveNoteConflict(
        local: NoteEntity,
        remote: Note,
        strategy: Strategy = Strategy.LAST_WRITE_WINS
    ): NoteEntity {
        return when (strategy) {
            Strategy.SERVER_WINS -> {
                // Always prefer server data
                remote.toEntity().copy(
                    lastSynced = System.currentTimeMillis(),
                    isDirty = false
                )
            }

            Strategy.CLIENT_WINS -> {
                // Always prefer local data
                local.copy(isDirty = true)
            }

            Strategy.LAST_WRITE_WINS -> {
                // Compare timestamps
                val remoteTimestamp = remote.uploadedAt?.seconds?.times(1000) ?: 0L

                if (local.lastModified > remoteTimestamp) {
                    // Local is newer, keep it but mark as dirty
                    local.copy(isDirty = true)
                } else {
                    // Remote is newer, use it
                    remote.toEntity().copy(
                        lastSynced = System.currentTimeMillis(),
                        isDirty = false
                    )
                }
            }

            Strategy.MANUAL -> {
                throw ConflictException(local, remote)
            }
        }
    }

    /**
     * Merge multiple notes, removing duplicates and resolving conflicts.
     */
    suspend fun mergeNotes(
        local: List<NoteEntity>,
        remote: List<NoteEntity>,
        strategy: Strategy = Strategy.LAST_WRITE_WINS
    ): List<NoteEntity> {
        val localMap = local.associateBy { it.id }
        val remoteMap = remote.associateBy { it.id }
        val merged = mutableListOf<NoteEntity>()

        // Process all remote notes
        remote.forEach { remoteNote ->
            val localNote = localMap[remoteNote.id]

            if (localNote != null && localNote.isDirty) {
                // Conflict exists - resolve it
                val resolved = when (strategy) {
                    Strategy.SERVER_WINS -> remoteNote.copy(
                        lastSynced = System.currentTimeMillis(),
                        isDirty = false
                    )
                    Strategy.CLIENT_WINS -> localNote
                    Strategy.LAST_WRITE_WINS -> {
                        if (localNote.lastModified > (remoteNote.uploadedAt ?: 0L)) {
                            localNote
                        } else {
                            remoteNote.copy(
                                lastSynced = System.currentTimeMillis(),
                                isDirty = false
                            )
                        }
                    }
                    Strategy.MANUAL -> throw ConflictException(
                        localNote,
                        Note() // Would need proper conversion
                    )
                }
                merged.add(resolved)
            } else {
                // No conflict, use remote
                merged.add(remoteNote.copy(
                    lastSynced = System.currentTimeMillis(),
                    isDirty = false
                ))
            }
        }

        // Add local-only notes
        local.forEach { localNote ->
            if (localNote.id !in remoteMap) {
                merged.add(localNote)
            }
        }

        return merged
    }

    /**
     * Exception thrown when manual conflict resolution is required.
     */
    class ConflictException(
        val local: NoteEntity,
        val remote: Note
    ) : Exception("Conflict between local and remote data requires manual resolution")
}

// Extension function to convert Note to NoteEntity
private fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        title = this.title,
        description = this.description,
        subject = this.subject,
        semester = this.semester,
        fileName = this.fileName,
        fileSize = this.fileSize,
        fileType = this.fileType,
        fileUrl = this.fileUrl,
        uploaderId = this.uploaderId,
        uploaderName = this.uploaderName,
        uploadedAt = this.uploadedAt?.seconds?.times(1000),
        downloads = this.downloads,
        views = this.views,
        cloudinaryPublicId = this.cloudinaryPublicId
    )
}

