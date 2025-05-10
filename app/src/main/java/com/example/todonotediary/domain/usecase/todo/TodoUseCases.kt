package com.example.todonotediary.domain.usecase.todo

data class TodoUseCases(
    val getTodos: GetTodosUseCase,
    val getTodoById: GetTodoByIdUseCase,
    val getTodoUpcoming: GetTodoUpcomingUseCase,
    val getTodoPast: GetTodoPastUseCase,
    val addTodo: AddTodoUseCase,
    val updateTodo: UpdateTodoUseCase,
    val deleteTodo: DeleteTodoUseCase,
    val toggleTodoCompletion: ToggleTodoCompletionUseCase,
)