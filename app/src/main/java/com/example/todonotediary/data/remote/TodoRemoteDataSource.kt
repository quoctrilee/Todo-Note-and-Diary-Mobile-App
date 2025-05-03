package com.example.todonotediary.data.remote

import com.example.todonotediary.domain.model.TodoEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class TodoRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_TODOS = "todos"
    }

    // Lấy danh sách todos theo userId
    suspend fun getTodos(userId: String): List<TodoEntity> {
        return try {
            firestore.collection(COLLECTION_TODOS)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(TodoEntity::class.java)?.copy(
                        id = document.id
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Lấy một todo theo id
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

    // Thêm hoặc cập nhật todo
    suspend fun saveTodo(todo: TodoEntity): Result<TodoEntity> {
        return try {
            val todoWithTimestamp = todo.copy(
                lastSyncTimestamp = Date().time
            )

            val todoRef = if (todo.id.isNotEmpty()) {
                firestore.collection(COLLECTION_TODOS).document(todo.id)
            } else {
                firestore.collection(COLLECTION_TODOS).document()
            }

            todoRef.set(todoWithTimestamp).await()

            Result.success(todoWithTimestamp.copy(id = todoRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Xóa todo
    suspend fun deleteTodo(todoId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_TODOS)
                .document(todoId)
                .update("isDeleted", true, "lastSyncTimestamp", Date().time)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Lấy các todo đã được thay đổi sau lastSyncTimestamp
    suspend fun getTodosUpdatedAfter(userId: String, lastSyncTimestamp: Long): List<TodoEntity> {
        return try {
            firestore.collection(COLLECTION_TODOS)
                .whereEqualTo("userId", userId)
                .whereGreaterThan("lastSyncTimestamp", lastSyncTimestamp)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(TodoEntity::class.java)?.copy(
                        id = document.id
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
