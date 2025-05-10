//package com.example.todonotediary.domain.usecase.todo
//
//import com.example.todonotediary.domain.repository.TodoRepository
//import javax.inject.Inject
//
//class SyncTodosUseCase @Inject constructor(
//    private val todoRepository: TodoRepository
//) {
//    suspend operator fun invoke(userId: String, lastSyncTimestamp: Long): Result<Unit> {
//        return todoRepository.syncTodos(userId, lastSyncTimestamp)
//    }
//}