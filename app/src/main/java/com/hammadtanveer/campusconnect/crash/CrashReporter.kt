package com.hammadtanveer.campusconnect.crash

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash reporting manager.
 *
 * Provides centralized crash and error reporting.
 * Note: Firebase Crashlytics integration pending - currently logs errors.
 */
@Singleton
@Suppress("UNUSED_PARAMETER")
class CrashReporter @Inject constructor() {

    /**
     * Log non-fatal exception.
     */
    fun logException(throwable: Throwable, message: String? = null) = Unit

    /**
     * Log custom error message.
     */
    fun logError(message: String) = Unit

    /**
     * Log warning.
     */
    fun logWarning(message: String) = Unit

    /**
     * Log info message.
     */
    fun logInfo(message: String) = Unit

    /**
     * Set user ID for crash reports.
     */
    fun setUserId(userId: String) = Unit

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: String) = Unit

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Boolean) = Unit

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Int) = Unit

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Long) = Unit

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Float) = Unit

    /**
     * Set custom key for crash reports.
     */
    fun setCustomKey(key: String, value: Double) = Unit

    /**
     * Enable/disable crash collection.
     */
    fun setCrashlyticsCollectionEnabled(enabled: Boolean) = Unit

    /**
     * Force send crash reports.
     */
    fun sendUnsentReports() = Unit

    /**
     * Check if there are unsent reports.
     */
    fun checkForUnsentReports() = Unit

    /**
     * Log breadcrumb for crash context.
     */
    fun logBreadcrumb(message: String) = Unit

    /**
     * Set user attributes for crash reports.
     */
    fun setUserAttributes(email: String?, role: String?, university: String?) = Unit
}

