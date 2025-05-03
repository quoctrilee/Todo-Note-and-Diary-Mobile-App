package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.repository.NoteRepository
import javax.inject.Inject

class SyncNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        return noteRepository.syncNotes(userId, lastSyncTimestamp)
    }
}