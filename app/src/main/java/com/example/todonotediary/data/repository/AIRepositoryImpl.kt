package com.example.todonotediary.data.repository

import android.util.Log
import com.example.todonotediary.data.remote.groq.GroqClient
import com.example.todonotediary.data.remote.groq.dto.VoiceCommandResponse
import com.example.todonotediary.domain.model.CommandIntent
import com.example.todonotediary.domain.model.TodoData
import com.example.todonotediary.domain.model.VoiceCommand
import com.example.todonotediary.domain.repository.AIRepository
import com.example.todonotediary.utils.GroqPromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(
    private val groqClient: GroqClient
) : AIRepository {
    
    companion object {
        private const val TAG = "AIRepositoryImpl"
    }
    
    override suspend fun processVoiceCommand(text: String): Result<VoiceCommand> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = GroqPromptBuilder.buildSystemPrompt(System.currentTimeMillis())
            
            val result = groqClient.chatCompletion(
                systemPrompt = systemPrompt,
                userMessage = text,
                useJsonFormat = true
            )
            
            result.fold(
                onSuccess = { response ->
                    Log.d(TAG, "🤖 Groq Response: $response")
                    val parsed = groqClient.parseResponse(response, VoiceCommandResponse::class.java)
                    if (parsed != null) {
                        Log.d(TAG, "📦 Parsed: intent=${parsed.intent}, title=${parsed.title}, startAt=${parsed.startAt}, deadline=${parsed.deadline}, responseVi=${parsed.responseVi}")
                        val voiceCommand = mapToVoiceCommand(parsed)
                        Result.success(voiceCommand)
                    } else {
                        Log.e(TAG, "❌ Parse failed")
                        Result.failure(Exception("Không thể phân tích"))
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun queryTodosNatural(userId: String, query: String): Result<String> {
        // TODO: Implement natural language query
        return Result.success("Tính năng truy vấn đang được phát triển")
    }
    
    private fun mapToVoiceCommand(response: VoiceCommandResponse): VoiceCommand {
        val intent = when (response.intent.uppercase()) {
            "ADD_TODO" -> CommandIntent.ADD_TODO
            "QUERY_TODOS" -> CommandIntent.QUERY_TODOS
            "COMPLETE_TODO" -> CommandIntent.COMPLETE_TODO
            "GENERAL_QUESTION" -> CommandIntent.GENERAL_QUESTION
            else -> CommandIntent.UNKNOWN
        }
        
        val todoData = if (intent == CommandIntent.ADD_TODO && response.title != null) {
            TodoData(
                title = response.title,
                description = response.description,
                startAt = response.startAt,
                deadline = response.deadline
            )
        } else {
            null
        }
        
        return VoiceCommand(
            intent = intent,
            todoData = todoData,
            responseText = response.responseVi ?: "Đã xử lý",
            confidence = response.confidence
        )
    }
}
