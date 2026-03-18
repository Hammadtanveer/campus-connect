package com.example.campusconnect

import android.app.Application
import android.util.Log
import com.example.campusconnect.notifications.AppNotificationChannels
import com.example.campusconnect.notifications.FirestoreAutoNotificationManager
import com.example.campusconnect.notifications.NotificationSubscriptionManager
import com.example.campusconnect.sync.SyncScheduler
import com.example.campusconnect.util.CloudinaryConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CampusConnectApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Cloudinary
        CloudinaryConfig.initialize(this)

        AppNotificationChannels.createAll(this)
        Log.d("CampusConnectApp", "Notification channels initialized")

        val firebaseApps = FirebaseApp.getApps(this)
        Log.d("CampusConnectApp", "Firebase initialized apps count=${firebaseApps.size}")

        initializeFcm()
        FirestoreAutoNotificationManager.start(this)

        // Schedule background sync
        scheduleSyncWork()

        Log.d("CampusConnectApp", "Application initialized successfully")
    }

    private fun scheduleSyncWork() {
        try {
            SyncScheduler.schedulePeriodicSync(this)
        } catch (e: Exception) {
            Log.e("CampusConnectApp", "Failed to schedule sync work", e)
        }
    }

    private fun initializeFcm() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e("CampusConnectApp", "Failed to fetch FCM token", task.exception)
                Log.e("FCM", "Token fetch failed", task.exception)
                return@addOnCompleteListener
            }

            Log.d("CampusConnectApp", "FCM token=${task.result}")
            Log.d("FCM", "Current token: ${task.result}")
            NotificationSubscriptionManager.subscribeAllTopics()
        }
    }
}


