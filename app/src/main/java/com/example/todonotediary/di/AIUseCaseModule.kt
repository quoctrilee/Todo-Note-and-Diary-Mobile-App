package com.example.todonotediary.di

import com.example.todonotediary.domain.repository.AIRepository
import com.example.todonotediary.domain.usecase.ai.AIUseCases
import com.example.todonotediary.domain.usecase.ai.ProcessVoiceCommandUseCase
import com.example.todonotediary.domain.usecase.auth.AuthUseCases
import com.example.todonotediary.domain.usecase.todo.TodoUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object AIUseCaseModule {
    
    @Provides
    fun provideProcessVoiceCommandUseCase(
        aiRepository: AIRepository,
        todoUseCases: TodoUseCases,
        authUseCases: AuthUseCases
    ): ProcessVoiceCommandUseCase {
        return ProcessVoiceCommandUseCase(
            aiRepository = aiRepository,
            todoUseCases = todoUseCases,
            authUseCases = authUseCases
        )
    }
    
    @Provides
    fun provideAIUseCases(
        processVoiceCommand: ProcessVoiceCommandUseCase
    ): AIUseCases {
        return AIUseCases(
            processVoiceCommand = processVoiceCommand
        )
    }
}
