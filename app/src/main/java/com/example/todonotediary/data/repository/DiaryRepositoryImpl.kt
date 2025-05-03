package com.example.todonotediary.data.repository

import com.example.todonotediary.data.local.DiaryDao
import com.example.todonotediary.data.remote.DiaryRemoteDataSource
import com.example.todonotediary.domain.model.DiaryEntity
import com.example.todonotediary.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class DiaryRepositoryImpl(private val diaryDao: DiaryDao, private val remoteDataSource: DiaryRemoteDataSource) : DiaryRepository {

    override fun getDiaries(userId: String): Flow<List<DiaryEntity>> {
        return flow {
            emit(diaryDao.getDiaries(userId))
        }.catch { exception ->
            emit(emptyList())
        }
    }

    override suspend fun getDiaryById(diaryId: String): DiaryEntity? {
        return diaryDao.getDiaryById(diaryId)
    }

    override suspend fun addDiary(diary: DiaryEntity): Result<DiaryEntity> {
        return try {
            diaryDao.insertDiary(diary)
            Result.success(diary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDiary(diary: DiaryEntity): Result<Unit> {
        return try {
            val updated = diary.copy(updatedAt = System.currentTimeMillis())
            diaryDao.updateDiary(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDiary(diaryId: String): Result<Unit> {
        return try {
            val diary = diaryDao.getDiaryById(diaryId)
                ?: return Result.failure(Exception("Diary not found"))

            val softDeleted = diary.copy(updatedAt = System.currentTimeMillis(), isDeleted = true, lastSyncTimestamp = System.currentTimeMillis())
            diaryDao.updateDiary(softDeleted)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun syncDiaries(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        return try {
            // 1. Đẩy dữ liệu local chưa đồng bộ lên Firestore
            val localDiariesToSync = diaryDao.getDiariesToSync(userId)
            for (diary in localDiariesToSync) {
                val syncedDiary = remoteDataSource.saveDiary(diary).getOrNull()
                if (syncedDiary != null) {
                    diaryDao.insertDiary(syncedDiary.copy(lastSyncTimestamp = System.currentTimeMillis()))
                }
            }

            // 2. Lấy các bản ghi từ Firestore cập nhật về local
            val remoteUpdates = remoteDataSource.getDiariesUpdatedAfter(userId, lastSyncTimestamp)
            for (remoteDiary in remoteUpdates) {
                diaryDao.insertDiary(remoteDiary)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}