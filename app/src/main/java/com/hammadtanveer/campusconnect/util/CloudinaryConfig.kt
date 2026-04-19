package com.hammadtanveer.campusconnect.util

import android.content.Context
import com.cloudinary.android.MediaManager

object CloudinaryConfig {

    private val CLOUD_NAME get() = com.hammadtanveer.campusconnect.BuildConfig.CLOUDINARY_CLOUD_NAME
    private val API_KEY get() = com.hammadtanveer.campusconnect.BuildConfig.CLOUDINARY_API_KEY
    private val API_SECRET get() = com.hammadtanveer.campusconnect.BuildConfig.CLOUDINARY_API_SECRET

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) {
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
        } catch (_: Exception) { }
    }

    fun isConfigured(): Boolean {
        return CLOUD_NAME != "your-cloud-name" &&
               API_KEY != "your-api-key" &&
               API_SECRET != "your-api-secret"
    }

    /**
     * Generates a signed URL for accessing authenticated PDFs.
     * This uses the Cloudinary API Download endpoint which works reliably for authenticated assets.
     *
     * Note: This requires API_SECRET to be present in the app.
     */
    fun getSignedPdfUrl(publicId: String): String {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val expiresAt = ((System.currentTimeMillis() / 1000) + 3600).toString() // 1 hour validity

        // Parameters to sign (must be sorted alphabetically by key)
        val params = java.util.TreeMap<String, String>()
        params["attachment"] = "false"
        params["expires_at"] = expiresAt
        params["format"] = "pdf"
        params["public_id"] = publicId
        params["timestamp"] = timestamp
        params["type"] = "authenticated"

        // Create signature string: key=value&key=value... + API_SECRET
        val sortedParams = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        val toSign = "$sortedParams$API_SECRET"

        // SHA-1 Hash
        val signature = try {
            val digest = java.security.MessageDigest.getInstance("SHA-1")
            val bytes = digest.digest(toSign.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) { "" }

        // Construct final URL
        // https://api.cloudinary.com/v1_1/<cloud_name>/image/download?...
        return "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/download?" +
               "$sortedParams&signature=$signature&api_key=$API_KEY"
    }
}
