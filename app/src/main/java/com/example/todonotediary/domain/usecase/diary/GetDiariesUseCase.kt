package com.example.todonotediary.domain.usecase.diary

import com.example.todonotediary.domain.model.DiaryEntity
import com.example.todonotediary.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDiariesUseCase @Inject constructor(
    private val diaryRepository: DiaryRepository
) {
    operator fun invoke(userId: String): Flow<List<DiaryEntity>> {
        return diaryRepository.getDiaries(userId)
    }
}