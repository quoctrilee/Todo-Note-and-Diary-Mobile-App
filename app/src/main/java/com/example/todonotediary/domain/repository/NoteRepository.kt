package com.example.todonotediary.domain.repository

import com.example.todonotediary.domain.model.NoteEntity
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(userId: String): Flow<List<NoteEntity>>
    fun getNotesByCategory(userId: String, category: String): Flow<List<NoteEntity>>
    fun searchByTitleOrContent(userId: String, search: String): Flow<List<NoteEntity>>
    suspend fun getCategories(userId: String): List<String>
    suspend fun  getNoteById(noteId: String): NoteEntity?
    suspend fun  addNote(note: NoteEntity): Result<NoteEntity>
    suspend fun  updateNote(note : NoteEntity): Result<Unit>
    suspend fun  deleteNote(noteId: String): Result<Unit>
    suspend fun syncNotes(userId: String, lastSyncTimestamp: Long): Result<Unit>
}