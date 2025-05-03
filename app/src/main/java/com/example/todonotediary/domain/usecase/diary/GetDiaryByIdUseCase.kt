package com.example.todonotediary.domain.usecase.diary

import com.example.todonotediary.domain.model.DiaryEntity
import com.example.todonotediary.domain.repository.DiaryRepository
import javax.inject.Inject

class GetDiaryByIdUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    suspend operator fun invoke(diaryId: String): DiaryEntity? {
        return diaryRepository.getDiaryById(diaryId)
    }
}