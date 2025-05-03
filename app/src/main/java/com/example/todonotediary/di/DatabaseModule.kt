package com.example.todonotediary.di

import android.content.Context
import androidx.room.Room
import com.example.todonotediary.data.local.TodoNoteDiaryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides@Singleton
    fun provideDatabase(appContext: Context): TodoNoteDiaryDatabase {
        return Room.databaseBuilder(
            appContext,
            TodoNoteDiaryDatabase::class.java,
            "todo_note_diary_db"
        ).build()
    }

    @Provides
    fun provideTodoDao(db: TodoNoteDiaryDatabase) = db.todoDao()

    @Provides
    fun provideNoteDao(db: TodoNoteDiaryDatabase) = db.noteDao()

    @Provides
    fun provideDiaryDao(db: TodoNoteDiaryDatabase) = db.diaryDao()
}