package com.example.todonotediary.data.remote

import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.utils.RetryHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.Date
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TodoRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_TODOS = "todos"
        private const val TAG = "TodoRemoteDataSource"
    }

    suspend fun getTodoById(todoId: String): TodoEntity? {
        return try {
            val document = firestore.collection(COLLECTION_TODOS)
                .document(todoId)
                .get()
                .await()

            document.toObject(TodoEntity::class.java)?.copy(
                id = document.id
            )
        } catch (e: Exception) {
            null
        }
    }

    // Lấy tất cả todos của user từ Firebase
    suspend fun getTodos(userId: String): Result<List<TodoEntity>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_TODOS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()

            val todos = snapshot.documents.mapNotNull { document ->
                document.toObject(TodoEntity::class.java)?.copy(
                    id = document.id
                )
            }
            Result.success(todos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveTodo(todo: TodoEntity): Result<TodoEntity> {
        return RetryHelper.retryWithExponentialBackoff {
            try {
                val currentTime = System.currentTimeMillis()
                val todoWithTimestamp = todo.copy(
                    lastSyncTimestamp = currentTime,
                    createdAt = if (todo.id.isEmpty()) currentTime else todo.createdAt,
                    updatedAt = currentTime
                )

                val todoRef = if (todo.id.isNotEmpty()) {
                    firestore.collection(COLLECTION_TODOS).document(todo.id)
                } else {
                    firestore.collection(COLLECTION_TODOS).document()
                }

                // Chuyển đổi TodoEntity thành Map với tên trường chính xác
                val todoMap = hashMapOf(
                    "userId" to todoWithTimestamp.userId,
                    "title" to todoWithTimestamp.title,
                    "description" to todoWithTimestamp.description,
                    "isCompleted" to todoWithTimestamp.isCompleted,
                    "createdAt" to todoWithTimestamp.createdAt,
                    "updatedAt" to todoWithTimestamp.updatedAt,
                    "startAt" to todoWithTimestamp.startAt,
                    "deadline" to todoWithTimestamp.deadline,
                    "lastSyncTimestamp" to todoWithTimestamp.lastSyncTimestamp,
                    "isDeleted" to todoWithTimestamp.isDeleted
                )

                todoRef.set(todoMap).await()
                todoWithTimestamp.copy(id = todoRef.id)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun deleteTodo(todoId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_TODOS)
                .document(todoId)
                .update("isDeleted", true, "lastSyncTimestamp", System.currentTimeMillis(), "updatedAt", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cập nhật trạng thái hoàn thành
    suspend fun updateTodoCompletionStatus(todoId: String, isCompleted: Boolean): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_TODOS)
                .document(todoId)
                .update(
                    "isCompleted", isCompleted,
                    "lastSyncTimestamp", System.currentTimeMillis(),
                    "updatedAt", System.currentTimeMillis()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}