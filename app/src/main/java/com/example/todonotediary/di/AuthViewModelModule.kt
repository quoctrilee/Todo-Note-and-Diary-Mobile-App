package com.example.todonotediary.di

import com.example.todonotediary.domain.usecase.auth.*
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
        registerWithEmailUseCase: RegisterWithEmailUseCase,
        loginWithEmailUseCase: LoginWithEmailUseCase,
        signOutUseCase: SignOutUseCase
    ): AuthUseCases {
        return AuthUseCases(
            getCurrentUser = getCurrentUserUseCase,
            signInWithGoogle = signInWithGoogleUseCase,
            registerWithEmail = registerWithEmailUseCase,
            loginWithEmail = loginWithEmailUseCase,
            signOut = signOutUseCase
        )
    }
}