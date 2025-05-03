package com.example.todonotediary.domain.usecase.todo

data class TodoUseCases(
    val getTodos: GetTodosUseCase,
    val getTodoById: GetTodoByIdUseCase,
    val addTodo: AddTodoUseCase,
    val updateTodo: UpdateTodoUseCase,
    val deleteTodo: DeleteTodoUseCase,
    val syncTodos: SyncTodosUseCase,
    val toggleTodoCompletion: ToggleTodoCompletionUseCase
)
