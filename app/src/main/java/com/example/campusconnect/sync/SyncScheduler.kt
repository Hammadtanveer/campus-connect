package com.example.campusconnect.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Scheduler for background sync operations.
 *
 * Sets up periodic WorkManager tasks for syncing data.
 */
object SyncScheduler {

    private const val TAG = "SyncScheduler"

    /**
     * Schedule periodic sync for all data.
     */
    fun schedulePeriodicSync(context: Context) {
        scheduleNotesSync(context)
        scheduleEventsSync(context)
        Log.d(TAG, "Periodic sync scheduled")
    }

    /**
     * Schedule notes sync every 15 minutes.
     */
    private fun scheduleNotesSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncNotesWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("sync")
            .addTag("notes")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SyncNotesWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

        Log.d(TAG, "Notes sync scheduled")
    }

    /**
     * Schedule events sync every 15 minutes.
     */
    private fun scheduleEventsSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncEventsWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("sync")
            .addTag("events")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SyncEventsWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

        Log.d(TAG, "Events sync scheduled")
    }

    /**
     * Cancel all sync work.
     */
    fun cancelAllSync(context: Context) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("sync")
        Log.d(TAG, "All sync work cancelled")
    }

    /**
     * Trigger immediate one-time sync.
     */
    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val notesSyncRequest = OneTimeWorkRequestBuilder<SyncNotesWorker>()
            .setConstraints(constraints)
            .build()

        val eventsSyncRequest = OneTimeWorkRequestBuilder<SyncEventsWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .beginWith(listOf(notesSyncRequest, eventsSyncRequest))
            .enqueue()

        Log.d(TAG, "Immediate sync triggered")
    }
}

