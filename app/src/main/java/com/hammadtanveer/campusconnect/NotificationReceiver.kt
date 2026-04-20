package com.hammadtanveer.campusconnect

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "events_channel"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_MEET_LINK = "meet_link"
        const val NOTIF_ID_BASE = 1000
    }

    @SuppressLint("MissingPermission", "NotificationPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        val title = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Upcoming event"

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
            .setContentTitle("Event reminder: $title")
            .setContentText("Your event starts in 30 minutes")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(NOTIF_ID_BASE + (eventId ?: "").hashCode(), builder.build())
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Event reminders"
            val descriptionText = "Alerts for upcoming scheduled events"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
