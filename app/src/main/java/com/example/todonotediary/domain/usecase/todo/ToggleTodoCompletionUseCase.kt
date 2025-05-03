package com.example.todonotediary.domain.usecase.todo

import com.example.todonotediary.domain.repository.TodoRepository
import javax.inject.Inject

class ToggleTodoCompletionUseCase @Inject constructor(
    private val todoRepository: TodoRepository,
    private val getTodoByIdUseCase: GetTodoByIdUseCase
) {
    suspend operator fun invoke(todoId: String): Result<Unit> {
        val todo = getTodoByIdUseCase(todoId) ?: return Result.failure(Exception("Todo not found"))
        val updatedTodo = todo.copy(isCompleted = !todo.isCompleted)
        return todoRepository.updateTodo(updatedTodo)
    }
}