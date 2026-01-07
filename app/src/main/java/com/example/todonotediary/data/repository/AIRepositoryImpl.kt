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
import java.text.SimpleDateFormat
import java.util.Locale
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
                        Log.d(TAG, "📦 Parsed: intent=${parsed.intent}, title=${parsed.title}, startAt='${parsed.startAt}', deadline='${parsed.deadline}', responseVi=${parsed.responseVi}")
                        val voiceCommand = mapToVoiceCommand(parsed)
                        Log.d(TAG, "✅ Mapped: startAtTimestamp=${voiceCommand.todoData?.startAt}, deadlineTimestamp=${voiceCommand.todoData?.deadline}")
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
    
    override suspend fun generateDiaryResponse(
        content: String,
        sentiment: com.example.todonotediary.domain.model.SentimentResult
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🤖 Generating diary response for sentiment: ${sentiment.label} (${sentiment.score})")
            
            // Build context-aware prompt based on sentiment
            val systemPrompt = buildDiaryResponsePrompt(sentiment)
            val userMessage = "Nội dung nhật ký: \"$content\""
            
            val result = groqClient.chatCompletion(
                systemPrompt = systemPrompt,
                userMessage = userMessage,
                useJsonFormat = false  // Get natural text response
            )
            
            result.fold(
                onSuccess = { groqResponse ->
                    // Extract message content from GroqChatResponse
                    val messageContent = groqResponse.choices.firstOrNull()?.message?.content
                    if (messageContent != null) {
                        Log.d(TAG, "✅ Generated response: ${messageContent.take(100)}...")
                        Result.success(messageContent)
                    } else {
                        Log.e(TAG, "❌ Empty response from Groq")
                        Result.failure(Exception("Không nhận được phản hồi từ AI"))
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to generate response: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception generating diary response: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Build sentiment-aware prompt for diary response
     */
    private fun buildDiaryResponsePrompt(sentiment: com.example.todonotediary.domain.model.SentimentResult): String {
        val emotionContext = when {
            sentiment.score < 0.3 -> "Người dùng đang có cảm xúc tiêu cực mạnh (${sentiment.label})"
            sentiment.score < 0.5 -> "Người dùng đang cảm thấy lo lắng hoặc buồn nhẹ (${sentiment.label})"
            sentiment.score < 0.7 -> "Người dùng có cảm xúc trung tính hoặc bình thường (${sentiment.label})"
            sentiment.score < 0.85 -> "Người dùng đang có cảm xúc tích cực (${sentiment.label})"
            else -> "Người dùng đang rất vui vẻ và hạnh phúc (${sentiment.label})"
        }
        
        return """
Bạn là một người bạn thân thiết, tử tế và thấu hiểu. Người dùng vừa viết nhật ký với tâm trạng: $emotionContext (điểm cảm xúc: ${sentiment.score}).

Hãy đưa ra phản hồi ngắn gọn (2-3 câu) phù hợp với tâm trạng của họ:
- Nếu họ buồn/lo âu: An ủi, động viên, đưa ra lời khuyên tích cực
- Nếu họ vui vẻ: Chia sẻ niềm vui, khích lệ tiếp tục
- Nếu họ bình thường: Gửi lời chúc tốt đẹp

Sử dụng emoji phù hợp, giọng văn thân thiện, ấm áp. Viết bằng tiếng Việt.
        """.trimIndent()
    }
    
    private fun mapToVoiceCommand(response: VoiceCommandResponse): VoiceCommand {
        val intent = when (response.intent.uppercase()) {
            "ADD_TODO" -> CommandIntent.ADD_TODO
            "QUERY_TODOS" -> CommandIntent.QUERY_TODOS
            "COMPLETE_TODO" -> CommandIntent.COMPLETE_TODO
            "GENERAL_QUESTION" -> CommandIntent.GENERAL_QUESTION
            else -> CommandIntent.GENERAL_QUESTION // Mặc định cho vào GENERAL_QUESTION nếu không rõ
        }
        
        val todoData = when (intent) {
            CommandIntent.ADD_TODO -> {
                if (response.title != null) {
                    TodoData(
                        title = response.title,
                        description = response.description,
                        startAt = parseDateTimeToTimestamp(response.startAt),
                        deadline = parseDateTimeToTimestamp(response.deadline)
                    )
                } else null
            }
            CommandIntent.COMPLETE_TODO -> {
                if (response.title != null) {
                    TodoData(
                        title = response.title,
                        description = response.description,
                        startAt = null,
                        deadline = null
                    )
                } else null
            }
            else -> null
        }
        
        return VoiceCommand(
            intent = intent,
            todoData = todoData,
            responseText = response.responseVi ?: "",
            confidence = response.confidence,
            queryFilter = response.queryFilter
        )
    }
    
    /**
     * Parse datetime string to timestamp (milliseconds)
     * Format: "dd/MM/yyyy HH:mm"
     * Đồng nhất với cách lưu thủ công: set second=0, millisecond=0
     */
    private fun parseDateTimeToTimestamp(dateTimeStr: String?): Long? {
        if (dateTimeStr.isNullOrBlank()) return null
        
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
            val date = format.parse(dateTimeStr) ?: return null
            
            // Đồng nhất với AddTodoScreen: set second=0, millisecond=0
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            
            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to parse datetime: $dateTimeStr", e)
            null
        }
    }
}
