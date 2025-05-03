package com.example.todonotediary.domain.usecase.auth

import com.example.todonotediary.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): FirebaseUser? {
        return authRepository.getCrurrentUser()
    }
}