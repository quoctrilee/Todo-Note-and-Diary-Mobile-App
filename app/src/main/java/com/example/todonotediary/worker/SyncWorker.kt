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

            // Two-way sync: Download from Firebase first, then upload pending changes
            downloadFromRemote(userId)
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

    private suspend fun downloadFromRemote(userId: String) {
        try {
            Log.d(TAG, "Downloading data from Firebase for user: $userId")
            
            // Download todos from Firebase
            val todosResult = todoRemoteDataSource.getTodos(userId)
            if (todosResult.isSuccess) {
                val remoteTodos = todosResult.getOrNull() ?: emptyList()
                Log.d(TAG, "Downloaded ${remoteTodos.size} todos from Firebase")
                
                // Get local todos to compare
                val localTodos = todoDao.getAllTodos().filter { it.userId == userId }
                
                remoteTodos.forEach { remoteTodo ->
                    val localTodo = localTodos.find { it.id == remoteTodo.id }
                    
                    if (localTodo == null) {
                        // New from Firebase - insert
                        todoDao.insertTodo(remoteTodo)
                        Log.d(TAG, "Inserted new todo from Firebase: ${remoteTodo.id}")
                    } else {
                        // Check if we should update
                        val hasLocalPendingChanges = localTodo.lastSyncTimestamp == 0L || localTodo.pendingDelete
                        
                        if (!hasLocalPendingChanges && remoteTodo.updatedAt > localTodo.updatedAt) {
                            todoDao.insertTodo(remoteTodo)
                            Log.d(TAG, "Updated todo from Firebase: ${remoteTodo.id}")
                        }
                    }
                }
            }
            
            // Download notes from Firebase
            val notesResult = noteRemoteDataSource.getNotes(userId)
            if (notesResult.isSuccess) {
                val remoteNotes = notesResult.getOrNull() ?: emptyList()
                Log.d(TAG, "Downloaded ${remoteNotes.size} notes from Firebase")
                
                val localNotes = noteDao.getAllNotes().filter { it.userId == userId }
                
                remoteNotes.forEach { remoteNote ->
                    val localNote = localNotes.find { it.id == remoteNote.id }
                    
                    if (localNote == null) {
                        noteDao.insertNote(remoteNote)
                        Log.d(TAG, "Inserted new note from Firebase: ${remoteNote.id}")
                    } else {
                        val hasLocalPendingChanges = localNote.lastSyncTimestamp == 0L || localNote.pendingDelete
                        
                        if (!hasLocalPendingChanges && remoteNote.updatedAt > localNote.updatedAt) {
                            noteDao.insertNote(remoteNote)
                            Log.d(TAG, "Updated note from Firebase: ${remoteNote.id}")
                        }
                    }
                }
            }
            
            // Download diaries from Firebase
            val diariesResult = diaryRemoteDataSource.getDiaries(userId)
            if (diariesResult.isSuccess) {
                val remoteDiaries = diariesResult.getOrNull() ?: emptyList()
                Log.d(TAG, "Downloaded ${remoteDiaries.size} diaries from Firebase")
                
                val localDiaries = diaryDao.getAllDiaries().filter { it.userId == userId }
                
                remoteDiaries.forEach { remoteDiary ->
                    val localDiary = localDiaries.find { it.id == remoteDiary.id }
                    
                    if (localDiary == null) {
                        diaryDao.insertDiary(remoteDiary)
                        Log.d(TAG, "Inserted new diary from Firebase: ${remoteDiary.id}")
                    } else {
                        val hasLocalPendingChanges = localDiary.lastSyncTimestamp == 0L || localDiary.pendingDelete
                        
                        if (!hasLocalPendingChanges && remoteDiary.updatedAt > localDiary.updatedAt) {
                            diaryDao.insertDiary(remoteDiary)
                            Log.d(TAG, "Updated diary from Firebase: ${remoteDiary.id}")
                        }
                    }
                }
            }
            
            Log.d(TAG, "Download from Firebase completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading from Firebase", e)
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
            // Get all notes for current user (including pending delete)
            val allLocalNotes = noteDao.getAllNotes()
            val userNotes = allLocalNotes.filter { it.userId == userId }
            
            // Process each pending note one by one (only those with lastSyncTimestamp = 0)
            val pendingNotes = userNotes.filter { 
                !it.isDeleted && !it.pendingDelete && it.lastSyncTimestamp == 0L 
            }
            Log.d(TAG, "Uploading ${pendingNotes.size} pending notes for user: $userId")

            pendingNotes.forEach { note ->
                try {
                    val result = noteRemoteDataSource.saveNote(note)
                    if (result.isSuccess) {
                        // Update lastSyncTimestamp to mark as synced
                        val syncedNote = note.copy(lastSyncTimestamp = System.currentTimeMillis())
                        noteDao.insertNote(syncedNote)
                        Log.d(TAG, "Uploaded note: ${note.id} (title: ${note.title})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload note: ${note.id}", e)
                }
            }

            // Process pending deletes one by one
            val pendingDeleteNotes = userNotes.filter { it.pendingDelete }
            Log.d(TAG, "Uploading ${pendingDeleteNotes.size} pending delete notes for user: $userId")

            pendingDeleteNotes.forEach { note ->
                try {
                    val remoteResult = noteRemoteDataSource.deleteNote(note.id)
                    if (remoteResult.isSuccess) {
                        // Mark as fully deleted
                        val fullyDeletedNote = note.copy(
                            isDeleted = true,
                            pendingDelete = false
                        )
                        noteDao.insertNote(fullyDeletedNote)
                        Log.d(TAG, "Deleted note from remote: ${note.id}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete note: ${note.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing notes", e)
        }
    }

    private suspend fun syncPendingDiaries(userId: String) {
        try {
            // Get all diaries for current user (including pending delete)
            val allLocalDiaries = diaryDao.getAllDiaries()
            val userDiaries = allLocalDiaries.filter { it.userId == userId }
            
            // Process each pending diary one by one (only those with lastSyncTimestamp = 0)
            val pendingDiaries = userDiaries.filter { 
                !it.isDeleted && !it.pendingDelete && it.lastSyncTimestamp == 0L 
            }
            Log.d(TAG, "Uploading ${pendingDiaries.size} pending diaries for user: $userId")

            pendingDiaries.forEach { diary ->
                try {
                    val result = diaryRemoteDataSource.saveDiary(diary)
                    if (result.isSuccess) {
                        // Update lastSyncTimestamp to mark as synced
                        val syncedDiary = diary.copy(lastSyncTimestamp = System.currentTimeMillis())
                        diaryDao.insertDiary(syncedDiary)
                        Log.d(TAG, "Uploaded diary: ${diary.id} (title: ${diary.title})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload diary: ${diary.id}", e)
                }
            }

            // Process pending deletes one by one
            val pendingDeleteDiaries = userDiaries.filter { it.pendingDelete }
            Log.d(TAG, "Uploading ${pendingDeleteDiaries.size} pending delete diaries for user: $userId")

            pendingDeleteDiaries.forEach { diary ->
                try {
                    val remoteResult = diaryRemoteDataSource.deleteDiary(diary.id)
                    if (remoteResult.isSuccess) {
                        // Mark as fully deleted
                        val fullyDeletedDiary = diary.copy(
                            isDeleted = true,
                            pendingDelete = false
                        )
                        diaryDao.insertDiary(fullyDeletedDiary)
                        Log.d(TAG, "Deleted diary from remote: ${diary.id}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete diary: ${diary.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing diaries", e)
        }
    }
}
