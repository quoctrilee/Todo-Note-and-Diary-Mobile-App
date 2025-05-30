package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNotesByCategoryUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(userId: String, category: String): Flow<List<NoteEntity>> {
        return noteRepository.getNotesByCategory(userId, category)
    }
}