package com.example.campusconnect

import android.app.Application
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

        FirebaseApp.getApps(this)

        initializeFcm()
        FirestoreAutoNotificationManager.start(this)

        // Schedule background sync
        scheduleSyncWork()

    }

    private fun scheduleSyncWork() {
        try {
            SyncScheduler.schedulePeriodicSync(this)
        } catch (e: Exception) {
            Unit
        }
    }

    private fun initializeFcm() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }

            NotificationSubscriptionManager.subscribeAllTopics()
        }
    }
}


