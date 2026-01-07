package com.example.todonotediary.domain.repository

import com.example.todonotediary.domain.model.SentimentResult

/**
 * Repository interface for sentiment analysis operations
 */
interface SentimentRepository {
    
    /**
     * Analyze sentiment of given text
     * @param text The text to analyze
     * @return Result containing SentimentResult or error
     */
    suspend fun analyzeSentiment(text: String): Result<SentimentResult>
}
