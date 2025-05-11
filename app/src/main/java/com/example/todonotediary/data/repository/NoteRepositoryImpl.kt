package com.example.todonotediary.data.repository

import com.example.todonotediary.data.remote.NoteRemoteDataSource
import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: NoteRemoteDataSource
) : NoteRepository {

    override fun getNotes(userId: String): Flow<List<NoteEntity>> {
        return flow {
            emit(remoteDataSource.getNotes(userId))
        }.catch {
            emit(emptyList())
        }
    }

    override suspend fun getCategories(userId: String): List<String> {
        return remoteDataSource.getCategories(userId)
    }

    override fun getNotesByCategory(userId: String, category: String): Flow<List<NoteEntity>> {
        return flow {
            emit(remoteDataSource.getNotesByCategory(userId, category))
        }.catch {
            emit(emptyList())
        }
    }

    override fun searchByTitleOrContent(userId: String, search: String): Flow<List<NoteEntity>> {
        return flow {
            emit(remoteDataSource.searchByTitleOrContent(userId, search))
        }.catch {
            emit(emptyList())
        }
    }


    override suspend fun getNoteById(noteId: String): NoteEntity? {
        return remoteDataSource.getNoteById(noteId)
    }

    override suspend fun addNote(note: NoteEntity): Result<NoteEntity> {
        return remoteDataSource.saveNote(note)
    }

    override suspend fun updateNote(note: NoteEntity): Result<Unit> {
        return try {
            val result = remoteDataSource.saveNote(note)
            if (result.isSuccess) Result.success(Unit)
            else Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        return remoteDataSource.deleteNote(noteId)
    }

    override suspend fun syncNotes(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        // Vì không còn local nữa, sync sẽ không thực hiện gì cả
        return Result.success(Unit)
    }
}
