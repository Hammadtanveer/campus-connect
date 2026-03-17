package com.example.campusconnect.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AppFcmService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_DEBUG", "Message received: ${remoteMessage.data}")
        Log.d(
            TAG,
            "onMessageReceived from=${remoteMessage.from}, messageId=${remoteMessage.messageId}, data=${remoteMessage.data}"
        )

        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body
        val type = remoteMessage.data["type"]
        val targetId = remoteMessage.data["targetId"]
        val route = NotificationIntentRouter.resolveRoute(type)

        Log.d(TAG, "Notification routing type=$type route=$route targetId=$targetId")

        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            Log.d(TAG, "Received FCM without displayable payload")
            return
        }

        NotificationHelper.showNotification(
            context = this,
            title = title ?: "CampusConnect",
            body = body ?: "",
            type = type,
            targetId = targetId
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken token=$token")
        NotificationSubscriptionManager.subscribeAllTopics()
    }
}
