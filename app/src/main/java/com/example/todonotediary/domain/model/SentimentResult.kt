package com.example.todonotediary.domain.model

/**
 * Represents the sentiment analysis result from the sentiment API
 */
data class SentimentResult(
    val score: Double,           // Sentiment score (0.0 to 1.0)
    val label: String,          // Label description (e.g., "Lo âu/Buồn nhẹ", "Vui vẻ")
    val rawText: String         // Original text analyzed
)
