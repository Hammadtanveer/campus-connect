package com.example.campusconnect

import android.app.Application
import android.util.Log
import com.example.campusconnect.sync.SyncScheduler
import com.example.campusconnect.util.CloudinaryConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CampusConnectApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Cloudinary
        CloudinaryConfig.initialize(this)

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
}


