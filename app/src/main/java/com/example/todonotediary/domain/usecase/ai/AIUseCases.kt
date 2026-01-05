package com.example.todonotediary.domain.usecase.ai

import javax.inject.Inject

/**
 * Container for all AI-related use cases
 */
data class AIUseCases @Inject constructor(
    val processVoiceCommand: ProcessVoiceCommandUseCase
)
