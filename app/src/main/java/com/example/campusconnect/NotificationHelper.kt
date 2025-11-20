package com.example.campusconnect

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.campusconnect.data.models.OnlineEvent
import java.util.concurrent.TimeUnit

object NotificationHelper {
    private const val EVENTS_CHANNEL_ID = "events_channel"
    private const val MENTORSHIP_CHANNEL_ID = "mentorship_channel"

    fun scheduleEventReminder(context: Context, event: OnlineEvent) {
        val eventTime = event.dateTime?.toDate()?.time ?: return
        val reminderTime = eventTime - TimeUnit.MINUTES.toMillis(30)
        if (reminderTime <= System.currentTimeMillis()) return // already passed

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(NotificationReceiver.EXTRA_EVENT_TITLE, event.title)
            putExtra(NotificationReceiver.EXTRA_MEET_LINK, event.meetLink)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            (event.id).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Use setExactAndAllowWhileIdle for exact timing
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pending)
    }

    fun cancelEventReminder(context: Context, eventId: String) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, eventId.hashCode(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pending)
            pending.cancel()
        }
    }

    // New: show a simple notification for mentorship or generic use
    fun showSimpleNotification(context: Context, notifId: Int, title: String, text: String) {
        createMentorshipChannel(context)
        val builder = NotificationCompat.Builder(context, MENTORSHIP_CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_person_24)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notifId, builder.build())
        }
    }

    private fun createMentorshipChannel(context: Context) {
        val name = "Mentorship"
        val descriptionText = "Notifications for mentorship requests and updates"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(MENTORSHIP_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
