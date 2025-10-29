package com.example.campusconnect

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "events_channel"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_MEET_LINK = "meet_link"
        const val NOTIF_ID_BASE = 1000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        val title = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Upcoming event"
        val meetLink = intent.getStringExtra(EXTRA_MEET_LINK) ?: ""

        createNotificationChannel(context)

        // Intent to open the app (MainActivity)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_EVENT_ID, eventId)
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            (eventId ?: "").hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_event_24)
            .setContentTitle("Reminder: $title")
            .setContentText("Starts in 30 minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIF_ID_BASE + (eventId ?: "").hashCode(), builder.build())
        }
    }

    private fun createNotificationChannel(context: Context) {
        val name = "Event reminders"
        val descriptionText = "Notifications for scheduled events"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

