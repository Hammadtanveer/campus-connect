package com.hammadtanveer.campusconnect.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object NotificationSubscriptionManager {
    private const val TAG = "NotificationSubMgr"

    fun subscribeAllTopics() {
        val topics = NotificationTopics.ALL

        Log.d(TAG, "Subscribing to topics=${topics.joinToString()}")

        topics.forEach { topic ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Subscribed to topic: $topic")
                    } else {
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        Log.e(TAG, "Failed to subscribe to topic: $topic. Error: $errorMessage")
                    }
                }
        }
    }
}

