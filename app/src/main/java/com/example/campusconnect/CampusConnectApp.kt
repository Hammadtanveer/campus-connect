package com.example.campusconnect

import android.app.Application
import com.example.campusconnect.util.CloudinaryConfig

class CampusConnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Cloudinary
        CloudinaryConfig.initialize(this)
    }
}


