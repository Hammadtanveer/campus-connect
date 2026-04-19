package com.hammadtanveer.campusconnect

import android.app.Application
import com.hammadtanveer.campusconnect.notifications.AppNotificationChannels
import com.hammadtanveer.campusconnect.notifications.FirestoreAutoNotificationManager
import com.hammadtanveer.campusconnect.notifications.NotificationSubscriptionManager
import com.hammadtanveer.campusconnect.sync.SyncScheduler
import com.hammadtanveer.campusconnect.util.CloudinaryConfig
import com.hammadtanveer.campusconnect.util.DbgLog
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CampusConnectApp : Application() {

    override fun onCreate() {
        DbgLog.d("App", "onCreate start")
        super.onCreate()

        // Initialize Cloudinary
        DbgLog.d("App", "cloudinary init start")
        CloudinaryConfig.initialize(this)
        DbgLog.d("App", "cloudinary init done")

        DbgLog.d("App", "notification channels init")
        AppNotificationChannels.createAll(this)

        DbgLog.d("App", "FirebaseApp.getApps")
        FirebaseApp.getApps(this)

        DbgLog.d("App", "initializeFcm start")
        initializeFcm()
        DbgLog.d("App", "FirestoreAutoNotificationManager start")
        FirestoreAutoNotificationManager.start(this)

        // Schedule background sync
        DbgLog.d("App", "scheduleSyncWork start")
        scheduleSyncWork()
        DbgLog.d("App", "onCreate end")

    }

    private fun scheduleSyncWork() {
        try {
            SyncScheduler.schedulePeriodicSync(this)
            DbgLog.d("App", "scheduleSyncWork success")
        } catch (e: Exception) {
            DbgLog.e("App", "scheduleSyncWork failed", e)
            Unit
        }
    }

    private fun initializeFcm() {
        DbgLog.d("App", "initializeFcm token request start")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                DbgLog.e("App", "initializeFcm token request failed", task.exception)
                return@addOnCompleteListener
            }

            DbgLog.d("App", "initializeFcm token request success, subscribe topics")
            NotificationSubscriptionManager.subscribeAllTopics()
        }
    }
}


