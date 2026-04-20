package com.hammadtanveer.campusconnect.util
import android.util.Log
import com.hammadtanveer.campusconnect.BuildConfig

object AppLogger {
    private const val MAX_MESSAGE_LENGTH = 2000
    fun d(tag: String, message: String) { if (BuildConfig.DEBUG) Log.d(tag, sanitize(message)) }
    fun w(tag: String, message: String, throwable: Throwable? = null) { if (BuildConfig.DEBUG) Log.w(tag, sanitize(message), throwable) }
    fun e(tag: String, message: String, throwable: Throwable? = null) { if (BuildConfig.DEBUG) Log.e(tag, sanitize(message), throwable) }
    private fun sanitize(message: String) = message.replace("\n", " ").take(MAX_MESSAGE_LENGTH)
}


