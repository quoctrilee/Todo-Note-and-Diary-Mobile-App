package com.example.todonotediary.data.remote

import android.util.Log
import com.example.todonotediary.domain.model.TodoEntity
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

            // Lọc danh sách todos theo ngày đã chọn và điều kiện upcoming (deadline >= currentTime)
            val filteredTodos = todos.filter { todo ->
                val todoStartAt = todo.startAt ?: 0
                val todoDeadline = todo.deadline ?: Long.MAX_VALUE
                val todoIsCompleted = todo.isCompleted
                val belongsToSelectedDay = (todoStartAt >= startOfDay && todoStartAt < endOfDay) ||
                        (todoDeadline >= startOfDay && todoDeadline < endOfDay)

                // Điều kiện upcoming: hạn chưa kết thúc hoặc chưa hoàn thành
                val isUpcoming = todoDeadline >= currentTime && todoIsCompleted == false

                belongsToSelectedDay && isUpcoming
            }


            filteredTodos
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTodoPast(userId: String, selectedDate: Long): List<TodoEntity> {
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

            // Lọc danh sách todos theo ngày đã chọn và điều kiện past (deadline < currentTime)
            val filteredTodos = todos.filter { todo ->
                val todoStartAt = todo.startAt ?: 0
                val todoDeadline = todo.deadline ?: 0  // Nếu không có deadline, coi như đã quá hạn
                val todoIsCompleted = todo.isCompleted
                val belongsToSelectedDay = (todoStartAt >= startOfDay && todoStartAt < endOfDay) ||
                        (todoDeadline >= startOfDay && todoDeadline < endOfDay)

                // Điều kiện past: hạn đã kết thúc
                val isPast = todoDeadline > 0 && todoDeadline < currentTime || todo.isCompleted == true

                belongsToSelectedDay && isPast
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
        return try {
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
                "isCompleted" to todoWithTimestamp.isCompleted,  // Sử dụng tên trường chính xác
                "createdAt" to todoWithTimestamp.createdAt,
                "updatedAt" to todoWithTimestamp.updatedAt,
                "startAt" to todoWithTimestamp.startAt,
                "deadline" to todoWithTimestamp.deadline,
                "lastSyncTimestamp" to todoWithTimestamp.lastSyncTimestamp,
                "isDeleted" to todoWithTimestamp.isDeleted  // Sử dụng tên trường chính xác
            )

            todoRef.set(todoMap).await()

            Result.success(todoWithTimestamp.copy(id = todoRef.id))
        } catch (e: Exception) {
            Result.failure(e)
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