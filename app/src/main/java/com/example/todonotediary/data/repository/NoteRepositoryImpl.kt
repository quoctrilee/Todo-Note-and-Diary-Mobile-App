package com.example.todonotediary.data.repository

import com.example.todonotediary.data.local.NoteDao
import com.example.todonotediary.data.remote.NoteRemoteDataSource
import com.example.todonotediary.data.remote.TodoRemoteDataSource
import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class NoteRepositoryImpl(private val noteDao: NoteDao, private val remoteDataSource: NoteRemoteDataSource) : NoteRepository {

    override fun getNotes(userId: String): Flow<List<NoteEntity>> {
        return flow {
            emit(noteDao.getNotes(userId))
        }.catch { exception ->
            emit(emptyList())
        }
    }

    override suspend fun getNoteById(noteId: String): NoteEntity? {
        return noteDao.getNoteById(noteId)
    }

    override suspend fun addNote(note: NoteEntity): Result<NoteEntity> {
        return try {
            noteDao.insertNote(note)
            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNote(note: NoteEntity): Result<Unit> {
        return try {
            val updated = note.copy(updatedAt = System.currentTimeMillis())
            noteDao.updateNote(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            val note = noteDao.getNoteById(noteId)
                ?: return Result.failure(Exception("Note not found"))

            val softDeleted = note.copy(updatedAt = System.currentTimeMillis(), isDeleted = true, lastSyncTimestamp = System.currentTimeMillis())
            noteDao.updateNote(softDeleted)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun syncNotes(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        return try {
            // 1. Đẩy dữ liệu local chưa đồng bộ lên Firestore
            val localNotesToSync = noteDao.getNotesToSync(userId)
            for (note in localNotesToSync) {
                val syncedNote = remoteDataSource.saveNote(note).getOrNull()
                if (syncedNote != null) {
                    noteDao.insertNote(syncedNote.copy(lastSyncTimestamp = System.currentTimeMillis()))
                }
            }

            // 2. Lấy các bản ghi từ Firestore cập nhật về local
            val remoteUpdates = remoteDataSource.getNotesUpdatedAfter(userId, lastSyncTimestamp)
            for (remoteNote in remoteUpdates) {
                noteDao.insertNote(remoteNote)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}