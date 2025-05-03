package com.example.todonotediary.domain.repository

import com.example.todonotediary.domain.model.DiaryEntity
import kotlinx.coroutines.flow.Flow

interface DiaryRepository {
    fun getDiaries(userId: String): Flow<List<DiaryEntity>>
    suspend fun getDiaryById(diaryId: String): DiaryEntity?
    suspend fun addDiary(diary: DiaryEntity): Result<DiaryEntity>
    suspend fun  updateDiary(diary: DiaryEntity): Result<Unit>
    suspend fun deleteDiary(diaryId: String): Result<Unit>
    suspend fun syncDiaries(userId: String, lastSyncTimestamp: Long): Result<Unit>
}