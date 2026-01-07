package com.example.todonotediary.domain.model

/**
 * Represents AI-generated response for user's diary entry
 * Based on sentiment analysis results
 */
data class DiaryResponse(
    val message: String,            // AI response message (advice, encouragement, comfort)
    val sentiment: SentimentResult  // Associated sentiment analysis
)
