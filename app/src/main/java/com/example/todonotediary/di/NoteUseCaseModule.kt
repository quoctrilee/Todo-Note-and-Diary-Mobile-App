package com.example.todonotediary.di

import com.example.todonotediary.domain.repository.NoteRepository
import com.example.todonotediary.domain.usecase.note.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NoteUseCaseModule {

    @Provides
    @Singleton
    fun provideNoteUseCases(
        repository: NoteRepository
    ): NoteUseCases {
        val getNotesUseCase = GetNotesUseCase(repository)
        val getNoteByIdUseCase = GetNoteByIdUseCase(repository)

        return NoteUseCases(
            getNotes = getNotesUseCase,
            getNoteById = getNoteByIdUseCase,
            addNote = AddNoteUseCase(repository),
            updateNote = UpdateNoteUseCase(repository),
            deleteNote = DeleteNoteUseCase(repository),
            syncNotes = SyncNotesUseCase(repository),
            getNotesByCategory = GetNotesByCategoryUseCase(getNotesUseCase)
        )
    }
}
