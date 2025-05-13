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

    suspend fun getDiaries(userId: String): List<DiaryEntity> {
        return try {
            firestore.collection(COLLECTION_DIARIES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(DiaryEntity::class.java)?.copy(id = doc.id)
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

            document.toObject(DiaryEntity::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            null
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

    suspend fun getDiariesByDate(userId: String, date: Long): List<DiaryEntity> {
        return try {
            // Calculate start and end of the day for the given date
            val startOfDay = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
            Log.d("DiaryDebug", "Fetching diaries for user: $userId on date: $formattedDate ($startOfDay)")

            // Modified approach: Get all diaries and compare the day
            val allDiaries = firestore.collection(COLLECTION_DIARIES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(DiaryEntity::class.java)?.copy(id = doc.id)
                }

            // Use the normalized date or fall back to day comparison if needed
            val filteredDiaries = allDiaries.filter { diary ->
                // First try exact match with normalized date
                if (diary.date == startOfDay) {
                    return@filter true
                }

                // If that fails, compare the day part of the timestamps
                val diaryDate = Calendar.getInstance().apply { timeInMillis = diary.date }
                val targetDate = Calendar.getInstance().apply { timeInMillis = date }

                diaryDate.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                        diaryDate.get(Calendar.MONTH) == targetDate.get(Calendar.MONTH) &&
                        diaryDate.get(Calendar.DAY_OF_MONTH) == targetDate.get(Calendar.DAY_OF_MONTH)
            }

            Log.d("DiaryDebug", "Number of diaries found: ${filteredDiaries.size}")
            filteredDiaries
        } catch (e: Exception) {
            Log.e("DiaryDebug", "Error fetching diaries", e)
            emptyList()
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

    suspend fun getDiariesUpdatedAfter(userId: String, lastSyncTimestamp: Long): List<DiaryEntity> {
        return try {
            firestore.collection(COLLECTION_DIARIES)
                .whereEqualTo("userId", userId)
                .whereGreaterThan("lastSyncTimestamp", lastSyncTimestamp)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(DiaryEntity::class.java)?.copy(id = document.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}