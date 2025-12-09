package com.example.campusconnect.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {
    @Query("SELECT * FROM notes ORDER BY uploadedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE semester = :semester ORDER BY uploadedAt DESC")
    fun getNotesBySemester(semester: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE subject = :subject ORDER BY uploadedAt DESC")
    fun getNotesBySubject(subject: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: String)

    @Query("DELETE FROM notes")
    suspend fun clearAll()
}

@Dao
interface EventsDao {
    @Query("SELECT * FROM events ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: String)

    @Query("DELETE FROM events")
    suspend fun clearAll()

    // Offline-first sync methods
    @Query("SELECT * FROM events")
    suspend fun getAllEventsOnce(): List<EventEntity>

    @Query("SELECT * FROM events WHERE isDirty = 1")
    suspend fun getDirtyEvents(): List<EventEntity>

    @Query("UPDATE events SET isDirty = 0, lastSynced = :syncTime WHERE id = :eventId")
    suspend fun markAsSynced(eventId: String, syncTime: Long)

    @Query("UPDATE events SET isDirty = 1 WHERE id = :eventId")
    suspend fun markAsDirty(eventId: String)
}



