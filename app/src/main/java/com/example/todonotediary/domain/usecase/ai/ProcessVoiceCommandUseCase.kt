package com.example.todonotediary.domain.usecase.ai

import android.util.Log
import com.example.todonotediary.domain.model.CommandIntent
import com.example.todonotediary.domain.model.VoiceCommand
import com.example.todonotediary.domain.repository.AIRepository
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.example.todonotediary.domain.usecase.todo.TodoUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Process voice command and execute corresponding action
 */
class ProcessVoiceCommandUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val todoUseCases: TodoUseCases,
    private val authUseCases: AuthUseCases
) {
    
    companion object {
        private const val TAG = "ProcessVoiceCommand"
    }
    
    suspend operator fun invoke(text: String): Result<VoiceCommand> = withContext(Dispatchers.IO) {
        try {
            val commandResult = aiRepository.processVoiceCommand(text)
            
            commandResult.fold(
                onSuccess = { command ->
                    Log.d(TAG, "✅ Intent: ${command.intent}")
                    
                    when (command.intent) {
                        CommandIntent.ADD_TODO -> {
                            executeTodoAddition(command)
                        }
                        CommandIntent.QUERY_TODOS -> {
                            executeTodoQuery(command)
                        }
                        CommandIntent.COMPLETE_TODO -> {
                            Result.success(command)
                        }
                        CommandIntent.GENERAL_QUESTION -> {
                            // Trả về câu trả lời cho câu hỏi tự nhiên
                            Result.success(command)
                        }
                        CommandIntent.UNKNOWN -> {
                            Result.failure(Exception("Không hiểu lệnh: $text"))
                        }
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
    
    private suspend fun executeTodoAddition(command: VoiceCommand): Result<VoiceCommand> {
        val currentUser = authUseCases.getCurrentUser()
        if (currentUser == null) {
            Log.e(TAG, "Not authenticated")
            return Result.failure(Exception("Bạn cần đăng nhập để thêm công việc"))
        }
        
        val todoData = command.todoData
        if (todoData == null) {
            return Result.failure(Exception("Thiếu thông tin công việc"))
        }

        // Xử lý logic startAt và deadline
        val now = System.currentTimeMillis()
        val startAt = todoData.startAt ?: now
        var deadline = todoData.deadline
        // Nếu deadline có nhưng nhỏ hơn startAt, bỏ deadline
        if (deadline != null && deadline < startAt) {
            deadline = null
        }

        return try {
            val result = todoUseCases.addTodo(
                userId = currentUser.uid,
                title = todoData.title,
                description = todoData.description ?: "",
                startAt = startAt,
                deadline = deadline
            )

            result.fold(
                onSuccess = { todo ->
                    Log.d(TAG, "✅ Added: ${todo.id}")
                    Result.success(command)
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun executeTodoQuery(command: VoiceCommand): Result<VoiceCommand> {
        val currentUser = authUseCases.getCurrentUser()
        if (currentUser == null) {
            Log.e(TAG, "Not authenticated")
            return Result.failure(Exception("Bạn cần đăng nhập để xem công việc"))
        }
        
        return try {
            // Trả về command với response text từ AI
            // UI sẽ tự động hiển thị danh sách todo hiện có
            Result.success(command)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Query Error: ${e.message}")
            Result.failure(e)
        }
    }
}
