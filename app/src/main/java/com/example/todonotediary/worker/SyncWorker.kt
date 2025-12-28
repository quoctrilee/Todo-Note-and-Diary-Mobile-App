package com.example.todonotediary.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todonotediary.data.local.DiaryDao
import com.example.todonotediary.data.local.NoteDao
import com.example.todonotediary.data.local.TodoDao
import com.example.todonotediary.data.remote.DiaryRemoteDataSource
import com.example.todonotediary.data.remote.NoteRemoteDataSource
import com.example.todonotediary.data.remote.TodoRemoteDataSource
import com.example.todonotediary.domain.repository.AuthRepository
import com.example.todonotediary.utils.NetworkManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val todoDao: TodoDao,
    private val noteDao: NoteDao,
    private val diaryDao: DiaryDao,
    private val todoRemoteDataSource: TodoRemoteDataSource,
    private val noteRemoteDataSource: NoteRemoteDataSource,
    private val diaryRemoteDataSource: DiaryRemoteDataSource,
    private val authRepository: AuthRepository,
    private val networkManager: NetworkManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_pending_data"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check network first
            if (!networkManager.isCurrentlyOnline()) {
                Log.d(TAG, "No network available, skipping sync")
                return@withContext Result.retry()
            }

            // Get current user
            val userId = authRepository.getCrurrentUser()?.uid
            if (userId == null) {
                Log.d(TAG, "No user logged in, skipping sync")
                return@withContext Result.success()
            }

            Log.d(TAG, "Starting background sync for user: $userId")

            // Only upload pending changes - local is source of truth
            syncPendingTodos(userId)
            syncPendingNotes(userId)
            syncPendingDiaries(userId)

            Log.d(TAG, "Background sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during background sync", e)
            Result.retry()
        }
    }

    private suspend fun syncPendingTodos(userId: String) {
        try {
            // Get all todos for current user (including pending delete)
            val allLocalTodos = todoDao.getAllTodos()
            val userTodos = allLocalTodos.filter { it.userId == userId }
            
            // Process each pending todo one by one (only those with lastSyncTimestamp = 0)
            val pendingTodos = userTodos.filter { 
                !it.isDeleted && !it.pendingDelete && it.lastSyncTimestamp == 0L 
            }
            Log.d(TAG, "Uploading ${pendingTodos.size} pending todos for user: $userId")

            pendingTodos.forEach { todo ->
                try {
                    val result = todoRemoteDataSource.saveTodo(todo)
                    if (result.isSuccess) {
                        // Update lastSyncTimestamp to mark as synced
                        val syncedTodo = todo.copy(lastSyncTimestamp = System.currentTimeMillis())
                        todoDao.insertTodo(syncedTodo)
                        Log.d(TAG, "Uploaded todo: ${todo.id} (title: ${todo.title}, completed: ${todo.isCompleted})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload todo: ${todo.id}", e)
                }
            }

            // Process pending deletes one by one
            val pendingDeleteTodos = userTodos.filter { it.pendingDelete }
            Log.d(TAG, "Uploading ${pendingDeleteTodos.size} pending delete todos for user: $userId")

            pendingDeleteTodos.forEach { todo ->
                try {
                    val remoteResult = todoRemoteDataSource.deleteTodo(todo.id)
                    if (remoteResult.isSuccess) {
                        // Mark as fully deleted
                        val fullyDeletedTodo = todo.copy(
                            isDeleted = true,
                            pendingDelete = false
                        )
                        todoDao.insertTodo(fullyDeletedTodo)
                        Log.d(TAG, "Deleted todo from remote: ${todo.id}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete todo: ${todo.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing todos", e)
        }
    }

    private suspend fun syncPendingNotes(userId: String) {
        try {
            val localNotes = noteDao.getNotes(userId)
            val pendingNotes = localNotes.filter { !it.isDeleted }

            Log.d(TAG, "Syncing ${pendingNotes.size} pending notes for user: $userId")

            pendingNotes.forEach { note ->
                try {
                    noteRemoteDataSource.saveNote(note)
                    Log.d(TAG, "Synced note: ${note.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync note: ${note.id}", e)
                }
            }

            // Sync deleted notes
            val deletedNotes = localNotes.filter { it.isDeleted }
            deletedNotes.forEach { note ->
                try {
                    noteRemoteDataSource.deleteNote(note.id)
                    Log.d(TAG, "Synced deleted note: ${note.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync deleted note: ${note.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing notes", e)
        }
    }

    private suspend fun syncPendingDiaries(userId: String) {
        try {
            val localDiaries = diaryDao.getDiaries(userId)
            val pendingDiaries = localDiaries.filter { !it.isDeleted }

            Log.d(TAG, "Syncing ${pendingDiaries.size} pending diaries for user: $userId")

            pendingDiaries.forEach { diary ->
                try {
                    diaryRemoteDataSource.saveDiary(diary)
                    Log.d(TAG, "Synced diary: ${diary.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync diary: ${diary.id}", e)
                }
            }

            // Sync deleted diaries
            val deletedDiaries = localDiaries.filter { it.isDeleted }
            deletedDiaries.forEach { diary ->
                try {
                    diaryRemoteDataSource.deleteDiary(diary.id)
                    Log.d(TAG, "Synced deleted diary: ${diary.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync deleted diary: ${diary.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing diaries", e)
        }
    }
}
