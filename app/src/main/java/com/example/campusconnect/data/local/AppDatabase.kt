package com.example.campusconnect.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, EventEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao
    abstract fun eventsDao(): EventsDao
}

