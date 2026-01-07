package com.example.todonotediary.data.repository

import android.util.Log
import com.example.todonotediary.data.remote.sentiment.SentimentApiService
import com.example.todonotediary.data.remote.sentiment.dto.SentimentRequest
import com.example.todonotediary.domain.model.SentimentResult
import com.example.todonotediary.domain.repository.SentimentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SentimentRepositoryImpl @Inject constructor(
    private val sentimentApiService: SentimentApiService
) : SentimentRepository {
    
    companion object {
        private const val TAG = "SentimentRepository"
    }
    
    override suspend fun analyzeSentiment(text: String): Result<SentimentResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Analyzing sentiment for text: ${text.take(50)}...")
            
            val request = SentimentRequest(text = text)
            val response = sentimentApiService.analyzeSentiment(request)
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(TAG, "✅ Sentiment result: score=${body.score}, label=${body.label}")
                
                Result.success(
                    SentimentResult(
                        score = body.score,
                        label = body.label,
                        rawText = text
                    )
                )
            } else {
                val error = "API error: ${response.code()} - ${response.message()}"
                Log.e(TAG, "❌ $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sentiment analysis failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
