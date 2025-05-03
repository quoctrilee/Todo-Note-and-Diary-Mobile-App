package com.example.todonotediary.domain.usecase.diary

import com.example.todonotediary.domain.repository.DiaryRepository
import javax.inject.Inject

class DeleteDiaryUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    suspend operator fun invoke(diaryId: String): Result<Unit> {
        return diaryRepository.deleteDiary(diaryId)
    }
}