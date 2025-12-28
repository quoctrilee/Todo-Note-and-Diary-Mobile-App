package com.example.todonotediary.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.todonotediary.worker.SyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val context: Context,
    private val networkManager: NetworkManager
) {
    private val workManager = WorkManager.getInstance(context)
    private var lastSyncRequestTime = 0L

    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_WORK_TAG = "sync_work"
        private const val DEBOUNCE_DELAY_MS = 500L // Debounce 500ms
    }

    init {
        // Auto-sync when network becomes available
        networkManager.setOnNetworkAvailableCallback {
            Log.d(TAG, "Network available, triggering sync")
            scheduleSyncNow()
        }
    }

    /**
     * Schedule one-time sync immediately
     */
    fun scheduleSyncNow() {
        // Debounce: Ignore if called within 500ms of last request
        val now = System.currentTimeMillis()
        if (now - lastSyncRequestTime < DEBOUNCE_DELAY_MS) {
            Log.d(TAG, "Sync request debounced (too soon)")
            return
        }
        lastSyncRequestTime = now

        if (!networkManager.isCurrentlyOnline()) {
            Log.d(TAG, "Device offline, scheduling sync for when network returns")
            schedulePeriodicSync()
            return
        }

        Log.d(TAG, "Scheduling immediate sync")
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(getConstraints())
            .addTag(SYNC_WORK_TAG)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP, // Changed from REPLACE to KEEP
            syncRequest
        )
    }

    /**
     * Schedule periodic sync (every 15 minutes when constraints are met)
     */
    fun schedulePeriodicSync() {
        Log.d(TAG, "Scheduling periodic sync")
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(getConstraints())
            .addTag(SYNC_WORK_TAG)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "${SyncWorker.WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Cancel all sync work
     */
    fun cancelAllSync() {
        Log.d(TAG, "Cancelling all sync work")
        workManager.cancelAllWorkByTag(SYNC_WORK_TAG)
    }

    /**
     * Get constraints for sync work
     * Only run when network is available
     */
    private fun getConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    /**
     * Trigger sync when network state changes from offline to online
     */
    fun onNetworkAvailable() {
        Log.d(TAG, "Network available, triggering sync")
        scheduleSyncNow()
    }
}
