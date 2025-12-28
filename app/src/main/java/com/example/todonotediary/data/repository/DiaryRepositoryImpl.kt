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

    override fun getDiaries(userId: String): Flow<List<DiaryEntity>> {
        // Pure offline-first: only emit from local database
        // Background sync via SyncWorker will keep data updated
        return localDataSource.getDiariesFlow(userId)
            .catch { exception ->
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
            val updatedDiary = diary.copy(updatedAt = System.currentTimeMillis())
            localDataSource.updateDiary(updatedDiary)

            // 2. Sync to remote
            val remoteResult = remoteDataSource.saveDiary(updatedDiary)
            if (remoteResult.isSuccess) {
                Result.success(Unit)
            } else {
                // Keep local changes even if remote fails
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("DiaryRepository", "Error updating diary", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteDiary(diaryId: String): Result<Unit> {
        return try {
            // 1. Soft delete in local
            val localDiary = localDataSource.getDiaryById(diaryId)
            if (localDiary != null) {
                val deletedDiary = localDiary.copy(
                    isDeleted = true,
                    updatedAt = System.currentTimeMillis()
                )
                localDataSource.updateDiary(deletedDiary)
            }

            // 2. Delete from remote
            val remoteResult = remoteDataSource.deleteDiary(diaryId)
            if (remoteResult.isSuccess) {
                Result.success(Unit)
            } else {
                // Keep local deletion even if remote fails
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

    // Helper function to check if two dates are on the same day
    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = date2 }
        
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
