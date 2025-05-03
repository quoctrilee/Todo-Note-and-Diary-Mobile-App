package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.repository.NoteRepository
import javax.inject.Inject

class UpdateNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(note: NoteEntity): Result<Unit> {
        return noteRepository.updateNote(note)
    }
}