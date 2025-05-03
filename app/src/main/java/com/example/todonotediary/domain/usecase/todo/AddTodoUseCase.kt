package com.example.todonotediary.domain.usecase.todo

import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.repository.TodoRepository
import java.util.UUID
import javax.inject.Inject

class AddTodoUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        userId: String,
        deadline: Long? = null
    ): Result<TodoEntity> {
        val todo = TodoEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            deadline = deadline,
            userId = userId,
            lastSyncTimestamp = 0,
            isDeleted = false
        )
        return todoRepository.addTodo(todo)
    }
}