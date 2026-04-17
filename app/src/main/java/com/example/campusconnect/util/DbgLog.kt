package com.example.campusconnect.util

import android.os.SystemClock
import android.util.Log

object DbgLog {
    private const val base = "SeniorFeature"
    val sessionId: String = System.currentTimeMillis().toString()
    private val t0 = SystemClock.elapsedRealtime()

    fun d(tag: String, msg: String) {
        val dt = SystemClock.elapsedRealtime() - t0
        Log.d("$base-$tag", "[sid=$sessionId][+${dt}ms][${Thread.currentThread().name}] $msg")
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        val dt = SystemClock.elapsedRealtime() - t0
        Log.e("$base-$tag", "[sid=$sessionId][+${dt}ms][${Thread.currentThread().name}] $msg", tr)
    }
}

