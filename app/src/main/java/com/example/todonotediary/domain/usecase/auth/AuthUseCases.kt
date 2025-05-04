package com.example.todonotediary.domain.usecase.auth

data class AuthUseCases(
    val getCurrentUser: GetCurrentUserUseCase,
    val signInWithGoogle: SignInWithGoogleUseCase,
    val loginWithEmail: LoginWithEmailUseCase,
    val signOut: SignOutUseCase,
    val saveUserToFirebaseUseCase: SaveUserToFirebaseUseCase
)