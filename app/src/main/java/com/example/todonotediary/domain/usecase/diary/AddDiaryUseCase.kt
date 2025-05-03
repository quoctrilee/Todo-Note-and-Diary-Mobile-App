package com.example.todonotediary.domain.usecase.diary

import com.example.todonotediary.domain.model.DiaryEntity
import com.example.todonotediary.domain.repository.DiaryRepository
import java.util.UUID
import javax.inject.Inject

class AddDiaryUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    suspend operator fun invoke(
        date: Long,
        mood: String,
        content: String,
        userId: String
    ): Result<DiaryEntity> {
        val diary = DiaryEntity(
            id = UUID.randomUUID().toString(),
            date = date,
            mood = mood,
            content = content,
            userId = userId,
            lastSyncTimestamp = 0,
            isDeleted = false
        )
        return diaryRepository.addDiary(diary)
    }
}
