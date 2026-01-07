package com.example.todonotediary.data.remote.sentiment

import com.example.todonotediary.data.remote.sentiment.dto.SentimentRequest
import com.example.todonotediary.data.remote.sentiment.dto.SentimentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service for sentiment analysis API
 * API runs on local machine at http://192.168.1.5:8000
 */
interface SentimentApiService {
    
    @POST("predict")
    suspend fun analyzeSentiment(
        @Body request: SentimentRequest
    ): Response<SentimentResponse>
}
