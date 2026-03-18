package com.example.campusconnect.util

import kotlin.system.measureTimeMillis

/**
 * Performance monitoring utilities.
 */
object PerformanceUtils {
    const val SLOW_THRESHOLD_MS = 100L

    /**
     * Measure execution time of a block and log if slow.
     */
    @Suppress("UNUSED_PARAMETER")
    inline fun <T> measureAndLog(
        operationName: String,
        threshold: Long = SLOW_THRESHOLD_MS,
        block: () -> T
    ): T = measure(block).first

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
    @Suppress("UNUSED_PARAMETER")
    fun logMemoryUsage(tag: String = "Performance") {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxMemory = runtime.maxMemory() / 1048576L
        val availableMemory = maxMemory - usedMemory

        @Suppress("UNUSED_VARIABLE")
        val memorySnapshot = "$usedMemory/$availableMemory/$maxMemory"
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
        }

        fun finish() {
            @Suppress("UNUSED_VARIABLE")
            val totalTime = System.currentTimeMillis() - startTime
        }
    }
}

