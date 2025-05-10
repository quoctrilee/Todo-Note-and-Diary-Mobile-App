package com.example.todonotediary.domain.repository

import com.example.todonotediary.domain.model.TodoEntity
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun getTodos(userId: String): Flow<List<TodoEntity>>

    fun getTodoUpcoming(userId: String,selectedDate: Long): Flow<List<TodoEntity>>

    fun getTodoPast(userId: String,selectedDate: Long): Flow<List<TodoEntity>>

    suspend fun getTodoById(todoId: String): TodoEntity?

    suspend fun addTodo(todo: TodoEntity): Result<TodoEntity>

    suspend fun updateTodo(todo: TodoEntity): Result<Unit>

    suspend fun deleteTodo(todoId: String): Result<Unit>

    suspend fun updateTodoCompletionStatus(todoId: String, isCompleted: Boolean): Result<Unit>
}