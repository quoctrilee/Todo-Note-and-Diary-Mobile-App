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
    val startAt: String? = null, // Format: "dd/MM/yyyy HH:mm"
    val deadline: String? = null, // Format: "dd/MM/yyyy HH:mm"
    @SerializedName("response_vi")
    val responseVi: String? = null,
    val confidence: Float = 0.0f,
    @SerializedName("query_filter")
    val queryFilter: String? = null // "today" | "tomorrow" | "this_week" | "this_month" | "all"
)
