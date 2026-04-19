package com.hammadtanveer.campusconnect.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hammadtanveer.campusconnect.data.repository.NotesRepository
import com.hammadtanveer.campusconnect.network.ConnectivityManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker for syncing notes with Firestore.
 *
 * Runs periodically to sync local Room database with remote Firestore.
 */
@HiltWorker
class SyncNotesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notesRepo: NotesRepository,
    private val connectivityManager: ConnectivityManager
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "SyncNotesWorker"
        const val WORK_NAME = "sync_notes_periodic"
    }

    override suspend fun doWork(): Result {
        return try {
            // Check network connectivity
            if (!connectivityManager.isNetworkAvailable()) {
                return Result.retry()
            }

            // Perform sync (syncNotes catches its own exceptions and returns Unit)
            notesRepo.syncNotes()

            Result.success()
        } catch (_: Exception) {

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

