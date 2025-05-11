package com.example.todonotediary.di

import com.example.todonotediary.data.repository.NoteRepositoryImpl
import com.example.todonotediary.domain.repository.TodoRepository
import com.example.todonotediary.data.repository.TodoRepositoryImpl
import com.example.todonotediary.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTodoRepository(
        impl: TodoRepositoryImpl
    ): TodoRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        impl: NoteRepositoryImpl
    ): NoteRepository
}
