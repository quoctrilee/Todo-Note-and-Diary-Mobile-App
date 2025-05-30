package com.example.todonotediary.domain.usecase.todo

import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodoUpcomingUseCase @Inject constructor(
    private val todoRepository: TodoRepository
) {
    operator fun invoke(userId: String, selectedDate: Long): Flow<List<TodoEntity>> {
        return todoRepository.getTodoUpcoming(userId, selectedDate)
    }
}