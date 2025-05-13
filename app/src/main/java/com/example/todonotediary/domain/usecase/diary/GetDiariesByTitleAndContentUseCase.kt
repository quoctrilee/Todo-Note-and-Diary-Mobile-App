package com.example.todonotediary.domain.usecase.diary

import com.example.todonotediary.domain.model.DiaryEntity
import com.example.todonotediary.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDiariesByTitleAndContentUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    suspend operator fun invoke(userId: String, query: String): Flow<List<DiaryEntity>> {
        return repository.getDiariesByTitleOrContent(userId, query)
    }
}
