package com.example.todonotediary.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diaries")
data class DiaryEntity (
    @PrimaryKey
    val id: String,
    val date: Long,
    val mood: String,
    val content: String,
    val userId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long = 0,
    val isDeleted: Boolean = false
)