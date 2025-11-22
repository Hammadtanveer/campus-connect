package com.example.campusconnect.util

import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager

object CloudinaryConfig {

    // TODO: Replace these with your actual Cloudinary credentials
    // Get them from: https://console.cloudinary.com/
    // For security, consider storing these in local.properties or Firebase Remote Config
    private const val CLOUD_NAME = "dkxunmucg"
    private const val API_KEY = "492784632542267"
    private const val API_SECRET = "3CSXo-IjIxXX6qy-CTo-9bBSunU"

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d("CloudinaryConfig", "Cloudinary already initialized")
            return
        }

        try {
            val config = mapOf(
                "cloud_name" to CLOUD_NAME,
                "api_key" to API_KEY,
                "api_secret" to API_SECRET,
                "secure" to true
            )

            MediaManager.init(context, config)
            isInitialized = true
            Log.d("CloudinaryConfig", "Cloudinary initialized successfully")
        } catch (e: Exception) {
            Log.e("CloudinaryConfig", "Failed to initialize Cloudinary", e)
        }
    }

    fun isConfigured(): Boolean {
        return CLOUD_NAME != "your-cloud-name" &&
               API_KEY != "your-api-key" &&
               API_SECRET != "your-api-secret"
    }
}

