package com.example.todonotediary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.todonotediary.domain.model.DiaryEntity
import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.model.TodoEntity

@Database(
    entities = [TodoEntity::class, NoteEntity::class, DiaryEntity::class],
    version = 1,
    exportSchema = false
)

abstract class TodoNoteDiaryDatabase: RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun noteDao(): NoteDao
    abstract fun diaryDao(): DiaryDao
}