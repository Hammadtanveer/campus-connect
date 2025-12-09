package com.example.campusconnect.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.campusconnect.data.repository.EventsRepository
import com.example.campusconnect.network.ConnectivityManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Background worker for syncing events with Firestore.
 *
 * Runs periodically to sync local Room database with remote Firestore.
 */
@HiltWorker
class SyncEventsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val eventsRepo: EventsRepository,
    private val connectivityManager: ConnectivityManager
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "SyncEventsWorker"
        const val WORK_NAME = "sync_events_periodic"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting events sync...")

        return try {
            // Check network connectivity
            if (!connectivityManager.isNetworkAvailable()) {
                Log.w(TAG, "No network available, will retry later")
                return Result.retry()
            }

            // Observe events (this will trigger caching)
            val result = eventsRepo.observeEvents().first()

            if (result is com.example.campusconnect.data.models.Resource.Success) {
                Log.d(TAG, "Events sync completed successfully")
                Result.success()
            } else {
                Log.e(TAG, "Events sync failed")

                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during events sync", e)

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

