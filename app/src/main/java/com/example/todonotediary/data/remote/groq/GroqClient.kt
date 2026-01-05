package com.example.todonotediary.data.remote.groq

import android.util.Log
import com.example.todonotediary.data.remote.groq.dto.GroqChatRequest
import com.example.todonotediary.data.remote.groq.dto.GroqChatResponse
import com.example.todonotediary.data.remote.groq.dto.Message
import com.example.todonotediary.data.remote.groq.dto.ResponseFormat
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client wrapper for Groq API with error handling
 */
@Singleton
class GroqClient @Inject constructor(
    private val apiService: GroqApiService,
    internal val gson: Gson
) {
    
    companion object {
        private const val TAG = "GroqClient"
        // llama-3.3-70b-versatile: More powerful, better reasoning (70B params)
        // llama-3.1-8b-instant: Faster but weaker reasoning (8B params)
        private const val MODEL = "llama-3.3-70b-versatile"
    }
    
    /**
     * Send chat completion request to Groq
     */
    suspend fun chatCompletion(
        systemPrompt: String,
        userMessage: String,
        temperature: Float = 0.7f,
        useJsonFormat: Boolean = true
    ): Result<GroqChatResponse> = withContext(Dispatchers.IO) {
        try {
            val request = GroqChatRequest(
                model = MODEL,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userMessage)
                ),
                temperature = temperature,
                responseFormat = if (useJsonFormat) ResponseFormat("json_object") else null,
                maxTokens = 500
            )
            
            val response = apiService.chatCompletion(request)
            Result.success(response)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Groq HTTP ${e.code()}: $errorBody")
            Result.failure(Exception("Lỗi API: ${e.code()} - ${e.message()}"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Groq error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Parse response content as specific type
     */
    fun <T> parseResponse(response: GroqChatResponse, typeClass: Class<T>): T? {
        return try {
            val content = response.choices.firstOrNull()?.message?.content ?: return null
            gson.fromJson(content, typeClass)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            null
        }
    }
}
