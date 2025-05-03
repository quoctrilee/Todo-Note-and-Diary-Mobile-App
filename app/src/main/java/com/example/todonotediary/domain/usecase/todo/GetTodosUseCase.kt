package com.example.todonotediary.domain.usecase.todo

import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodosUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    operator fun invoke(userId: String) : Flow<List<TodoEntity>>{
        return todoRepository.getTodos(userId)
    }
}