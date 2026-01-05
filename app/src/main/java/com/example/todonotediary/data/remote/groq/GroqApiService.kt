package com.example.todonotediary.data.remote.groq

import com.example.todonotediary.data.remote.groq.dto.GroqChatRequest
import com.example.todonotediary.data.remote.groq.dto.GroqChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for Groq API
 * Base URL: https://api.groq.com/
 */
interface GroqApiService {
    
    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Body request: GroqChatRequest
    ): GroqChatResponse
}