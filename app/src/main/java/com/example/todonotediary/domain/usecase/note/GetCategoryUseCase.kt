package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.repository.NoteRepository
import javax.inject.Inject

class GetCategoryUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(userId: String): List<String> {
        return noteRepository.getCategories(userId)
    }
}