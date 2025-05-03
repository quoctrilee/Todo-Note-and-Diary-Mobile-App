package com.example.todonotediary.di

import com.example.todonotediary.domain.repository.TodoRepository
import com.example.todonotediary.domain.usecase.todo.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TodoUseCaseModule {

    @Provides
    @Singleton
    fun provideTodoUseCases(
        repository: TodoRepository
    ): TodoUseCases {
        val getTodoByIdUseCase = GetTodoByIdUseCase(repository)

        return TodoUseCases(
            getTodos = GetTodosUseCase(repository),
            getTodoById = getTodoByIdUseCase,
            addTodo = AddTodoUseCase(repository),
            updateTodo = UpdateTodoUseCase(repository),
            deleteTodo = DeleteTodoUseCase(repository),
            syncTodos = SyncTodosUseCase(repository),
            toggleTodoCompletion = ToggleTodoCompletionUseCase(repository, getTodoByIdUseCase)
        )
    }
}
