package com.hammadtanveer.campusconnect.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hammadtanveer.campusconnect.data.repository.EventsRepository
import com.hammadtanveer.campusconnect.network.ConnectivityManager
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
        return try {
            // Check network connectivity
            if (!connectivityManager.isNetworkAvailable()) {
                return Result.retry()
            }

            // Observe events (this will trigger caching)
            val result = eventsRepo.observeEvents().first()

            if (result is com.hammadtanveer.campusconnect.data.models.Resource.Success) {
                Result.success()
            } else {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (_: Exception) {

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

