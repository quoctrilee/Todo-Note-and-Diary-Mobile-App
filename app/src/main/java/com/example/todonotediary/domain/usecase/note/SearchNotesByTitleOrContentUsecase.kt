package com.example.todonotediary.domain.usecase.note

import com.example.todonotediary.domain.model.NoteEntity
import com.example.todonotediary.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchNotesByTitleOrContentUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(userId: String, search: String): Flow<List<NoteEntity>> {
        return noteRepository.searchByTitleOrContent(userId, search )
    }
}