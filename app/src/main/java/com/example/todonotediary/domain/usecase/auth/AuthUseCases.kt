package com.example.todonotediary.domain.usecase.auth

data class AuthUseCases(
    val getCurrentUser: GetCurrentUserUseCase,
    val signInWithGoogle: SignInWithGoogleUseCase,
    val registerWithEmail: RegisterWithEmailUseCase,
    val loginWithEmail: LoginWithEmailUseCase,
    val signOut: SignOutUseCase
)