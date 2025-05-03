package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.repository.NoteRepository
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(noteId: String): Result<Unit> {
        return noteRepository.deleteNote(noteId)
    }
}