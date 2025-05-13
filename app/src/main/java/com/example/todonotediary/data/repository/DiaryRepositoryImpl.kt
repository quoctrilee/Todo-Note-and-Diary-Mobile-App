package com.example.todonotediary.data.repository

import com.example.todonotediary.data.remote.DiaryRemoteDataSource
import com.example.todonotediary.domain.model.DiaryEntity
import com.example.todonotediary.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DiaryRepositoryImpl @Inject constructor(
    private val remoteDataSource: DiaryRemoteDataSource
) : DiaryRepository {

    override fun getDiaries(userId: String): Flow<List<DiaryEntity>> {
        return flow {
            val diaries = remoteDataSource.getDiaries(userId)
            emit(diaries)
        }.catch {
            emit(emptyList())
        }
    }

    override suspend fun getDiariesByDate(userId: String, date: Long): Flow<List<DiaryEntity>> {
        return flow {
            val diaries = remoteDataSource.getDiariesByDate(userId, date)
            emit(diaries)
        }.catch {
            emit(emptyList())
        }
    }

    override suspend fun getDiariesByTitleOrContent(userId: String, text: String): Flow<List<DiaryEntity>> {
        return flow {
            val diaries = remoteDataSource.searchDiaries(userId, text)
            emit(diaries)
        }.catch {
            emit(emptyList())
        }
    }

    override suspend fun getDiaryById(diaryId: String): DiaryEntity? {
        return remoteDataSource.getDiaryById(diaryId)
    }

    override suspend fun addDiary(diary: DiaryEntity): Result<DiaryEntity> {
        return remoteDataSource.saveDiary(diary)
    }

    override suspend fun updateDiary(diary: DiaryEntity): Result<Unit> {
        val updated = diary.copy(updatedAt = System.currentTimeMillis())
        return remoteDataSource.saveDiary(updated).map { Unit }
    }

    override suspend fun deleteDiary(diaryId: String): Result<Unit> {
        return remoteDataSource.deleteDiary(diaryId)
    }

    override suspend fun syncDiaries(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        // Không thực hiện gì vì không có local database để sync
        return Result.success(Unit)
    }
}
