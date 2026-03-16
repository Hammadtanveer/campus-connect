package com.example.campusconnect.notifications

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    @SuppressLint("MissingPermission")
    @Suppress("UNUSED_PARAMETER")
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        type: String?,
        targetId: String?
    ) {
        val builder = NotificationCompat.Builder(context, AppNotificationChannels.EVENTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)

        val notificationId = System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}

