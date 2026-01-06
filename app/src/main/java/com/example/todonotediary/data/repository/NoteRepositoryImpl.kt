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

    // Cache last sync time to avoid too frequent syncs
    private var lastSyncTime = 0L
    private val syncIntervalMs = 30_000L // 30 seconds

    private fun shouldSync(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastSyncTime) > syncIntervalMs
    }

    override fun getNotes(userId: String): Flow<List<NoteEntity>> {
        return flow {
            // Trigger sync from remote first if online and not synced recently
            if (networkManager.isCurrentlyOnline() && shouldSync()) {
                try {
                    syncFromRemote(userId)
                    lastSyncTime = System.currentTimeMillis()
                    Log.d("NoteRepository", "Synced notes from Firebase before emitting")
                } catch (e: Exception) {
                    Log.e("NoteRepository", "Failed to sync from remote, continuing with local data", e)
                }
            }
            
            // Emit from local database (which now has updated data)
            localDataSource.getNotesFlow(userId)
                .collect { notes ->
                    emit(notes)
                }
        }.catch { exception ->
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
                    // Reset sync cache to force refresh next time
                    lastSyncTime = 0L
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
            val updatedNote = note.copy(
                updatedAt = System.currentTimeMillis(),
                lastSyncTimestamp = 0L // Mark as pending sync
            )
            localDataSource.updateNote(updatedNote)

            // 2. Sync to remote only if online
            if (networkManager.isCurrentlyOnline()) {
                val remoteResult = remoteDataSource.saveNote(updatedNote)
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
            Log.e("NoteRepository", "Error updating note", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            // 1. Mark as pending delete in local
            val localNote = localDataSource.getNoteById(noteId)
            if (localNote != null) {
                val deletedNote = localNote.copy(
                    pendingDelete = true,
                    updatedAt = System.currentTimeMillis()
                )
                localDataSource.updateNote(deletedNote)
                Log.d("NoteRepository", "Marked note as pending delete: $noteId")
            }

            // 2. Try to delete from remote if online
            if (networkManager.isCurrentlyOnline()) {
                val remoteResult = remoteDataSource.deleteNote(noteId)
                if (remoteResult.isSuccess) {
                    // Successfully deleted from remote, now hard delete locally
                    if (localNote != null) {
                        val fullyDeletedNote = localNote.copy(
                            isDeleted = true,
                            pendingDelete = false
                        )
                        localDataSource.updateNote(fullyDeletedNote)
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

    override suspend fun syncFromRemote(userId: String): Result<Unit> {
        return try {
            if (!networkManager.isCurrentlyOnline()) {
                Log.d("NoteRepository", "No network, skipping remote sync")
                return Result.success(Unit)
            }

            // 1. Fetch all notes from Firebase
            val remoteResult = remoteDataSource.getNotes(userId)
            if (remoteResult.isFailure) {
                Log.e("NoteRepository", "Failed to fetch notes from Firebase", remoteResult.exceptionOrNull())
                return Result.failure(remoteResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val remoteNotes = remoteResult.getOrNull() ?: emptyList()
            Log.d("NoteRepository", "Fetched ${remoteNotes.size} notes from Firebase")

            // 2. Get all local notes
            val localNotes = localDataSource.getAllNotes()
            val userLocalNotes = localNotes.filter { it.userId == userId }
            
            // 3. Merge logic: Firebase is source of truth for synced data
            remoteNotes.forEach { remoteNote ->
                val localNote = userLocalNotes.find { it.id == remoteNote.id }
                
                if (localNote == null) {
                    // New note from Firebase - insert to local
                    localDataSource.insertNote(remoteNote)
                    Log.d("NoteRepository", "Inserted new note from Firebase: ${remoteNote.id}")
                } else {
                    // Note exists locally - check if we should update
                    // Only update if remote is newer AND local doesn't have pending changes
                    val hasLocalPendingChanges = localNote.lastSyncTimestamp == 0L || localNote.pendingDelete
                    
                    if (!hasLocalPendingChanges && remoteNote.updatedAt > localNote.updatedAt) {
                        // Remote is newer and local has no pending changes - update local
                        localDataSource.insertNote(remoteNote)
                        Log.d("NoteRepository", "Updated note from Firebase: ${remoteNote.id}")
                    } else if (hasLocalPendingChanges) {
                        Log.d("NoteRepository", "Skipping update for note ${remoteNote.id} - has pending local changes")
                    }
                }
            }

            Log.d("NoteRepository", "Sync from remote completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error syncing from remote", e)
            Result.failure(e)
        }
    }
}
