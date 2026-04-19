package com.hammadtanveer.campusconnect.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.hammadtanveer.campusconnect.util.DbgLog
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

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countNotes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    suspend fun insertNotesLogged(notes: List<NoteEntity>) {
        DbgLog.d("DAO", "NotesDao.insertNotes start size=${notes.size}")
        insertNotes(notes)
        DbgLog.d("DAO", "NotesDao.insertNotes done size=${notes.size}")
    }

    suspend fun countNotesLogged(): Int {
        val count = countNotes()
        DbgLog.d("DAO", "NotesDao.countNotes=$count")
        return count
    }

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

    @Query("SELECT COUNT(*) FROM events")
    suspend fun countEvents(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    suspend fun insertEventsLogged(events: List<EventEntity>) {
        DbgLog.d("DAO", "EventsDao.insertEvents start size=${events.size}")
        insertEvents(events)
        DbgLog.d("DAO", "EventsDao.insertEvents done size=${events.size}")
    }

    suspend fun countEventsLogged(): Int {
        val count = countEvents()
        DbgLog.d("DAO", "EventsDao.countEvents=$count")
        return count
    }

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



