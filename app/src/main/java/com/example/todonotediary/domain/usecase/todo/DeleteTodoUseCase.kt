package com.example.todonotediary.domain.usecase.todo

import com.example.todonotediary.domain.repository.TodoRepository
import javax.inject.Inject

class DeleteTodoUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    suspend operator fun invoke(todoId: String): Result<Unit> {
        return todoRepository.deleteTodo(todoId)
    }
}