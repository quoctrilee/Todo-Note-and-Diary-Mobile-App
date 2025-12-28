package com.example.todonotediary.data.remote

import android.util.Log
import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.utils.RetryHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class TodoRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_TODOS = "todos"
        private const val TAG = "TodoRemoteDataSource"
    }

    // Lấy danh sách todos theo userId
    suspend fun getTodos(userId: String): List<TodoEntity> {
        return try {
            val todos = firestore.collection(COLLECTION_TODOS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(TodoEntity::class.java)?.copy(
                        id = document.id
                    )
                }
            todos
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getStartAndEndOfDay(dateMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return startOfDay to endOfDay
    }

    suspend fun getTodoUpcoming(userId: String, selectedDate: Long): List<TodoEntity> {
        val (startOfDay, endOfDay) = getStartAndEndOfDay(selectedDate)
        val currentTime = System.currentTimeMillis()

        return try {

            // Lấy tất cả todos của người dùng mà chưa bị xóa
            val todos = firestore.collection(COLLECTION_TODOS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(TodoEntity::class.java)?.copy(id = document.id)
                }

            // Lọc danh sách todos cho upcoming:
            // - Công việc chưa hoàn thành
            // - Ngày được chọn nằm trong khoảng từ startAt đến deadline (so sánh theo ngày)
            // - Deadline chưa quá hạn (>= currentTime)
            val filteredTodos = todos.filter { todo ->
                val todoStartAt = todo.startAt ?: 0
                val todoDeadline = todo.deadline ?: 0

                // Ngày của todo.startAt (00:00:00)
                val todoStartDay = Calendar.getInstance().apply {
                    timeInMillis = todoStartAt
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                // Ngày của todo.deadline (23:59:59)
                val todoDeadlineDay = Calendar.getInstance().apply {
                    timeInMillis = todoDeadline
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                // End của ngày được chọn (23:59:59)
                val endOfSelectedDay = Calendar.getInstance().apply {
                    timeInMillis = startOfDay
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                // Kiểm tra: ngày được chọn nằm trong khoảng từ ngày bắt đầu đến ngày deadline
                val isInDateRange = startOfDay >= todoStartDay && endOfSelectedDay <= todoDeadlineDay

                // Điều kiện upcoming: chưa hoàn thành và deadline chưa quá hạn
                val isUpcoming = !todo.isCompleted && todoDeadline >= currentTime

                isInDateRange && isUpcoming
            }


            filteredTodos
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTodoPast(userId: String, selectedDate: Long): List<TodoEntity> {
        val currentTime = System.currentTimeMillis()

        return try {
            // Lấy tất cả todos của người dùng mà chưa bị xóa
            val todos = firestore.collection(COLLECTION_TODOS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(TodoEntity::class.java)?.copy(id = document.id)
                }

            // Lọc danh sách todos cho past:
            // - Công việc đã hoàn thành, HOẶC
            // - Deadline đã qua so với ngày hiện tại (không phải ngày được chọn)
            val filteredTodos = todos.filter { todo ->
                val todoDeadline = todo.deadline ?: 0

                // Điều kiện past: đã hoàn thành HOẶC deadline đã qua (< currentTime)
                todo.isCompleted || (todoDeadline > 0 && todoDeadline < currentTime)
            }

        filteredTodos
        } catch (e: Exception) {
            emptyList()
        }
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

    private fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        return dateFormat.format(Date(timestamp))
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
                .update("isDeleted", true, "lastSyncTimestamp", Date().time, "updatedAt", System.currentTimeMillis())
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
                    "lastSyncTimestamp", Date().time,
                    "updatedAt", System.currentTimeMillis()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}