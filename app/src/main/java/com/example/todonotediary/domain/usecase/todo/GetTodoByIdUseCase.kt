package com.example.todonotediary.domain.usecase.todo

import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.repository.TodoRepository
import javax.inject.Inject

class GetTodoByIdUseCase @Inject constructor(
    private val todoRepository: TodoRepository
){
    suspend operator fun invoke(todoId: String): TodoEntity?{
        return todoRepository.getTodoById(todoId)
    }
}