package com.example.todonotediary.data.remote.sentiment.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for sentiment analysis API
 */
data class SentimentRequest(
    @SerializedName("text")
    val text: String
)
