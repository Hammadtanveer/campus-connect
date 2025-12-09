package com.example.campusconnect.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "notes", indices = [Index("semester"), Index("subject"), Index("uploaderId")])
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val subject: String,
    val semester: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val fileUrl: String,
    val uploaderId: String,
    val uploaderName: String,
    val uploadedAt: Long?,
    val downloads: Int,
    val views: Int,
    val cloudinaryPublicId: String = "",

    // Sync tracking fields for offline-first
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null,
    val isDirty: Boolean = false,
    val version: Int = 1
)

@Entity(tableName = "events", indices = [Index("startTime"), Index("organizerId")])
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val organizerId: String,
    val organizerName: String,
    val startTime: Long,
    val durationMinutes: Long,
    val maxParticipants: Int,
    val meetLink: String,

    // Sync tracking fields for offline-first
    val lastModified: Long = System.currentTimeMillis(),
    val lastSynced: Long? = null,
    val isDirty: Boolean = false
)

