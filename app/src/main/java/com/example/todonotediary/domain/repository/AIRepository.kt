package com.example.todonotediary.domain.repository

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
}
