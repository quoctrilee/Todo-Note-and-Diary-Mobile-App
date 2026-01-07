package com.example.todonotediary.domain.usecase.diary

import com.example.todonotediary.domain.model.SentimentResult
import com.example.todonotediary.domain.repository.SentimentRepository
import javax.inject.Inject

/**
 * Use case to analyze diary content sentiment
 */
class AnalyzeDiarySentimentUseCase @Inject constructor(
    private val sentimentRepository: SentimentRepository
) {
    suspend operator fun invoke(content: String): Result<SentimentResult> {
        return sentimentRepository.analyzeSentiment(content)
    }
}
