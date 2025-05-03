package com.example.todonotediary.domain.usecase.todo

import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.repository.TodoRepository
import javax.inject.Inject

class UpdateTodoUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    suspend operator fun invoke(todo: TodoEntity): Result<Unit> {
        return todoRepository.updateTodo(todo)
    }
}