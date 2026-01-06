package com.example.todonotediary.domain.model

/**
 * Voice command parsed from speech input
 */
data class VoiceCommand(
    val intent: CommandIntent,
    val todoData: TodoData? = null,
    val responseText: String = "",
    val confidence: Float = 0.0f,
    val queryFilter: String? = null // "today" | "tomorrow" | "this_week" | "this_month" | "all"
)

/**
 * Todo data extracted from voice command
 */
data class TodoData(
    val title: String,
    val description: String? = null,
    val startAt: Long? = null,
    val deadline: Long? = null
)

/**
 * Intent types for voice commands
 */
enum class CommandIntent {
    ADD_TODO,
    QUERY_TODOS,
    COMPLETE_TODO,
    GENERAL_QUESTION
}
