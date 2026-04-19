package com.hammadtanveer.campusconnect.notifications

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    private const val TAG = "NotificationHelper"

    @SuppressLint("MissingPermission")
    @Suppress("UNUSED_PARAMETER")
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        type: String?,
        targetId: String?,
        parentId: String? = null
    ) {
        AppNotificationChannels.create(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Log.d(TAG, "Notification skipped: POST_NOTIFICATIONS not granted")
                return
            }
        }

        val channelId = when (type?.trim()?.lowercase()) {
            "notes" -> AppNotificationChannels.NOTES
            "placements" -> AppNotificationChannels.JOBS
            "society", "society_updates" -> AppNotificationChannels.SOCIETY
            "meetings", "announcements" -> AppNotificationChannels.EVENTS
            else -> AppNotificationChannels.EVENTS
        }

        val contentIntent = NotificationIntentRouter.createPendingIntent(
            context = context,
            type = type,
            targetId = targetId,
            parentId = parentId
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        val notificationId = System.currentTimeMillis().toInt()
        Log.d(
            TAG,
            "Showing notification id=$notificationId, type=$type, targetId=$targetId, parentId=$parentId"
        )
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}

