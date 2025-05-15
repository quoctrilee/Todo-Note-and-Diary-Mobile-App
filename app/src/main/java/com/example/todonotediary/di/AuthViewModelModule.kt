package com.example.todonotediary.di

import com.example.todonotediary.domain.usecase.auth.*
import com.example.todonotediary.domain.usecase.user.GetUserDataUseCase
import com.example.todonotediary.domain.usecase.user.UpdateUserAvatarUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthViewModelModule {

    @Provides
    @Singleton
    fun provideAuthUseCases(
        getCurrentUserUseCase: GetCurrentUserUseCase,
        signInWithGoogleUseCase: SignInWithGoogleUseCase,
        saveUserToFirebaseUseCase: SaveUserToFirebaseUseCase,
        loginWithEmailUseCase: LoginWithEmailUseCase,
        signOutUseCase: SignOutUseCase,
        getUserDataUseCase: GetUserDataUseCase,
        updateUserAvatarUseCase: UpdateUserAvatarUseCase
    ): AuthUseCases {
        return AuthUseCases(
            getCurrentUser = getCurrentUserUseCase,
            signInWithGoogle = signInWithGoogleUseCase,
            saveUserToFirebaseUseCase = saveUserToFirebaseUseCase,
            loginWithEmail = loginWithEmailUseCase,
            signOut = signOutUseCase,
            getUserDataUseCase = getUserDataUseCase,
            updateUserAvatarUseCase = updateUserAvatarUseCase

        )
    }
}