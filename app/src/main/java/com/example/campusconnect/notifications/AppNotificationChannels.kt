package com.example.campusconnect.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AppNotificationChannels {
    const val EVENTS = "events"
    const val NOTES = "notes"
    const val JOBS = "jobs"
    const val SOCIETY = "society"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val eventsChannel = NotificationChannel(
            EVENTS,
            "Events",
            NotificationManager.IMPORTANCE_HIGH
        )

        val notesChannel = NotificationChannel(
            NOTES,
            "Notes",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val jobsChannel = NotificationChannel(
            JOBS,
            "Jobs",
            NotificationManager.IMPORTANCE_HIGH
        )

        val societyChannel = NotificationChannel(
            SOCIETY,
            "Society",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        notificationManager.createNotificationChannel(eventsChannel)
        notificationManager.createNotificationChannel(notesChannel)
        notificationManager.createNotificationChannel(jobsChannel)
        notificationManager.createNotificationChannel(societyChannel)
    }
}

