package com.example.todonotediary.data.repository

import com.example.todonotediary.data.local.TodoDao
import com.example.todonotediary.data.remote.TodoRemoteDataSource
import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class TodoRepositoryImpl(private val todoDao: TodoDao,
                         private val remoteDataSource: TodoRemoteDataSource) : TodoRepository {

    override fun getTodos(userId: String): Flow<List<TodoEntity>> {
        return flow {
            emit(todoDao.getTodos(userId)) // Lấy dữ liệu từ database
        }.catch { exception ->
            emit(emptyList()) // Xử lý lỗi nếu có
        }
    }

    override suspend fun getTodoById(todoId: String): TodoEntity? {
        return todoDao.getTodoById(todoId)
    }

    override suspend fun addTodo(todo: TodoEntity): Result<TodoEntity> {
        return try {
            todoDao.insertTodo(todo)
            Result.success(todo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTodo(todo: TodoEntity): Result<Unit> {
        return try {
            val updated = todo.copy(updatedAt = System.currentTimeMillis())
            todoDao.updateTodo(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTodo(todoId: String): Result<Unit> {
        return try {
            val todo = todoDao.getTodoById(todoId)
                ?: return Result.failure(Exception("Todo not found"))

            val softDeleted = todo.copy(updatedAt = System.currentTimeMillis(), isDeleted = true, lastSyncTimestamp = System.currentTimeMillis())
            todoDao.updateTodo(softDeleted)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun syncTodos(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        return try {
            // 1. Đẩy dữ liệu local chưa được đồng bộ lên Firestore
            val localTodosToSync = todoDao.getTodosToSync(userId)
            for (todo in localTodosToSync) {
                val syncedTodo = remoteDataSource.saveTodo(todo).getOrNull()
                if (syncedTodo != null) {
                    // Cập nhật lại lastSyncTimestamp trong local
                    todoDao.insertTodo(syncedTodo.copy(lastSyncTimestamp = System.currentTimeMillis()))
                }
            }
            // 2. Lấy các bản ghi mới từ Firestore cập nhật về local
            val remoteUpdates = remoteDataSource.getTodosUpdatedAfter(userId, lastSyncTimestamp)
            for (remoteTodo in remoteUpdates) {
                todoDao.insertTodo(remoteTodo)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}