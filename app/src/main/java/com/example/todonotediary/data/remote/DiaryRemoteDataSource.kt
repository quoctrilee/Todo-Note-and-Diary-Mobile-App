package com.example.todonotediary.data.remote

import android.util.Log
import com.example.todonotediary.domain.model.DiaryEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class DiaryRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_DIARIES = "diaries"
    }

    suspend fun getDiaryById(diaryId: String): DiaryEntity? {
        return try {
            val document = firestore.collection(COLLECTION_DIARIES)
                .document(diaryId)
                .get()
                .await()

            document.toObject(DiaryEntity::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            null
        }
    }

    // Lấy tất cả diaries của user từ Firebase
    suspend fun getDiaries(userId: String): Result<List<DiaryEntity>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_DIARIES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()

            val diaries = snapshot.documents.mapNotNull { document ->
                document.toObject(DiaryEntity::class.java)?.copy(
                    id = document.id
                )
            }
            Result.success(diaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDiary(diary: DiaryEntity): Result<DiaryEntity> {
        return try {
            val timestamp = Date().time
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(diary.date))
            Log.d("DiaryDebug", "Date: $diary, Formatted date: $formattedDate")

            // Normalize the date to start of day for more consistent date filtering
            val calendar = Calendar.getInstance().apply {
                timeInMillis = diary.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val normalizedDate = calendar.timeInMillis

            val diaryWithTimestamp = diary.copy(
                date = normalizedDate,
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

    suspend fun searchDiaries(userId: String, query: String): List<DiaryEntity> {
        return try {
            firestore.collection(COLLECTION_DIARIES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(DiaryEntity::class.java)?.copy(id = doc.id)
                }.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.content.contains(query, ignoreCase = true)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}