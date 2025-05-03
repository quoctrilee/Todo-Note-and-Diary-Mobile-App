package com.example.todonotediary.domain.usecase.diary

data class DiaryUseCases(
    val getDiaries: GetDiariesUseCase,
    val getDiaryById: GetDiaryByIdUseCase,
    val addDiary: AddDiaryUseCase,
    val updateDiary: UpdateDiaryUseCase,
    val deleteDiary: DeleteDiaryUseCase,
    val syncDiaries: SyncDiariesUseCase
)
