package com.example.todonotediary.data.local

import androidx.room.*
import com.example.todonotediary.domain.model.DiaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: DiaryEntity)

    @Update
    suspend fun updateDiary(diary: DiaryEntity)

    @Query("SELECT * FROM diaries WHERE userId = :userId AND isDeleted = 0 AND pendingDelete = 0 ORDER BY date DESC")
    suspend fun getDiaries(userId: String): List<DiaryEntity>

    @Query("SELECT * FROM diaries WHERE userId = :userId AND isDeleted = 0 AND pendingDelete = 0 ORDER BY date DESC")
    fun getDiariesFlow(userId: String): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diaries ORDER BY date DESC")
    suspend fun getAllDiaries(): List<DiaryEntity>

    @Query("SELECT * FROM diaries WHERE id = :id")
    suspend fun getDiaryById(id: String): DiaryEntity?

    @Query("SELECT * FROM diaries WHERE userId = :userId AND lastSyncTimestamp < updatedAt")
    suspend fun getDiariesToSync(userId: String): List<DiaryEntity>

}