package com.example.todonotediary.data.local

import androidx.room.*
import com.example.todonotediary.domain.model.TodoEntity
@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("SELECT * FROM todos WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getTodos(userId: String): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?

    @Query("SELECT * FROM todos WHERE userId = :userId AND lastSyncTimestamp < updatedAt")
    suspend fun getTodosToSync(userId: String): List<TodoEntity>
}
