package com.example.todonotediary.di

import com.example.todonotediary.domain.repository.AuthRepository
import com.example.todonotediary.domain.usecase.auth.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthUseCaseModule {

    @Provides
    @Singleton
    fun provideGetCurrentUserUseCase(authRepository: AuthRepository): GetCurrentUserUseCase {
        return GetCurrentUserUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSignInWithGoogleUseCase(authRepository: AuthRepository): SignInWithGoogleUseCase {
        return SignInWithGoogleUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSignOutUseCase(authRepository: AuthRepository): SignOutUseCase {
        return SignOutUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideLoginWithEmailUseCase(authRepository: AuthRepository): LoginWithEmailUseCase {
        return LoginWithEmailUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideSaveUserToFirebaseUseCase(authRepository: AuthRepository): SaveUserToFirebaseUseCase {
        return SaveUserToFirebaseUseCase(authRepository)
    }
}
