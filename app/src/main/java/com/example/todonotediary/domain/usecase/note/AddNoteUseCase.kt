package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.repository.NoteRepository
import java.util.UUID
import javax.inject.Inject

class AddNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(
        title: String,
        content: String,
        userId: String,
        category: String = "General"
    ): Result<NoteEntity> {
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            category = category,
            createdAt = System.currentTimeMillis(),
            userId = userId,
            lastSyncTimestamp = 0,
            isDeleted = false
        )
        return noteRepository.addNote(note)
    }
}