package com.hammadtanveer.campusconnect.util

import android.content.Context
import com.cloudinary.android.MediaManager

object CloudinaryConfig {
    private val CLOUD_NAME get() = com.hammadtanveer.campusconnect.BuildConfig.CLOUDINARY_CLOUD_NAME
    private val UPLOAD_PRESET get() = com.hammadtanveer.campusconnect.BuildConfig.CLOUDINARY_UPLOAD_PRESET
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val config = mapOf(
                "cloud_name" to CLOUD_NAME,
                "secure" to true
            )
            MediaManager.init(context, config)
            isInitialized = true
        } catch (_: Exception) { }
    }

    fun isConfigured(): Boolean = CLOUD_NAME.isNotBlank() && UPLOAD_PRESET.isNotBlank()

    fun getUploadPreset(): String = UPLOAD_PRESET

    fun getCloudName(): String = CLOUD_NAME
}
