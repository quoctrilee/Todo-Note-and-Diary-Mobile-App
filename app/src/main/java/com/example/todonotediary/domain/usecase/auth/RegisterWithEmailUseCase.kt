package com.example.todonotediary.domain.usecase.auth

import com.example.todonotediary.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

class RegisterWithEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<FirebaseUser> {
        return repository.registerWithEmail(email, password)
    }
}