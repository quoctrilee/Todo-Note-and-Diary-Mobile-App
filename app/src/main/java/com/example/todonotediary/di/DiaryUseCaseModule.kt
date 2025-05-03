package com.example.todonotediary.di

import com.example.todonotediary.domain.repository.DiaryRepository
import com.example.todonotediary.domain.usecase.diary.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiaryUseCaseModule {

    @Provides
    @Singleton
    fun provideDiaryUseCases(
        repository: DiaryRepository
    ): DiaryUseCases {
        val getDiariesUseCase = GetDiariesUseCase(repository)
        val getDiaryByIdUseCase = GetDiaryByIdUseCase(repository)

        return DiaryUseCases(
            getDiaries = getDiariesUseCase,
            getDiaryById = getDiaryByIdUseCase,
            addDiary = AddDiaryUseCase(repository),
            updateDiary = UpdateDiaryUseCase(repository),
            deleteDiary = DeleteDiaryUseCase(repository),
            syncDiaries = SyncDiariesUseCase(repository)
        )
    }
}
