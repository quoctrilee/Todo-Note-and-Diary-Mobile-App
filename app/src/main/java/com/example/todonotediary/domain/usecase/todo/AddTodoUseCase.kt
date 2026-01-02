package com.example.todonotediary.domain.usecase.todo

import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.repository.TodoRepository
import java.util.UUID
import javax.inject.Inject

class AddTodoUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    suspend operator fun invoke(
        userId: String,
        title: String,
        description: String,
        startAt: Long,
        deadline: Long
    ): Result<TodoEntity> {
        val currentTime = System.currentTimeMillis()
        val todo = TodoEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            description = description,
            startAt = startAt,
            deadline = deadline,
            isCompleted = false,
            isDeleted = false,
            createdAt = currentTime,
            updatedAt = currentTime,
            lastSyncTimestamp = 0L
        )
        return todoRepository.addTodo(todo)
    }
}
