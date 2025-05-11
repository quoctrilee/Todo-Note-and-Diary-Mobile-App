package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.repository.NoteRepository
import java.util.UUID
import javax.inject.Inject

class AddNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(
        title: String,
        content: String,
        userId: String,
        category: String,
        backgroundColor: String
    ): Result<NoteEntity> {
        val noteId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()

        val note = NoteEntity(
            id = noteId,
            title = title,
            content = content,
            userId = userId,
            category = category,
            createdAt = currentTime,
            updatedAt = currentTime,
            background_color = backgroundColor // Lưu màu nền
        )

        return repository.addNote(note)
    }
}