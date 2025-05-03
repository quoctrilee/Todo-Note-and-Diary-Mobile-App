package com.example.todonotediary.domain.usecase.diary

import com.example.todonotediary.domain.repository.DiaryRepository
import javax.inject.Inject

class SyncDiariesUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    suspend operator fun invoke(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        return diaryRepository.syncDiaries(userId, lastSyncTimestamp)
    }
}