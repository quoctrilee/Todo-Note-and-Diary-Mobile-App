package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.repository.NoteRepository
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(noteId: String): NoteEntity? {
        return noteRepository.getNoteById(noteId)
    }
}