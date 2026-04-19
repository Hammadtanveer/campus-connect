package com.hammadtanveer.campusconnect.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Scheduler for background sync operations.
 *
 * Sets up periodic WorkManager tasks for syncing data.
 */
object SyncScheduler {

    /**
     * Schedule periodic sync for all data.
     */
    fun schedulePeriodicSync(context: Context) {
        scheduleNotesSync(context)
        scheduleEventsSync(context)
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
    }

    /**
     * Cancel all sync work.
     */
    fun cancelAllSync(context: Context) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("sync")
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
    }
}

