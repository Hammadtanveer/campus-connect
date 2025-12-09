package com.example.campusconnect.crash

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash reporting manager.
 *
 * Provides centralized crash and error reporting.
 * Note: Firebase Crashlytics integration pending - currently logs errors.
 */
@Singleton
class CrashReporter @Inject constructor() {

    companion object {
        private const val TAG = "CrashReporter"
    }

    /**
     * Log non-fatal exception.
     */
    fun logException(throwable: Throwable, message: String? = null) {
        val logMessage = message ?: throwable.message ?: "Unknown exception"
        Log.e(TAG, "Exception logged: $logMessage", throwable)
    }

    /**
     * Log custom error message.
     */
    fun logError(message: String) {
        Log.e(TAG, "ERROR: $message")
    }

    /**
     * Log warning.
     */
    fun logWarning(message: String) {
        Log.w(TAG, "WARNING: $message")
    }

    /**
     * Log info message.
     */
    fun logInfo(message: String) {
        Log.d(TAG, "INFO: $message")
    }

    /**
     * Set user ID for crash reports.
     */
    fun setUserId(userId: String) {
        Log.d(TAG, "User ID set: $userId")
    }

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: String) {
        Log.d(TAG, "Custom key: $key = $value")
    }

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Boolean) {
        Log.d(TAG, "Custom key: $key = $value")
    }

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Int) {
        Log.d(TAG, "Custom key: $key = $value")
    }

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Long) {
        Log.d(TAG, "Custom key: $key = $value")
    }

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Float) {
        Log.d(TAG, "Custom key: $key = $value")
    }

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Double) {
        Log.d(TAG, "Custom key: $key = $value")
    }

    /**
     * Enable/disable crash collection.
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        Log.d(TAG, "Crashlytics collection enabled: $enabled")
    }

    /**
     * Force send crash reports.
     */
    fun sendUnsentReports() {
        Log.d(TAG, "Sending unsent reports...")
    }

    /**
     * Check if there are unsent reports.
     */
    fun checkForUnsentReports() {
        Log.d(TAG, "Checking for unsent reports...")
    }

    /**
     * Log breadcrumb for crash context.
     */
    fun logBreadcrumb(message: String) {
        Log.d(TAG, "Breadcrumb: $message")
    }

    /**
     * Set user attributes for crash reports.
     */
    fun setUserAttributes(email: String?, role: String?, university: String?) {
        email?.let { Log.d(TAG, "User email: $it") }
        role?.let { Log.d(TAG, "User role: $it") }
        university?.let { Log.d(TAG, "University: $it") }
    }
}

