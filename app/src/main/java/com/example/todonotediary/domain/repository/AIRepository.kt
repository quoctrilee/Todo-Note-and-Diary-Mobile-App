package com.example.todonotediary.domain.repository

import com.example.todonotediary.domain.model.SentimentResult
import com.example.todonotediary.domain.model.VoiceCommand

/**
 * Repository for AI-related operations
 */
interface AIRepository {
    
    /**
     * Process voice command text and extract intent & entities
     */
    suspend fun processVoiceCommand(text: String): Result<VoiceCommand>
    
    /**
     * Query todos using natural language
     */
    suspend fun queryTodosNatural(userId: String, query: String): Result<String>
    
    /**
     * Generate personalized response for diary entry based on sentiment
     * @param content Diary content
     * @param sentiment Sentiment analysis result
     * @return AI-generated response message
     */
    suspend fun generateDiaryResponse(content: String, sentiment: SentimentResult): Result<String>
}
