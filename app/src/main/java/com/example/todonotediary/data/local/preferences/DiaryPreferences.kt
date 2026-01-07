package com.example.todonotediary.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages diary-related user preferences
 */
@Singleton
class DiaryPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREF_NAME = "diary_preferences"
        private const val KEY_SENTIMENT_ANALYSIS_ENABLED = "sentiment_analysis_enabled"
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if sentiment analysis feature is enabled
     * Default: false (disabled for privacy)
     */
    fun isSentimentAnalysisEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SENTIMENT_ANALYSIS_ENABLED, false)
    }
    
    /**
     * Enable or disable sentiment analysis feature
     */
    fun setSentimentAnalysisEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SENTIMENT_ANALYSIS_ENABLED, enabled)
            .apply()
    }
}
