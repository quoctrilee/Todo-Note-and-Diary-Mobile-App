package com.example.todonotediary.domain.usecase.user

import com.example.todonotediary.domain.repository.AuthRepository
import javax.inject.Inject

class GetUserDataUseCase @Inject constructor(
    private val userRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String) = userRepository.getUserData(userId)
}