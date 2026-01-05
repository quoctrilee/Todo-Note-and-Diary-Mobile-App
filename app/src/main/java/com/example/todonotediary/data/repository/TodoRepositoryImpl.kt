package com.example.todonotediary.data.repository

import android.util.Log
import com.example.todonotediary.data.local.TodoDao
import com.example.todonotediary.data.remote.TodoRemoteDataSource
import com.example.todonotediary.domain.model.TodoEntity
import com.example.todonotediary.domain.repository.TodoRepository
import com.example.todonotediary.utils.NetworkManager
import com.example.todonotediary.utils.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val localDataSource: TodoDao,
    private val remoteDataSource: TodoRemoteDataSource,
    private val networkManager: NetworkManager,
    private val syncManager: SyncManager
) : TodoRepository {

    // Cache last sync time to avoid too frequent syncs
    private var lastSyncTime = 0L
    private val syncIntervalMs = 30_000L // 30 seconds

    private fun shouldSync(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastSyncTime) > syncIntervalMs
    }

    override fun getTodos(userId: String): Flow<List<TodoEntity>> {
        return flow {
            // Trigger sync from remote first if online and not synced recently
            if (networkManager.isCurrentlyOnline() && shouldSync()) {
                try {
                    syncFromRemote(userId)
                    lastSyncTime = System.currentTimeMillis()
                    Log.d("TodoRepository", "Synced todos from Firebase before emitting")
                } catch (e: Exception) {
                    Log.e("TodoRepository", "Failed to sync from remote, continuing with local data", e)
                }
            }
            
            // Emit from local database (which now has updated data)
            localDataSource.getTodosFlow(userId)
                .collect { todos ->
                    emit(todos)
                }
        }.catch { exception ->
            Log.e("TodoRepository", "Error loading todos", exception)
            emit(emptyList())
        }
    }

    override fun getTodoUpcoming(userId: String, selectedDate: Long): Flow<List<TodoEntity>> {
        return flow {
            // Trigger sync from remote first if online and not synced recently
            if (networkManager.isCurrentlyOnline() && shouldSync()) {
                try {
                    syncFromRemote(userId)
                    lastSyncTime = System.currentTimeMillis()
                    Log.d("TodoRepository", "Synced todos from Firebase before getting upcoming")
                } catch (e: Exception) {
                    Log.e("TodoRepository", "Failed to sync from remote, continuing with local data", e)
                }
            }
            
            localDataSource.getTodosFlow(userId)
                .collect { todos ->
                    val upcomingTodos = filterUpcomingTodos(todos, selectedDate)
                    emit(upcomingTodos)
                }
        }.catch { exception ->
            Log.e("TodoRepository", "Error loading upcoming todos", exception)
            emit(emptyList())
        }
    }

    override fun getTodoPast(userId: String, selectedDate: Long): Flow<List<TodoEntity>> {
        return flow {
            // Trigger sync from remote first if online and not synced recently
            if (networkManager.isCurrentlyOnline() && shouldSync()) {
                try {
                    syncFromRemote(userId)
                    lastSyncTime = System.currentTimeMillis()
                    Log.d("TodoRepository", "Synced todos from Firebase before getting past")
                } catch (e: Exception) {
                    Log.e("TodoRepository", "Failed to sync from remote, continuing with local data", e)
                }
            }
            
            localDataSource.getTodosFlow(userId)
                .collect { todos ->
                    val pastTodos = filterPastTodos(todos, selectedDate)
                    emit(pastTodos)
                }
        }.catch { exception ->
            Log.e("TodoRepository", "Error loading past todos", exception)
            emit(emptyList())
        }
    }

    override suspend fun getTodoById(todoId: String): TodoEntity? {
        // Try local first
        val localTodo = localDataSource.getTodoById(todoId)
        if (localTodo != null) return localTodo

        // Fallback to remote
        return try {
            remoteDataSource.getTodoById(todoId)
        } catch (e: Exception) {
            Log.e("TodoRepository", "Error getting todo by id", e)
            null
        }
    }

    override suspend fun addTodo(todo: TodoEntity): Result<TodoEntity> {
        return try {
            // 1. Save to local first
            localDataSource.insertTodo(todo)

            // 2. Sync to remote only if online
            if (networkManager.isCurrentlyOnline()) {
                val remoteResult = remoteDataSource.saveTodo(todo)
                if (remoteResult.isSuccess) {
                    // Reset sync cache to force refresh next time
                    lastSyncTime = 0L
                    Result.success(todo)
                } else {
                    // Keep in local, schedule sync for later
                    syncManager.scheduleSyncNow()
                    Result.success(todo)
                }
            } else {
                // Offline: schedule sync when network returns
                syncManager.schedulePeriodicSync()
                Result.success(todo)
            }
        } catch (e: Exception) {
            Log.e("TodoRepository", "Error adding todo", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTodo(todo: TodoEntity): Result<Unit> {
        return try {
            // 1. Update local first
            val updatedTodo = todo.copy(updatedAt = System.currentTimeMillis())
            localDataSource.updateTodo(updatedTodo)

            // 2. Sync to remote only if online
            if (networkManager.isCurrentlyOnline()) {
                val remoteResult = remoteDataSource.saveTodo(updatedTodo)
                if (remoteResult.isSuccess) {
                    // Reset sync cache to force refresh next time
                    lastSyncTime = 0L
                    Result.success(Unit)
                } else {
                    // Keep local changes, schedule sync for later
                    syncManager.scheduleSyncNow()
                    Result.success(Unit)
                }
            } else {
                // Offline: schedule sync when network returns
                syncManager.schedulePeriodicSync()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("TodoRepository", "Error updating todo", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTodo(todoId: String): Result<Unit> {
        return try {
            // 1. Mark as pending delete in local
            val localTodo = localDataSource.getTodoById(todoId)
            if (localTodo != null) {
                val pendingDeleteTodo = localTodo.copy(
                    pendingDelete = true,
                    updatedAt = System.currentTimeMillis()
                )
                localDataSource.updateTodo(pendingDeleteTodo)
            }

            // 2. Try to delete from remote if online
            if (networkManager.isCurrentlyOnline()) {
                val remoteResult = remoteDataSource.deleteTodo(todoId)
                if (remoteResult.isSuccess) {
                    // Mark as fully deleted
                    if (localTodo != null) {
                        val fullyDeletedTodo = localTodo.copy(
                            isDeleted = true,
                            pendingDelete = false,
                            updatedAt = System.currentTimeMillis()
                        )
                        localDataSource.updateTodo(fullyDeletedTodo)
                    }
                    Result.success(Unit)
                } else {
                    // Keep as pending delete, schedule sync
                    syncManager.scheduleSyncNow()
                    Result.success(Unit)
                }
            } else {
                // Offline: keep as pending delete, schedule sync when network returns
                syncManager.schedulePeriodicSync()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("TodoRepository", "Error deleting todo", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTodoCompletionStatus(todoId: String, isCompleted: Boolean): Result<Unit> {
        return try {
            // Update local first with pending sync marker
            val localTodo = localDataSource.getTodoById(todoId)
            if (localTodo != null) {
                val updatedTodo = localTodo.copy(
                    isCompleted = isCompleted,
                    updatedAt = System.currentTimeMillis(),
                    lastSyncTimestamp = 0L // Mark as pending sync
                )
                localDataSource.updateTodo(updatedTodo)
                Log.d("TodoRepository", "Marked todo completion as pending: $todoId, completed=$isCompleted")
            }

            // Schedule background sync
            syncManager.scheduleSyncNow()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TodoRepository", "Error updating completion status", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFromRemote(userId: String): Result<Unit> {
        return try {
            if (!networkManager.isCurrentlyOnline()) {
                Log.d("TodoRepository", "No network, skipping remote sync")
                return Result.success(Unit)
            }

            // 1. Fetch all todos from Firebase
            val remoteResult = remoteDataSource.getTodos(userId)
            if (remoteResult.isFailure) {
                Log.e("TodoRepository", "Failed to fetch todos from Firebase", remoteResult.exceptionOrNull())
                return Result.failure(remoteResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val remoteTodos = remoteResult.getOrNull() ?: emptyList()
            Log.d("TodoRepository", "Fetched ${remoteTodos.size} todos from Firebase")

            // 2. Get all local todos
            val localTodos = localDataSource.getAllTodos()
            val userLocalTodos = localTodos.filter { it.userId == userId }
            
            // 3. Merge logic: Firebase is source of truth for synced data
            remoteTodos.forEach { remoteTodo ->
                val localTodo = userLocalTodos.find { it.id == remoteTodo.id }
                
                if (localTodo == null) {
                    // New todo from Firebase - insert to local
                    localDataSource.insertTodo(remoteTodo)
                    Log.d("TodoRepository", "Inserted new todo from Firebase: ${remoteTodo.id}")
                } else {
                    // Todo exists locally - check if we should update
                    // Only update if remote is newer AND local doesn't have pending changes
                    val hasLocalPendingChanges = localTodo.lastSyncTimestamp == 0L || localTodo.pendingDelete
                    
                    if (!hasLocalPendingChanges && remoteTodo.updatedAt > localTodo.updatedAt) {
                        // Remote is newer and local has no pending changes - update local
                        localDataSource.insertTodo(remoteTodo)
                        Log.d("TodoRepository", "Updated todo from Firebase: ${remoteTodo.id}")
                    } else if (hasLocalPendingChanges) {
                        Log.d("TodoRepository", "Skipping update for todo ${remoteTodo.id} - has pending local changes")
                    }
                }
            }

            Log.d("TodoRepository", "Sync from remote completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("TodoRepository", "Error syncing from remote", e)
            Result.failure(e)
        }
    }

    // Helper functions to filter todos locally
    private fun filterUpcomingTodos(todos: List<TodoEntity>, selectedDate: Long): List<TodoEntity> {
        val currentTime = System.currentTimeMillis()

        // Tính start và end của ngày được chọn (00:00 - 23:59:59)
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfSelectedDay = calendar.timeInMillis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        val endOfSelectedDay = calendar.timeInMillis

        return todos.filter { todo ->
            val todoStartAt = todo.startAt ?: 0L
            val todoDeadline = todo.deadline // Keep nullable

            // Ngày của todo.startAt (00:00:00)
            val todoStartDay = java.util.Calendar.getInstance().apply {
                timeInMillis = todoStartAt
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Kiểm tra ngày: nếu có deadline, kiểm tra range; nếu không, chỉ kiểm tra startAt
            val isInDateRange = if (todoDeadline != null) {
                // Ngày của todo.deadline (23:59:59)
                val todoDeadlineDay = java.util.Calendar.getInstance().apply {
                    timeInMillis = todoDeadline
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }.timeInMillis
                // Kiểm tra: ngày được chọn nằm trong khoảng từ ngày bắt đầu đến ngày deadline
                startOfSelectedDay >= todoStartDay && endOfSelectedDay <= todoDeadlineDay
            } else {
                // Không có deadline, chỉ kiểm tra startAt có nằm trong ngày được chọn
                todoStartDay == startOfSelectedDay
            }

            // Điều kiện upcoming: (cả completed lẫn chưa completed) vẫn hiển thị trong upcoming
            // cho đến khi deadline đã qua. Chỉ loại bỏ todo đã xóa.
            val isUpcoming = !todo.isDeleted && (todoDeadline == null || todoDeadline >= currentTime)

            isInDateRange && isUpcoming
        }.sortedWith(compareBy(
            { it.startAt ?: Long.MAX_VALUE },
            { it.deadline ?: Long.MAX_VALUE }
        ))
    }

    private fun filterPastTodos(todos: List<TodoEntity>, selectedDate: Long): List<TodoEntity> {
        val currentTime = System.currentTimeMillis()

        // Past không cần filter theo selectedDate - hiển thị tất cả past todos
        return todos.filter { todo ->
            val todoDeadline = todo.deadline // Keep nullable

            // Điều kiện past: chưa xóa VÀ có deadline VÀ deadline đã qua
            !todo.isDeleted && todoDeadline != null && todoDeadline < currentTime
        }.sortedWith(
            compareBy<TodoEntity> { it.deadline ?: Long.MAX_VALUE }
        )
    }
}
