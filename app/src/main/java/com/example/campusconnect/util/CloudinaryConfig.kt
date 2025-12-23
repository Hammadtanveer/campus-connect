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
        } catch (e: Exception) {
            Log.e("CloudinaryConfig", "Error generating signature", e)
            ""
        }

        // Construct final URL
        // https://api.cloudinary.com/v1_1/<cloud_name>/image/download?...
        return "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/download?" +
               "$sortedParams&signature=$signature&api_key=$API_KEY"
    }
}
