package com.example.todonotediary.data.repository

import android.util.Log
import com.example.todonotediary.data.local.NoteDao
import com.example.todonotediary.data.remote.NoteRemoteDataSource
import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.repository.NoteRepository
import com.example.todonotediary.utils.NetworkManager
import com.example.todonotediary.utils.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val localDataSource: NoteDao,
    private val remoteDataSource: NoteRemoteDataSource,
    private val networkManager: NetworkManager,
    private val syncManager: SyncManager
) : NoteRepository {

    override fun getNotes(userId: String): Flow<List<NoteEntity>> {
        // Pure offline-first: only emit from local database
        // Background sync via SyncWorker will keep data updated
        return localDataSource.getNotesFlow(userId)
            .catch { exception ->
                Log.e("NoteRepository", "Error loading notes", exception)
                emit(emptyList())
            }
    }

    override suspend fun getCategories(userId: String): List<String> {
        return try {
            // Get categories from local first
            val localNotes = localDataSource.getNotes(userId)
            val localCategories = localNotes.mapNotNull { it.category }.distinct()
            
            // Try to get from remote
            try {
                remoteDataSource.getCategories(userId)
            } catch (e: Exception) {
                Log.e("NoteRepository", "Error getting remote categories", e)
                localCategories
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error getting categories", e)
            emptyList()
        }
    }

    override fun getNotesByCategory(userId: String, category: String): Flow<List<NoteEntity>> {
        // Pure offline-first: filter local data only
        return flow {
            localDataSource.getNotesFlow(userId)
                .collect { notes ->
                    val filteredNotes = notes.filter { it.category == category }
                    emit(filteredNotes)
                }
        }.catch { exception ->
            Log.e("NoteRepository", "Error loading notes by category", exception)
            emit(emptyList())
        }
    }

    override fun searchByTitleOrContent(userId: String, search: String): Flow<List<NoteEntity>> {
        // Pure offline-first: search in local database only
        return flow {
            localDataSource.getNotesFlow(userId)
                .collect { notes ->
                    val searchResults = notes.filter { note ->
                        note.title.contains(search, ignoreCase = true) ||
                                note.content.contains(search, ignoreCase = true)
                    }
                    emit(searchResults)
                }
        }.catch { exception ->
            Log.e("NoteRepository", "Error searching notes", exception)
            emit(emptyList())
        }
    }

    override suspend fun getNoteById(noteId: String): NoteEntity? {
        // Try local first
        val localNote = localDataSource.getNoteById(noteId)
        if (localNote != null) return localNote

        // Fallback to remote
        return try {
            remoteDataSource.getNoteById(noteId)
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error getting note by id", e)
            null
        }
    }

    override suspend fun addNote(note: NoteEntity): Result<NoteEntity> {
        return try {
            // 1. Save to local first
            localDataSource.insertNote(note)

            // 2. Sync to remote only if online
            if (networkManager.isCurrentlyOnline()) {
                val remoteResult = remoteDataSource.saveNote(note)
                if (remoteResult.isSuccess) {
                    Result.success(note)
                } else {
                    // Keep in local, schedule sync for later
                    syncManager.scheduleSyncNow()
                    Result.success(note)
                }
            } else {
                // Offline: schedule sync when network returns
                syncManager.schedulePeriodicSync()
                Result.success(note)
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error adding note", e)
            Result.failure(e)
        }
    }

    override suspend fun updateNote(note: NoteEntity): Result<Unit> {
        return try {
            // 1. Update local first
            val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
            localDataSource.updateNote(updatedNote)

            // 2. Sync to remote
            val remoteResult = remoteDataSource.saveNote(updatedNote)
            if (remoteResult.isSuccess) {
                Result.success(Unit)
            } else {
                // Keep local changes even if remote fails
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error updating note", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            // 1. Soft delete in local
            val localNote = localDataSource.getNoteById(noteId)
            if (localNote != null) {
                val deletedNote = localNote.copy(
                    isDeleted = true,
                    updatedAt = System.currentTimeMillis()
                )
                localDataSource.updateNote(deletedNote)
            }

            // 2. Delete from remote
            val remoteResult = remoteDataSource.deleteNote(noteId)
            if (remoteResult.isSuccess) {
                Result.success(Unit)
            } else {
                // Keep local deletion even if remote fails
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error deleting note", e)
            Result.failure(e)
        }
    }

    override suspend fun syncNotes(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        return try {
            // Get notes that need to be synced (modified after last sync)
            val notesToSync = localDataSource.getNotesToSync(userId)
            
            // Sync each note to remote
            notesToSync.forEach { note ->
                remoteDataSource.saveNote(note)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error syncing notes", e)
            Result.failure(e)
        }
    }
}
