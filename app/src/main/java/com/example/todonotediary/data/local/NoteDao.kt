package com.example.todonotediary.data.local

import androidx.room.*
import com.example.todonotediary.domain.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getNotes(userId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getNotesFlow(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE userId = :userId AND lastSyncTimestamp < updatedAt")
    suspend fun getNotesToSync(userId: String): List<NoteEntity>
}