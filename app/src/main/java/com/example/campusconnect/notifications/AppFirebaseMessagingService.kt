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
        Log.d(TAG, "onMessageReceived data=${remoteMessage.data}")

        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]
        val type = remoteMessage.data["type"]
        val targetId = remoteMessage.data["targetId"]

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
    }
}
