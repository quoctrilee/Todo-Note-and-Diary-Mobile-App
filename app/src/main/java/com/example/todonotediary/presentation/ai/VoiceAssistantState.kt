package com.example.todonotediary.presentation.ai

/**
 * State for Voice Assistant feature
 */
data class VoiceAssistantState(
    val isVisible: Boolean = false,
    val status: VoiceStatus = VoiceStatus.IDLE,
    val partialText: String = "",
    val audioLevel: Float = 0f,
    val resultMessage: String = "",
    val errorMessage: String? = null,
    val isProcessing: Boolean = false
)

/**
 * Status of voice interaction
 */
enum class VoiceStatus {
    IDLE,           // Ready to listen
    LISTENING,      // Currently listening
    PROCESSING,     // Processing command
    SUCCESS,        // Command executed successfully
    ERROR           // Error occurred
}

/**
 * Events for Voice Assistant
 */
sealed class VoiceAssistantEvent {
    object OnMicClick : VoiceAssistantEvent()
    object OnCancel : VoiceAssistantEvent()
    object OnDismiss : VoiceAssistantEvent()
    data class OnPartialResult(val text: String) : VoiceAssistantEvent()
    data class OnFinalResult(val text: String) : VoiceAssistantEvent()
    data class OnAudioLevel(val level: Float) : VoiceAssistantEvent()
}

/**
 * UI Events for Voice Assistant
 */
sealed class VoiceAssistantUiEvent {
    data class ShowToast(val message: String) : VoiceAssistantUiEvent()
    object TodoAdded : VoiceAssistantUiEvent()
    object DismissSheet : VoiceAssistantUiEvent()
}
