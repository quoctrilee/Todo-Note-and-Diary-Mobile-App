package com.example.todonotediary.domain.usecase.diary

import com.example.todonotediary.domain.model.DiaryResponse
import com.example.todonotediary.domain.model.SentimentResult
import com.example.todonotediary.domain.repository.AIRepository
import javax.inject.Inject

/**
 * Use case to generate personalized AI response for diary entry
 * based on sentiment analysis
 */
class GenerateDiaryResponseUseCase @Inject constructor(
    private val aiRepository: AIRepository
) {
    suspend operator fun invoke(
        content: String,
        sentiment: SentimentResult
    ): Result<DiaryResponse> {
        return aiRepository.generateDiaryResponse(content, sentiment).map { message ->
            DiaryResponse(
                message = message,
                sentiment = sentiment
            )
        }
    }
}
