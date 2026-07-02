package com.example.todonotediary.domain.usecase.auth

import com.example.todonotediary.domain.usecase.user.GetUserDataUseCase
import com.example.todonotediary.domain.usecase.user.UpdateUserAvatarUseCase
import javax.inject.Inject

data class AuthUseCases @Inject constructor(
    val getCurrentUser: GetCurrentUserUseCase,
    val signInWithGoogle: SignInWithGoogleUseCase,
    val loginWithEmail: LoginWithEmailUseCase,
    val signOut: SignOutUseCase,
    val saveUserToFirebaseUseCase: SaveUserToFirebaseUseCase,
    val getUserDataUseCase: GetUserDataUseCase,
    val updateUserAvatarUseCase: UpdateUserAvatarUseCase
)