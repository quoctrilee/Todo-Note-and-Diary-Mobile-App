package com.example.todonotediary.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    var title: String = "",
    var description: String = "",

    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,

    var createdAt: Long = 0,
    var updatedAt: Long = 0,
    var startAt: Long? = null,
    var deadline: Long? = null,
    var lastSyncTimestamp: Long = 0,

    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false
)