package com.example.todonotediary.utils

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.pow

object RetryHelper {
    private const val TAG = "RetryHelper"
    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1000L // 1 second

    /**
     * Retry a suspending function with exponential backoff
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param initialBackoffMs Initial backoff delay in milliseconds (default: 1000ms)
     * @param block The suspending function to retry
     * @return Result of the operation
     */
    suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = MAX_RETRIES,
        initialBackoffMs: Long = INITIAL_BACKOFF_MS,
        block: suspend () -> T
    ): Result<T> {
        var currentAttempt = 0
        var lastException: Exception? = null

        while (currentAttempt <= maxRetries) {
            try {
                val result = block()
                if (currentAttempt > 0) {
                    Log.d(TAG, "Operation succeeded after $currentAttempt retries")
                }
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                currentAttempt++

                if (currentAttempt > maxRetries) {
                    Log.e(TAG, "Max retries ($maxRetries) exceeded", e)
                    break
                }

                // Calculate exponential backoff: initialBackoff * 2^(attempt - 1)
                val backoffMs = initialBackoffMs * (2.0.pow(currentAttempt - 1)).toLong()
                Log.d(TAG, "Retry attempt $currentAttempt/$maxRetries after ${backoffMs}ms delay")
                
                delay(backoffMs)
            }
        }

        return Result.failure(lastException ?: Exception("Unknown error"))
    }

    /**
     * Retry with custom condition check
     */
    suspend fun <T> retryWithCondition(
        maxRetries: Int = MAX_RETRIES,
        shouldRetry: (Exception) -> Boolean,
        block: suspend () -> T
    ): Result<T> {
        var currentAttempt = 0
        var lastException: Exception? = null

        while (currentAttempt <= maxRetries) {
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastException = e
                currentAttempt++

                if (currentAttempt > maxRetries || !shouldRetry(e)) {
                    Log.e(TAG, "Retry aborted at attempt $currentAttempt", e)
                    break
                }

                val backoffMs = INITIAL_BACKOFF_MS * (2.0.pow(currentAttempt - 1)).toLong()
                delay(backoffMs)
            }
        }

        return Result.failure(lastException ?: Exception("Unknown error"))
    }
}
