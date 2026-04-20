package com.hammadtanveer.campusconnect.notifications

import com.hammadtanveer.campusconnect.util.AppLogger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AppFcmService"
    }

    override fun onCreate() {
        super.onCreate()
        AppNotificationChannels.createAll(this)
        AppLogger.d(TAG, "FCM service created and notification channels verified")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            AppLogger.d(TAG, "FCM message received")

            val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title
            val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body
            val type = remoteMessage.data["type"]
            val targetId = remoteMessage.data["targetId"]
            val route = NotificationIntentRouter.resolveRoute(type)

            AppLogger.d(TAG, "Notification payload parsed")

            if (title.isNullOrBlank() && body.isNullOrBlank()) {
                AppLogger.d(TAG, "Received FCM without displayable payload")
                return
            }

            NotificationHelper.showNotification(
                context = this,
                title = title ?: "CampusConnect",
                body = body ?: "",
                type = type,
                targetId = targetId
            )
            AppLogger.d(TAG, "Notification displayed successfully")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed while handling FCM message", e)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AppLogger.d(TAG, "onNewToken received")
        NotificationSubscriptionManager.subscribeAllTopics()
    }
}
