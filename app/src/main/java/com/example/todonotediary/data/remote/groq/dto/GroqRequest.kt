package com.example.todonotediary.data.remote.groq.dto

import com.google.gson.annotations.SerializedName

data class GroqChatRequest(
    val model: String = "llama-3.1-70b-versatile",
    val messages: List<Message>,
    val temperature: Float = 0.3f,
    @SerializedName("response_format")
    val responseFormat: ResponseFormat? = null,
    @SerializedName("max_tokens")
    val maxTokens: Int? = 1000
)

data class Message(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class ResponseFormat(
    val type: String // "json_object"
)
