package com.example.todonotediary.data.remote

import com.example.todonotediary.domain.model.NoteEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class NoteRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_NOTES = "notes"
    }

    suspend fun getCategories(userId: String): List<String> {
        return try {
            firestore.collection("notes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("category") }
                .distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Lấy tất cả notes của user từ Firebase
    suspend fun getNotes(userId: String): Result<List<NoteEntity>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_NOTES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()

            val notes = snapshot.documents.mapNotNull { document ->
                document.toObject(NoteEntity::class.java)?.copy(
                    id = document.id
                )
            }
            Result.success(notes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Lấy một note theo id
    suspend fun getNoteById(noteId: String): NoteEntity? {
        return try {
            val document = firestore.collection(COLLECTION_NOTES)
                .document(noteId)
                .get()
                .await()

            document.toObject(NoteEntity::class.java)?.copy(
                id = document.id
            )
        } catch (e: Exception) {
            null
        }
    }

    // Thêm hoặc cập nhật note
    suspend fun saveNote(note: NoteEntity): Result<NoteEntity> {
        return try {
            val noteWithTimestamp = note.copy(
                lastSyncTimestamp = Date().time
            )

            val noteRef = if (note.id.isNotEmpty()) {
                firestore.collection(COLLECTION_NOTES).document(note.id)
            } else {
                firestore.collection(COLLECTION_NOTES).document()
            }

            noteRef.set(noteWithTimestamp).await()

            Result.success(noteWithTimestamp.copy(id = noteRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Xóa note
    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_NOTES)
                .document(noteId)
                .update("isDeleted", true, "lastSyncTimestamp", Date().time)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
