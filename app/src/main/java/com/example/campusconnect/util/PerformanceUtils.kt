package com.example.campusconnect.util

import android.util.Log
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring utilities.
 */
object PerformanceUtils {

    const val TAG = "Performance"
    const val SLOW_THRESHOLD_MS = 100L

    /**
     * Measure execution time of a block and log if slow.
     */
    inline fun <T> measureAndLog(
        operationName: String,
        threshold: Long = SLOW_THRESHOLD_MS,
        block: () -> T
    ): T {
        var result: T
        val timeMs = measureTimeMillis {
            result = block()
        }

        if (timeMs > threshold) {
            Log.w(TAG, "$operationName took ${timeMs}ms (threshold: ${threshold}ms)")
        } else {
            Log.d(TAG, "$operationName completed in ${timeMs}ms")
        }

        return result
    }

    /**
     * Measure and return execution time.
     */
    inline fun <T> measure(block: () -> T): Pair<T, Long> {
        var result: T
        val timeMs = measureTimeMillis {
            result = block()
        }
        return Pair(result, timeMs)
    }

    /**
     * Log memory usage.
     */
    fun logMemoryUsage(tag: String = TAG) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxMemory = runtime.maxMemory() / 1048576L
        val availableMemory = maxMemory - usedMemory

        Log.d(tag, "Memory: ${usedMemory}MB used, ${availableMemory}MB available, ${maxMemory}MB max")
    }

    /**
     * Suggest garbage collection if memory is low.
     */
    fun suggestGCIfNeeded() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val percentUsed = (usedMemory * 100) / maxMemory

        if (percentUsed > 80) {
            Log.w(TAG, "Memory usage at $percentUsed%, suggesting GC")
            System.gc()
        }
    }

    /**
     * Performance tracker for monitoring operations.
     */
    class PerformanceTracker(private val name: String) {
        private val startTime = System.currentTimeMillis()
        private val checkpoints = mutableMapOf<String, Long>()

        fun checkpoint(name: String) {
            checkpoints[name] = System.currentTimeMillis() - startTime
            Log.d(TAG, "[$this.name] Checkpoint '$name': ${checkpoints[name]}ms")
        }

        fun finish() {
            val totalTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "[$name] Total time: ${totalTime}ms")
            if (checkpoints.isNotEmpty()) {
                Log.d(TAG, "[$name] Checkpoints: $checkpoints")
            }
        }
    }
}

