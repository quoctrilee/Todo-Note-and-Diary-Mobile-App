package com.example.todonotediary.data.repository

import android.util.Log
import com.example.todonotediary.data.remote.TodoRemoteDataSource
import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val remoteDataSource: TodoRemoteDataSource
) : TodoRepository {

    override fun getTodos(userId: String): Flow<List<TodoEntity>> {
        return flow {
            val todos = remoteDataSource.getTodos(userId)
            emit(todos)
        }.catch { exception ->
            emit(emptyList())
        }
    }


    override fun getTodoUpcoming(userId: String, selectedDate: Long): Flow<List<TodoEntity>> {
        return flow {
            val todos = remoteDataSource.getTodoUpcoming(userId, selectedDate)
            emit(todos)
        }.catch { exception ->
            emit(emptyList())
        }
    }

    override fun getTodoPast(userId: String,selectedDate: Long): Flow<List<TodoEntity>> {
        return flow {
            val todos = remoteDataSource.getTodoPast(userId, selectedDate)
            emit(todos)
        }.catch { exception ->
            emit(emptyList())
        }
    }

    override suspend fun getTodoById(todoId: String): TodoEntity? {
        return remoteDataSource.getTodoById(todoId)
    }

    override suspend fun addTodo(todo: TodoEntity): Result<TodoEntity> {
        return remoteDataSource.saveTodo(todo)
    }

    override suspend fun updateTodo(todo: TodoEntity): Result<Unit> {
        return try {
            val result = remoteDataSource.saveTodo(todo)
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTodo(todoId: String): Result<Unit> {
        return remoteDataSource.deleteTodo(todoId)
    }

    override suspend fun updateTodoCompletionStatus(todoId: String, isCompleted: Boolean): Result<Unit> {
        return remoteDataSource.updateTodoCompletionStatus(todoId, isCompleted)
    }

}