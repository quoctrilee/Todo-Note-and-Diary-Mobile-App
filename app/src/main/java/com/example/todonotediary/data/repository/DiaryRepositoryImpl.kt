package com.example.todonotediary.data.repository

import android.util.Log
import com.example.todonotediary.data.local.DiaryDao
import com.example.todonotediary.data.remote.DiaryRemoteDataSource
import com.example.todonotediary.domain.model.DiaryEntity
import com.example.todonotediary.domain.repository.DiaryRepository
import com.example.todonotediary.utils.NetworkManager
import com.example.todonotediary.utils.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DiaryRepositoryImpl @Inject constructor(
    private val localDataSource: DiaryDao,
    private val remoteDataSource: DiaryRemoteDataSource,
    private val networkManager: NetworkManager,
    private val syncManager: SyncManager
) : DiaryRepository {

    // Cache last sync time to avoid too frequent syncs
    private var lastSyncTime = 0L
    private val syncIntervalMs = 30_000L // 30 seconds

    private fun shouldSync(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastSyncTime) > syncIntervalMs
    }

    override fun getDiaries(userId: String): Flow<List<DiaryEntity>> {
        return flow {
            // Trigger sync from remote first if online and not synced recently
            if (networkManager.isCurrentlyOnline() && shouldSync()) {
                try {
                    syncFromRemote(userId)
                    lastSyncTime = System.currentTimeMillis()
                    Log.d("DiaryRepository", "Synced diaries from Firebase before emitting")
                } catch (e: Exception) {
                    Log.e("DiaryRepository", "Failed to sync from remote, continuing with local data", e)
                }
            }
            
            // Emit from local database (which now has updated data)
            localDataSource.getDiariesFlow(userId)
                .collect { diaries ->
                    emit(diaries)
                }
        }.catch { exception ->
            Log.e("DiaryRepository", "Error loading diaries", exception)
            emit(emptyList())
        }
    }

    override suspend fun getDiariesByDate(userId: String, date: Long): Flow<List<DiaryEntity>> {
        // Pure offline-first: filter local data only
        return flow {
            localDataSource.getDiariesFlow(userId)
                .collect { diaries ->
                    val filteredDiaries = diaries.filter { diary ->
                        isSameDay(diary.date, date)
                    }
                    emit(filteredDiaries)
                }
        }.catch { exception ->
            Log.e("DiaryRepository", "Error loading diaries by date", exception)
            emit(emptyList())
        }
    }

    override suspend fun getDiariesByTitleOrContent(userId: String, text: String): Flow<List<DiaryEntity>> {
        return flow {
            // 1. Search in local first
            val localDiaries = localDataSource.getDiaries(userId)
            val searchResults = localDiaries.filter { diary ->
                diary.title.contains(text, ignoreCase = true) ||
                        diary.content.contains(text, ignoreCase = true)
            }
            emit(searchResults)

            // 2. Try remote search
            try {
                val remoteResults = remoteDataSource.searchDiaries(userId, text)
                remoteResults.forEach { diary ->
                    localDataSource.insertDiary(diary)
                }
                emit(remoteResults)
            } catch (e: Exception) {
                Log.e("DiaryRepository", "Error searching remote diaries", e)
            }
        }.catch { exception ->
            Log.e("DiaryRepository", "Error searching diaries", exception)
            emit(emptyList())
        }
    }

    override suspend fun getDiaryById(diaryId: String): DiaryEntity? {
        // Try local first
        val localDiary = localDataSource.getDiaryById(diaryId)
        if (localDiary != null) return localDiary

        // Fallback to remote
        return try {
            remoteDataSource.getDiaryById(diaryId)
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error getting diary by id", e)
            null
        }
    }

    override suspend fun addDiary(diary: DiaryEntity): Result<DiaryEntity> {
        return try {
            // 1. Save to local first
            localDataSource.insertDiary(diary)

            // 2. Sync to remote only if online
            if (networkManager.isCurrentlyOnline()) {
                val remoteResult = remoteDataSource.saveDiary(diary)
                if (remoteResult.isSuccess) {
                    // Reset sync cache to force refresh next time
                    lastSyncTime = 0L
                    Result.success(diary)
                } else {
                    // Keep in local, schedule sync for later
                    syncManager.scheduleSyncNow()
                    Result.success(diary)
                }
            } else {
                // Offline: schedule sync when network returns
                syncManager.schedulePeriodicSync()
                Result.success(diary)
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error adding diary", e)
            Result.failure(e)
        }
    }

    override suspend fun updateDiary(diary: DiaryEntity): Result<Unit> {
        return try {
            // 1. Update local first
            val updatedDiary = diary.copy(
                updatedAt = System.currentTimeMillis(),
                lastSyncTimestamp = 0L // Mark as pending sync
            )
            localDataSource.updateDiary(updatedDiary)

            // 2. Sync to remote only if online
            if (networkManager.isCurrentlyOnline()) {
                val remoteResult = remoteDataSource.saveDiary(updatedDiary)
                if (remoteResult.isSuccess) {
                    // Reset sync cache to force refresh next time
                    lastSyncTime = 0L
                    Result.success(Unit)
                } else {
                    // Keep local changes, schedule sync for later
                    syncManager.scheduleSyncNow()
                    Result.success(Unit)
                }
            } else {
                // Offline: schedule sync when network returns
                syncManager.schedulePeriodicSync()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error updating diary", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteDiary(diaryId: String): Result<Unit> {
        return try {
            // 1. Mark as pending delete in local
            val localDiary = localDataSource.getDiaryById(diaryId)
            if (localDiary != null) {
                val deletedDiary = localDiary.copy(
                    pendingDelete = true,
                    updatedAt = System.currentTimeMillis()
                )
                localDataSource.updateDiary(deletedDiary)
                Log.d("DiaryRepository", "Marked diary as pending delete: $diaryId")
            }

            // 2. Try to delete from remote if online
            if (networkManager.isCurrentlyOnline()) {
                val remoteResult = remoteDataSource.deleteDiary(diaryId)
                if (remoteResult.isSuccess) {
                    // Successfully deleted from remote, now hard delete locally
                    if (localDiary != null) {
                        val fullyDeletedDiary = localDiary.copy(
                            isDeleted = true,
                            pendingDelete = false
                        )
                        localDataSource.updateDiary(fullyDeletedDiary)
                    }
                    Result.success(Unit)
                } else {
                    // Keep as pending delete, schedule sync
                    syncManager.scheduleSyncNow()
                    Result.success(Unit)
                }
            } else {
                // Offline: keep as pending delete, schedule sync when network returns
                syncManager.schedulePeriodicSync()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error deleting diary", e)
            Result.failure(e)
        }
    }

    override suspend fun syncDiaries(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        return try {
            // Get diaries that need to be synced (modified after last sync)
            val diariesToSync = localDataSource.getDiariesToSync(userId)
            
            // Sync each diary to remote
            diariesToSync.forEach { diary ->
                remoteDataSource.saveDiary(diary)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error syncing diaries", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFromRemote(userId: String): Result<Unit> {
        return try {
            if (!networkManager.isCurrentlyOnline()) {
                Log.d("DiaryRepository", "No network, skipping remote sync")
                return Result.success(Unit)
            }

            // 1. Fetch all diaries from Firebase
            val remoteResult = remoteDataSource.getDiaries(userId)
            if (remoteResult.isFailure) {
                Log.e("DiaryRepository", "Failed to fetch diaries from Firebase", remoteResult.exceptionOrNull())
                return Result.failure(remoteResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val remoteDiaries = remoteResult.getOrNull() ?: emptyList()
            Log.d("DiaryRepository", "Fetched ${remoteDiaries.size} diaries from Firebase")

            // 2. Get all local diaries
            val localDiaries = localDataSource.getAllDiaries()
            val userLocalDiaries = localDiaries.filter { it.userId == userId }
            
            // 3. Merge logic: Firebase is source of truth for synced data
            remoteDiaries.forEach { remoteDiary ->
                val localDiary = userLocalDiaries.find { it.id == remoteDiary.id }
                
                if (localDiary == null) {
                    // New diary from Firebase - insert to local
                    localDataSource.insertDiary(remoteDiary)
                    Log.d("DiaryRepository", "Inserted new diary from Firebase: ${remoteDiary.id}")
                } else {
                    // Diary exists locally - check if we should update
                    // Only update if remote is newer AND local doesn't have pending changes
                    val hasLocalPendingChanges = localDiary.lastSyncTimestamp == 0L || localDiary.pendingDelete
                    
                    if (!hasLocalPendingChanges && remoteDiary.updatedAt > localDiary.updatedAt) {
                        // Remote is newer and local has no pending changes - update local
                        localDataSource.insertDiary(remoteDiary)
                        Log.d("DiaryRepository", "Updated diary from Firebase: ${remoteDiary.id}")
                    } else if (hasLocalPendingChanges) {
                        Log.d("DiaryRepository", "Skipping update for diary ${remoteDiary.id} - has pending local changes")
                    }
                }
            }

            Log.d("DiaryRepository", "Sync from remote completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error syncing from remote", e)
            Result.failure(e)
        }
    }

    // Helper function to check if two dates are on the same day
    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = date2 }
        
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
