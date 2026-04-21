package com.hammadtanveer.campusconnect.util

import android.content.Context
import com.cloudinary.android.MediaManager
import com.hammadtanveer.campusconnect.BuildConfig

object CloudinaryConfig {

    fun initialize(context: Context) {
        try {
            MediaManager.get()
            return // Already initialized, skip
        } catch (e: IllegalStateException) {
            val config = mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET,
                "secure" to true
            )
            MediaManager.init(context, config)
        }
    }

    fun isConfigured(): Boolean {
        return BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank() &&
                BuildConfig.CLOUDINARY_CLOUD_NAME != "null" &&
                BuildConfig.CLOUDINARY_API_KEY.isNotBlank() &&
                BuildConfig.CLOUDINARY_API_KEY != "null" &&
                BuildConfig.CLOUDINARY_API_SECRET.isNotBlank() &&
                BuildConfig.CLOUDINARY_API_SECRET != "null"
    }

    fun getUploadPreset(): String = BuildConfig.CLOUDINARY_UPLOAD_PRESET

    fun getCloudName(): String = BuildConfig.CLOUDINARY_CLOUD_NAME
}
