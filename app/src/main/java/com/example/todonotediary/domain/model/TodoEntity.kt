package com.example.todonotediary.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    val id: String = "",
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deadline: Long? = null,
    val userId: String,
    val lastSyncTimestamp: Long = 0,
    val isDeleted: Boolean = false
)
