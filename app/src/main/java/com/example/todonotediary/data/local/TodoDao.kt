package com.example.todonotediary.data.local

import androidx.room.*
import com.example.todonotediary.domain.model.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("SELECT * FROM todos WHERE userId = :userId AND isDeleted = 0 AND pendingDelete = 0 ORDER BY createdAt DESC")
    suspend fun getTodos(userId: String): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE userId = :userId AND isDeleted = 0 AND pendingDelete = 0 ORDER BY createdAt DESC")
    fun getTodosFlow(userId: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    suspend fun getAllTodos(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<TodoEntity>)
}
