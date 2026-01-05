package com.example.todonotediary.data.remote.groq.dto

import com.google.gson.annotations.SerializedName

/**
 * Parsed response from Groq AI for voice commands
 */
data class VoiceCommandResponse(
    val intent: String, // "ADD_TODO", "QUERY_TODOS", "COMPLETE_TODO", "GENERAL_QUESTION", "UNKNOWN"
    val title: String? = null,
    val description: String? = null,
    @SerializedName("start_at")
    val startAt: Long? = null,
    val deadline: Long? = null,
    @SerializedName("response_vi")
    val responseVi: String? = null,
    val confidence: Float = 0.0f
)
