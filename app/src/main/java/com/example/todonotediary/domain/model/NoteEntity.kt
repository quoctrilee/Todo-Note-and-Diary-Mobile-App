package com.example.todonotediary.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    var title: String = "",
    var content: String = "",
    var category: String = "",
    var background_color: String = "",
    var createdAt: Long = 0,
    var updatedAt: Long = 0,
    var lastSyncTimestamp: Long = 0,

    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false
)

