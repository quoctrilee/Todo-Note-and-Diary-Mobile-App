package com.example.todonotediary.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Helper class for Android Speech Recognition (STT)
 */
@Singleton
class SpeechRecognizerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "SpeechRecognizer"
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var lastPartialResult = ""
    private var resultReceived = false
    private var timeoutJob: kotlinx.coroutines.Job? = null
    private var lastLoggedAudioLevel = 0f
    
    /**
     * Start listening for speech input
     */
    fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onAudioLevel: (Float) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }

        Log.d(TAG, "Starting speech recognition...")

        // Check RECORD_AUDIO permission (runtime check)
        val permission = android.Manifest.permission.RECORD_AUDIO
        val pm = context.packageManager
        val hasPermission = try {
            android.content.pm.PackageManager.PERMISSION_GRANTED ==
                androidx.core.content.ContextCompat.checkSelfPermission(context, permission)
        } catch (e: Exception) {
            false
        }
        if (!hasPermission) {
            Log.e(TAG, "No RECORD_AUDIO permission")
            onError("Ứng dụng chưa được cấp quyền ghi âm")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            onError("Speech recognition không khả dụng trên thiết bị này")
            return
        }

        // Reset state
        lastPartialResult = ""
        resultReceived = false
        timeoutJob?.cancel()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "✅ Ready - Mic ready, waiting for speech...")
                        isListening = true
                        
                        // Cancel any existing timeout job first
                        timeoutJob?.cancel()
                        
                        // Start timeout fallback (15 seconds) - only once
                        timeoutJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(15000)
                            if (!resultReceived && isListening) {
                                Log.w(TAG, "⏰ Timeout: No result after 15s")
                                if (lastPartialResult.isNotEmpty()) {
                                    Log.d(TAG, "Using partial: '$lastPartialResult'")
                                    resultReceived = true
                                    onFinalResult(lastPartialResult)
                                } else {
                                    Log.e(TAG, "No speech detected at all")
                                    onError("Không nghe thấy giọng nói. Hãy nói to hơn và rõ ràng hơn.")
                                }
                                isListening = false
                            }
                        }
                    }
                    
                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "🎤 Speech started")
                    }
                    
                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = (rmsdB + 2f) / 12f
                        val level = normalized.coerceIn(0f, 1f)
                        onAudioLevel(level)
                    }
                    
                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Not used
                    }
                    
                    override fun onEndOfSpeech() {
                        Log.d(TAG, "🛑 Speech ended - waiting for results")
                        isListening = false
                        
                        // If no result after 3 seconds, use partial result
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(3000)
                            if (!resultReceived) {
                                if (lastPartialResult.isNotEmpty()) {
                                    Log.d(TAG, "✅ Using partial result as final: '$lastPartialResult'")
                                    onFinalResult(lastPartialResult)
                                    resultReceived = true
                                } else {
                                    Log.w(TAG, "⚠️ No partial or final result received")
                                    onError("Không nhận được giọng nói. Vui lòng nói rõ hơn.")
                                }
                            }
                        }
                    }
                    
                    override fun onError(error: Int) {
                        Log.e(TAG, "❌ Speech recognition error: $error")
                        isListening = false
                        timeoutJob?.cancel()
                        
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> {
                                Log.e(TAG, "ERROR_AUDIO: Problem with audio recording")
                                "Lỗi ghi âm. Kiểm tra microphone."
                            }
                            SpeechRecognizer.ERROR_CLIENT -> {
                                Log.e(TAG, "ERROR_CLIENT: Client side error")
                                "Lỗi client"
                            }
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                Log.e(TAG, "ERROR_INSUFFICIENT_PERMISSIONS")
                                "Không có quyền truy cập microphone"
                            }
                            SpeechRecognizer.ERROR_NETWORK -> {
                                Log.e(TAG, "ERROR_NETWORK: Network error")
                                "Lỗi mạng. Kiểm tra kết nối internet."
                            }
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                                Log.e(TAG, "ERROR_NETWORK_TIMEOUT")
                                "Hết thời gian kết nối mạng"
                            }
                            SpeechRecognizer.ERROR_NO_MATCH -> {
                                Log.w(TAG, "ERROR_NO_MATCH: No speech detected or recognized")
                                // Try to use partial result if available
                                if (lastPartialResult.isNotEmpty()) {
                                    Log.d(TAG, "Using partial result despite NO_MATCH: $lastPartialResult")
                                    onFinalResult(lastPartialResult)
                                    return
                                }
                                "Không nhận diện được. Vui lòng nói rõ hơn."
                            }
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                                Log.e(TAG, "ERROR_RECOGNIZER_BUSY")
                                "Recognizer đang bận. Thử lại."
                            }
                            SpeechRecognizer.ERROR_SERVER -> {
                                Log.e(TAG, "ERROR_SERVER")
                                "Lỗi server Google Speech"
                            }
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                Log.w(TAG, "ERROR_SPEECH_TIMEOUT: No speech input")
                                "Không phát hiện giọng nói. Nói lớn hơn."
                            }
                            else -> {
                                Log.e(TAG, "Unknown error code: $error")
                                "Lỗi không xác định: $error"
                            }
                        }
                        
                        onError(errorMessage)
                    }
                    
                    override fun onResults(results: Bundle?) {
                        timeoutJob?.cancel()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        
                        Log.d(TAG, "✅ Final result received: '$text'")
                        Log.d(TAG, "All matches: $matches")
                        resultReceived = true
                        lastPartialResult = text
                        
                        if (text.isNotEmpty()) {
                            onFinalResult(text)
                        } else {
                            Log.w(TAG, "Empty final result")
                            onError("Không nhận diện được giọng nói")
                        }
                        
                        isListening = false
                    }
                    
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        
                        if (text.isNotEmpty() && text != lastPartialResult) {
                            // Only log and callback when text changes
                            Log.i(TAG, "💬 Partial: '$text'")
                            lastPartialResult = text
                            onPartialResult(text)
                        }
                    }
                    
                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Not used
                    }
                })
            }
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Try Vietnamese first, fallback to English if not available
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
                // Add fallback languages
                putStringArrayListExtra(
                    RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                    arrayListOf("vi-VN", "en-US", "vi")
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                // Increase silence timeout
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
                // Prefer online recognition for better accuracy
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }
            
            Log.d(TAG, "🎯 Starting listening with intent...")
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "🎯 startListening() called")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            onError("Không thể khởi động speech recognition: ${e.message}")
            isListening = false
        }
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        try {
            timeoutJob?.cancel()
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping", e)
        }
    }
    
    /**
     * Cancel and cleanup
     */
    fun cancel() {
        try {
            timeoutJob?.cancel()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            lastPartialResult = ""
            resultReceived = false
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling", e)
        }
    }
    
    fun isCurrentlyListening(): Boolean = isListening
}
