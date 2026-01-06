package com.example.todonotediary.presentation.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todonotediary.domain.model.CommandIntent
import com.example.todonotediary.domain.usecase.ai.AIUseCases
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.example.todonotediary.domain.usecase.todo.TodoUseCases
import com.example.todonotediary.utils.NetworkManager
import com.example.todonotediary.utils.SpeechRecognizerHelper
import com.example.todonotediary.utils.TextToSpeechHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
    private val aiUseCases: AIUseCases,
    private val authUseCases: AuthUseCases,
    private val speechHelper: SpeechRecognizerHelper,
    private val ttsHelper: TextToSpeechHelper,
    private val networkManager: NetworkManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "VoiceAssistantVM"
    }
    
    private val _state = MutableStateFlow(VoiceAssistantState())
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()
    
    private val _uiEvent = MutableSharedFlow<VoiceAssistantUiEvent>()
    val uiEvent: SharedFlow<VoiceAssistantUiEvent> = _uiEvent.asSharedFlow()
    
    fun onEvent(event: VoiceAssistantEvent) {
        when (event) {
            is VoiceAssistantEvent.OnMicClick -> handleMicClick()
            is VoiceAssistantEvent.OnCancel -> handleCancel()
            is VoiceAssistantEvent.OnDismiss -> handleDismiss()
            is VoiceAssistantEvent.OnPartialResult -> handlePartialResult(event.text)
            is VoiceAssistantEvent.OnFinalResult -> handleFinalResult(event.text)
            is VoiceAssistantEvent.OnAudioLevel -> handleAudioLevel(event.level)
        }
    }
    
    private fun handleMicClick() {
        if (!networkManager.isCurrentlyOnline()) {
            _state.value = _state.value.copy(
                status = VoiceStatus.ERROR,
                errorMessage = "Cần kết nối internet để sử dụng AI Assistant",
                isVisible = true
            )
            return
        }
        
        // Check auth
        val currentUser = authUseCases.getCurrentUser()
        if (currentUser == null) {
            _state.value = _state.value.copy(
                status = VoiceStatus.ERROR,
                errorMessage = "Bạn cần đăng nhập để sử dụng tính năng này",
                isVisible = true
            )
            return
        }
        
        startListening()
    }
    
    private fun startListening() {
        Log.d(TAG, "Đang bắt đầu nghe...")
        _state.value = VoiceAssistantState(
            isVisible = true,
            status = VoiceStatus.LISTENING,
            partialText = "",
            audioLevel = 0f
        )
        
        speechHelper.startListening(
            onPartialResult = { text ->
                // Only update state, reduce logging
                onEvent(VoiceAssistantEvent.OnPartialResult(text))
            },
            onFinalResult = { text ->
                Log.d(TAG, "✅ Final: '$text'")
                onEvent(VoiceAssistantEvent.OnFinalResult(text))
            },
            onAudioLevel = { level ->
                onEvent(VoiceAssistantEvent.OnAudioLevel(level))
            },
            onError = { error ->
                Log.e(TAG, "❌ $error")
                _state.value = _state.value.copy(
                    status = VoiceStatus.ERROR,
                    errorMessage = error
                )
                
                viewModelScope.launch {
                    delay(3000)
                    handleDismiss()
                }
            }
        )
    }
    
    private fun handlePartialResult(text: String) {
        // Only update state without logging to reduce overhead
        _state.value = _state.value.copy(
            partialText = text
        )
    }
    
    private fun handleFinalResult(text: String) {
        if (text.isBlank()) {
            _state.value = _state.value.copy(
                status = VoiceStatus.ERROR,
                errorMessage = "Không nhận được giọng nói"
            )
            viewModelScope.launch {
                delay(2000)
                handleDismiss()
            }
            return
        }
        
        _state.value = _state.value.copy(
            status = VoiceStatus.PROCESSING,
            partialText = text,
            isProcessing = true
        )
        
        // Don't call stopListening() - speech recognizer already stopped when final result arrives
        processVoiceCommand(text)
    }
    
    private fun handleAudioLevel(level: Float) {
        _state.value = _state.value.copy(
            audioLevel = level
        )
    }
    
    private fun handleCancel() {
        speechHelper.cancel()
        ttsHelper.stop()
        
        _state.value = VoiceAssistantState(
            isVisible = false,
            status = VoiceStatus.IDLE
        )
    }
    
    private fun handleDismiss() {
        speechHelper.cancel()
        ttsHelper.stop()
        
        _state.value = VoiceAssistantState(
            isVisible = false,
            status = VoiceStatus.IDLE
        )
        
        viewModelScope.launch {
            _uiEvent.emit(VoiceAssistantUiEvent.DismissSheet)
        }
    }
    
    private fun processVoiceCommand(text: String) {
        viewModelScope.launch {
            try {
                val result = aiUseCases.processVoiceCommand(text)
                
                result.fold(
                    onSuccess = { command ->
                        Log.d(TAG, "✅ ${command.intent}: ${command.responseText}")
                        
                        when (command.intent) {
                            CommandIntent.ADD_TODO -> {
                                _state.value = _state.value.copy(
                                    status = VoiceStatus.SUCCESS,
                                    resultMessage = command.responseText,
                                    isProcessing = false
                                )
                                
                                // Emit success event first to refresh UI
                                _uiEvent.emit(VoiceAssistantUiEvent.TodoAdded)
                                
                                // Speak result and dismiss when done
                                ttsHelper.speak(command.responseText) {
                                    viewModelScope.launch {
                                        delay(1000) // Wait for speech to fully finish
                                        handleDismiss()
                                    }
                                }
                            }
                            CommandIntent.QUERY_TODOS -> {
                                _state.value = _state.value.copy(
                                    status = VoiceStatus.SUCCESS,
                                    resultMessage = command.responseText,
                                    isProcessing = false
                                )
                                
                                ttsHelper.speak(command.responseText) {
                                    viewModelScope.launch {
                                        delay(1000)
                                        handleDismiss()
                                    }
                                }
                            }
                            CommandIntent.COMPLETE_TODO -> {
                                _state.value = _state.value.copy(
                                    status = VoiceStatus.SUCCESS,
                                    resultMessage = command.responseText,
                                    isProcessing = false
                                )
                                
                                ttsHelper.speak(command.responseText) {
                                    viewModelScope.launch {
                                        delay(1000)
                                        handleDismiss()
                                    }
                                }
                            }
                            CommandIntent.GENERAL_QUESTION -> {
                                _state.value = _state.value.copy(
                                    status = VoiceStatus.SUCCESS,
                                    resultMessage = command.responseText,
                                    isProcessing = false
                                )
                                
                                ttsHelper.speak(command.responseText) {
                                    viewModelScope.launch {
                                        delay(1000)
                                        handleDismiss()
                                    }
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        _state.value = _state.value.copy(
                            status = VoiceStatus.ERROR,
                            errorMessage = error.message ?: "Có lỗi xảy ra",
                            isProcessing = false
                        )
                        
                        delay(3000)
                        handleDismiss()
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    status = VoiceStatus.ERROR,
                    errorMessage = "Lỗi: ${e.message}",
                    isProcessing = false
                )
                
                delay(3000)
                handleDismiss()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        speechHelper.cancel()
        ttsHelper.stop()
    }
}
