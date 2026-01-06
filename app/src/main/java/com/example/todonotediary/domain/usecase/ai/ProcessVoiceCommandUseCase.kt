package com.example.todonotediary.domain.usecase.ai

import android.util.Log
import com.example.todonotediary.domain.model.CommandIntent
import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.model.VoiceCommand
import com.example.todonotediary.domain.repository.AIRepository
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.example.todonotediary.domain.usecase.todo.TodoUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
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
                        CommandIntent.ADD_TODO -> executeTodoAddition(command)
                        CommandIntent.QUERY_TODOS -> executeTodoQuery(command)
                        CommandIntent.COMPLETE_TODO -> executeTodoCompletion(command)
                        CommandIntent.GENERAL_QUESTION -> {
                            // Trả về câu trả lời tự nhiên, không liên quan todos
                            if (command.responseText.isNotBlank()) {
                                Result.success(command)
                            } else {
                                Result.failure(Exception("Xin lỗi, tôi chưa có câu trả lời cho câu hỏi này."))
                            }
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
            val currentTime = System.currentTimeMillis()
            
            // Lấy tất cả todos của user
            val allTodos = todoUseCases.getTodos(currentUser.uid).firstOrNull() ?: emptyList()
            
            // Filter upcoming todos: chưa hoàn thành, chưa xóa, deadline chưa qua (hoặc không có deadline)
            val upcomingTodos = allTodos.filter { todo ->
                val todoDeadline = todo.deadline
                !todo.isCompleted && !todo.isDeleted && (todoDeadline == null || todoDeadline >= currentTime)
            }
            
            Log.d(TAG, "📋 Query filter: ${command.queryFilter}")
            Log.d(TAG, "📋 Total upcoming todos: ${upcomingTodos.size}")
            
            // Filter theo query_filter
            val filteredTodos = when (command.queryFilter) {
                "today" -> {
                    val todayStart = getTodayStartTime()
                    val todayEnd = getTodayEndTime()
                    allTodos.filter { todo ->
                        !todo.isCompleted && !todo.isDeleted && (
                            (todo.startAt != null && todo.startAt in todayStart..todayEnd) ||
                            (todo.deadline != null && todo.deadline in todayStart..todayEnd)
                        )
                    }
                }
                "tomorrow" -> {
                    val tomorrowStart = getTomorrowStartTime()
                    val tomorrowEnd = getTomorrowEndTime()
                    upcomingTodos.filter { todo ->
                        val todoDeadline = todo.deadline
                        val todoTime = todoDeadline ?: todo.startAt ?: 0L
                        todoTime in tomorrowStart..tomorrowEnd
                    }
                }
                "this_week" -> {
                    val weekStart = getThisWeekStartTime()
                    val weekEnd = getThisWeekEndTime()
                    upcomingTodos.filter { todo ->
                        val todoDeadline = todo.deadline
                        val todoTime = todoDeadline ?: todo.startAt ?: 0L
                        todoTime in weekStart..weekEnd
                    }
                }
                "this_month" -> {
                    val monthStart = getThisMonthStartTime()
                    val monthEnd = getThisMonthEndTime()
                    upcomingTodos.filter { todo ->
                        val todoDeadline = todo.deadline
                        val todoTime = todoDeadline ?: todo.startAt ?: 0L
                        todoTime in monthStart..monthEnd
                    }
                }
                "all" -> upcomingTodos
                else -> upcomingTodos // Default show all upcoming
            }
            
            Log.d(TAG, "✅ Filtered todos: ${filteredTodos.size}")
            filteredTodos.forEach {
                Log.d(TAG, "  - ${it.title}")
            }
            
            // Tạo response text tự nhiên từ danh sách todos
            val responseText = buildTodoListResponse(filteredTodos, command.queryFilter)
            
            Result.success(command.copy(responseText = responseText))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Query Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun buildTodoListResponse(todos: List<TodoEntity>, filter: String?): String {
        if (todos.isEmpty()) {
            return when (filter) {
                "today" -> "Bạn không có công việc nào hôm nay"
                "tomorrow" -> "Bạn không có công việc nào vào ngày mai"
                "this_week" -> "Bạn không có công việc nào trong tuần này"
                "this_month" -> "Bạn không có công việc nào trong tháng này"
                else -> "Bạn không có công việc nào chưa hoàn thành"
            }
        }
        
        val filterDesc = when (filter) {
            "today" -> "hôm nay"
            "tomorrow" -> "ngày mai"
            "this_week" -> "trong tuần này"
            "this_month" -> "trong tháng này"
            else -> "chưa hoàn thành"
        }
        
        val dateFormat = java.text.SimpleDateFormat("HH:mm 'ngày' dd/MM", java.util.Locale("vi", "VN"))
        
        val todoDescriptions = todos.take(5).mapIndexed { index, todo ->
            val timeInfo = when {
                todo.deadline != null && todo.startAt != null -> {
                    "từ ${dateFormat.format(todo.startAt)} đến ${dateFormat.format(todo.deadline)}"
                }
                todo.deadline != null -> {
                    "hạn chót ${dateFormat.format(todo.deadline)}"
                }
                todo.startAt != null -> {
                    "bắt đầu lúc ${dateFormat.format(todo.startAt)}"
                }
                else -> ""
            }
            
            "${index + 1}. ${todo.title}${if (timeInfo.isNotEmpty()) " - $timeInfo" else ""}"
        }
        
        val header = "Bạn có ${todos.size} công việc $filterDesc:\n"
        val moreInfo = if (todos.size > 5) "\nvà ${todos.size - 5} công việc khác" else ""
        
        return header + todoDescriptions.joinToString("\n") + moreInfo
    }
    
    private fun getTodayStartTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getTodayEndTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    private fun getTomorrowStartTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getTomorrowEndTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    private fun getThisWeekStartTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        // Set to Monday 00:00:00
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val diff = if (dayOfWeek == java.util.Calendar.SUNDAY) {
            -6 // Sunday to Monday
        } else {
            java.util.Calendar.MONDAY - dayOfWeek
        }
        calendar.add(java.util.Calendar.DAY_OF_MONTH, diff)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getThisWeekEndTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        // Set to Sunday 23:59:59
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val diff = if (dayOfWeek == java.util.Calendar.SUNDAY) {
            0
        } else {
            java.util.Calendar.SATURDAY - dayOfWeek + 1
        }
        calendar.add(java.util.Calendar.DAY_OF_MONTH, diff)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    private fun getThisMonthStartTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getThisMonthEndTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    private suspend fun executeTodoCompletion(command: VoiceCommand): Result<VoiceCommand> {
        val currentUser = authUseCases.getCurrentUser()
        if (currentUser == null) {
            Log.e(TAG, "Not authenticated")
            return Result.failure(Exception("Bạn cần đăng nhập để hoàn thành công việc"))
        }
        
        val todoData = command.todoData
        if (todoData == null || todoData.title.isBlank()) {
            return Result.failure(Exception("Thiếu thông tin công việc cần hoàn thành"))
        }
        
        return try {
            // Lấy danh sách todos của user
            val todos = todoUseCases.getTodos(currentUser.uid).firstOrNull() ?: emptyList()
            val uncompletedTodos = todos.filter { !it.isCompleted }
            
            Log.d(TAG, "🔍 Tìm todo: '${todoData.title}'")
            Log.d(TAG, "📋 Có ${uncompletedTodos.size} todos chưa hoàn thành: ${uncompletedTodos.map { it.title }}")
            
            // Tìm todo chưa hoàn thành khớp với title
            val matchedTodo = findBestMatchTodo(todoData.title, uncompletedTodos)
            
            if (matchedTodo == null) {
                Log.e(TAG, "❌ Todo not found: '${todoData.title}'")
                return Result.failure(Exception("Không tìm thấy công việc '${todoData.title}'"))
            }
            
            Log.d(TAG, "✅ Matched todo: '${matchedTodo.title}' (id=${matchedTodo.id})")
            
            // Đánh dấu hoàn thành
            val result = todoUseCases.toggleTodoCompletion(matchedTodo.id)
            
            result.fold(
                onSuccess = {
                    Log.d(TAG, "✅ Completed: ${matchedTodo.title}")
                    Result.success(command)
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Failed to complete: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Completion Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Tìm todo khớp nhất với title từ voice command
     * Sử dụng simple fuzzy matching (lowercase + contains)
     */
    private fun findBestMatchTodo(
        voiceTitle: String,
        todos: List<com.example.todonotediary.domain.model.TodoEntity>
    ): com.example.todonotediary.domain.model.TodoEntity? {
        val normalizedVoiceTitle = voiceTitle.lowercase().trim()
        
        Log.d(TAG, "🔍 Finding match for: '$normalizedVoiceTitle'")
        
        // Tìm exact match trước
        todos.find { it.title.lowercase().trim() == normalizedVoiceTitle }?.let { 
            Log.d(TAG, "✅ Exact match: '${it.title}'")
            return it 
        }
        
        // Tìm contains match (todo title chứa voice title)
        todos.find { it.title.lowercase().contains(normalizedVoiceTitle) }?.let { 
            Log.d(TAG, "✅ Contains match: '${it.title}'")
            return it 
        }
        
        // Tìm partial match (voice title chứa todo title)
        todos.find { normalizedVoiceTitle.contains(it.title.lowercase()) }?.let { 
            Log.d(TAG, "✅ Partial match: '${it.title}'")
            return it 
        }
        
        Log.e(TAG, "❌ No match found")
        return null
    }
}
