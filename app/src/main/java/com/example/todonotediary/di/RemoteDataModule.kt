package com.example.todonotediary.di

import com.example.todonotediary.data.remote.DiaryRemoteDataSource
import com.example.todonotediary.data.remote.NoteRemoteDataSource
import com.example.todonotediary.data.remote.TodoRemoteDataSource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteDataModule {

    @Provides
    @Singleton
    fun provideTodoRemoteDataSource(firestore: FirebaseFirestore): TodoRemoteDataSource {
        return TodoRemoteDataSource(firestore)
    }

    @Provides
    @Singleton
    fun provideNoteRemoteDataSource(firestore: FirebaseFirestore): NoteRemoteDataSource {
        return NoteRemoteDataSource(firestore)
    }

    @Provides
    @Singleton
    fun provideDiaryRemoteDataSource(firestore: FirebaseFirestore): DiaryRemoteDataSource {
        return DiaryRemoteDataSource(firestore)
    }
}