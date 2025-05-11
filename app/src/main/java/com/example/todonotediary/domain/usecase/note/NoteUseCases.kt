package com.example.todonotediary.domain.usecase.note

data class NoteUseCases(
    val addNote: AddNoteUseCase,
    val deleteNote: DeleteNoteUseCase,
    val getNoteById: GetNoteByIdUseCase,
    val getNotesByCategory: GetNotesByCategoryUseCase,
    val getNotes: GetNotesUseCase,
    val syncNotes: SyncNotesUseCase,
    val updateNote: UpdateNoteUseCase,
    val searchNotesByTitleOrContentUseCase: SearchNotesByTitleOrContentUseCase,
    val getCategoryUseCase: GetCategoryUseCase
)