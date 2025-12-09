package com.example.campusconnect.data.local

import com.example.campusconnect.data.models.Note
import com.example.campusconnect.data.models.OnlineEvent
import com.google.firebase.Timestamp

// Extension function to convert Note to NoteEntity
fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        description = description,
        subject = subject,
        semester = semester,
        fileName = fileName,
        fileSize = fileSize,
        fileType = fileType,
        fileUrl = fileUrl,
        uploaderId = uploaderId,
        uploaderName = uploaderName,
        uploadedAt = uploadedAt?.seconds?.times(1000),
        downloads = downloads,
        views = views
    )
}

// Extension function to convert NoteEntity to Note
fun NoteEntity.toNote(): Note {
    return Note(
        id = id,
        title = title,
        description = description,
        subject = subject,
        semester = semester,
        fileName = fileName,
        fileSize = fileSize,
        fileType = fileType,
        fileUrl = fileUrl,
        uploaderId = uploaderId,
        uploaderName = uploaderName,
        uploadedAt = uploadedAt?.let { Timestamp(it / 1000, 0) },
        downloads = downloads,
        views = views
    )
}

// Extension function to convert OnlineEvent to EventEntity
fun OnlineEvent.toEntity(): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        description = description,
        category = category.name,
        organizerId = organizerId,
        organizerName = organizerName,
        startTime = (dateTime?.seconds ?: 0) * 1000,
        durationMinutes = durationMinutes,
        maxParticipants = maxParticipants,
        meetLink = meetLink
    )
}

// Extension function to convert EventEntity to OnlineEvent
fun EventEntity.toEvent(): OnlineEvent {
    return OnlineEvent(
        id = id,
        title = title,
        description = description,
        category = com.example.campusconnect.data.models.EventCategory.valueOf(category),
        organizerId = organizerId,
        organizerName = organizerName,
        dateTime = Timestamp(startTime / 1000, 0),
        durationMinutes = durationMinutes,
        maxParticipants = maxParticipants,
        meetLink = meetLink
    )
}

