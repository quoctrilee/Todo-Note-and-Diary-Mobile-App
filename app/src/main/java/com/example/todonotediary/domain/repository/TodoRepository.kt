package com.example.todonotediary.domain.repository

import com.example.todonotediary.domain.model.TodoEntity
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getTodos(userId: String): Flow<List<TodoEntity>>
    suspend fun getTodoById(todoId: String): TodoEntity?
    suspend fun addTodo(todo: TodoEntity): Result<TodoEntity>
    suspend fun updateTodo(todo: TodoEntity): Result<Unit>
    suspend fun deleteTodo(todoId: String): Result<Unit>
    suspend fun syncTodos(userId: String, lastSyncTimestamp: Long): Result<Unit>
}