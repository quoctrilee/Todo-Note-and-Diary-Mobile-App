package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.model.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNotesByCategoryUseCase @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase
) {
    operator fun invoke(userId: String, category: String): Flow<List<NoteEntity>> {
        return getNotesUseCase(userId).map { notes ->
            notes.filter { it.category == category }
        }
    }
}