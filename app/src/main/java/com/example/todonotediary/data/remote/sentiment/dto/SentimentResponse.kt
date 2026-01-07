package com.example.todonotediary.data.remote.sentiment.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO from sentiment analysis API
 */
data class SentimentResponse(
    @SerializedName("score")
    val score: Double,
    
    @SerializedName("label")
    val label: String
)
