package com.example.todonotediary.data.remote

import com.example.todonotediary.domain.model.DiaryEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class DiaryRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_DIARIES = "diaries"
    }

    suspend fun getDiaries(userId: String): List<DiaryEntity> {
        return try {
            firestore.collection(COLLECTION_DIARIES)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(DiaryEntity::class.java)?.copy(
                        id = document.id
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDiaryById(diaryId: String): DiaryEntity? {
        return try {
            val document = firestore.collection(COLLECTION_DIARIES)
                .document(diaryId)
                .get()
                .await()

            document.toObject(DiaryEntity::class.java)?.copy(
                id = document.id
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveDiary(diary: DiaryEntity): Result<DiaryEntity> {
        return try {
            val diaryWithTimestamp = diary.copy(
                lastSyncTimestamp = Date().time
            )

            val diaryRef = if (diary.id.isNotEmpty()) {
                firestore.collection(COLLECTION_DIARIES).document(diary.id)
            } else {
                firestore.collection(COLLECTION_DIARIES).document()
            }

            diaryRef.set(diaryWithTimestamp).await()

            Result.success(diaryWithTimestamp.copy(id = diaryRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDiary(diaryId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_DIARIES)
                .document(diaryId)
                .update("isDeleted", true, "lastSyncTimestamp", Date().time)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDiariesUpdatedAfter(userId: String, lastSyncTimestamp: Long): List<DiaryEntity> {
        return try {
            firestore.collection(COLLECTION_DIARIES)
                .whereEqualTo("userId", userId)
                .whereGreaterThan("lastSyncTimestamp", lastSyncTimestamp)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(DiaryEntity::class.java)?.copy(
                        id = document.id
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}