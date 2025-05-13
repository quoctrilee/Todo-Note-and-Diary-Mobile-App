package com.example.todonotediary.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "diaries")
data class DiaryEntity (
    @PrimaryKey
    var id: String = "",
    var date: Long = 0L,
    var mood: String = "",
    var title: String = "",
    var content: String = "",
    var userId: String = "",
    var updatedAt: Long = System.currentTimeMillis(),
    var lastSyncTimestamp: Long = 0L,
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false
)