package com.example.todonotediary.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Text-to-Speech (TTS)
 */
@Singleton
class TextToSpeechHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "TextToSpeech"
    }
    
    private var tts: TextToSpeech? = null
    @Volatile private var isInitialized = false
    private var currentCallback: (() -> Unit)? = null

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("vi", "VN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Vietnamese language not supported, using default")
                        tts?.setLanguage(Locale.US)
                    }
                    tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            currentCallback?.invoke()
                            currentCallback = null
                        }
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "TTS error")
                            currentCallback?.invoke()
                            currentCallback = null
                        }
                    })
                    isInitialized = true
                } else {
                    Log.e(TAG, "TTS initialization failed")
                    isInitialized = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS init exception", e)
            isInitialized = false
        }
    }
    
    /**
     * Speak the given text
     */
    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized yet")
            onComplete()
            return
        }
        if (text.isBlank()) {
            Log.w(TAG, "TTS: empty text")
            onComplete()
            return
        }
        try {
            currentCallback = onComplete
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
            if (result != TextToSpeech.SUCCESS) {
                Log.e(TAG, "TTS speak() failed: $result")
                currentCallback = null
                onComplete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            currentCallback = null
            onComplete()
        }
    }
    
    /**
     * Stop speaking
     */
    fun stop() {
        try {
            tts?.stop()
            currentCallback?.invoke()
            currentCallback = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }
    
    /**
     * Cleanup resources
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            currentCallback = null
            Log.d(TAG, "TTS shutdown")
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
