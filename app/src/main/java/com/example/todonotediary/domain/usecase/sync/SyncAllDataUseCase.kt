package com.example.todonotediary.domain.usecase.sync

import com.example.todonotediary.domain.usecase.diary.SyncDiariesUseCase
import com.example.todonotediary.domain.usecase.note.SyncNotesUseCase
import com.example.todonotediary.domain.usecase.todo.SyncTodosUseCase
import javax.inject.Inject

/**
 * Use case để đồng bộ toàn bộ dữ liệu của ứng dụng
 */
class SyncAllDataUseCase @Inject constructor(
    private val syncTodosUseCase: SyncTodosUseCase,
    private val syncNotesUseCase: SyncNotesUseCase,
    private val syncDiariesUseCase: SyncDiariesUseCase
) {
    suspend operator fun invoke(userId: String, lastSyncTimestamp: Long): Result<Unit> {
        return try {
            // Đồng bộ todos
            syncTodosUseCase(userId, lastSyncTimestamp).getOrThrow()

            // Đồng bộ notes
            syncNotesUseCase(userId, lastSyncTimestamp).getOrThrow()

            // Đồng bộ diaries
            syncDiariesUseCase(userId, lastSyncTimestamp).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}