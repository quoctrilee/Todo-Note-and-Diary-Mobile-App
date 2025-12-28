package com.example.todonotediary

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.todonotediary.utils.NetworkManager
import com.example.todonotediary.utils.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TodoNoteDiaryApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var networkManager: NetworkManager

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        
        // Schedule periodic sync on app start
        syncManager.schedulePeriodicSync()
        
        // Observe network changes and trigger sync when online
        observeNetworkChanges()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun observeNetworkChanges() {
        // Network state changes are handled by NetworkManager
        // SyncManager will be triggered when network becomes available
    }
}