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

    // Lấy danh sách notes theo userId
    suspend fun getNotes(userId: String): List<NoteEntity> {
        return try {
            firestore.collection(COLLECTION_NOTES)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(NoteEntity::class.java)?.copy(
                        id = document.id
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
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


    suspend fun searchByTitleOrContent(userId: String, search: String): List<NoteEntity> {
        return try {
            val keyword = search.trim().lowercase()

            firestore.collection(COLLECTION_NOTES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(NoteEntity::class.java)?.copy(id = document.id)
                }
                .filter { note ->
                    note.title?.lowercase()?.contains(keyword) == true ||
                            note.content?.lowercase()?.contains(keyword) == true
                }
        } catch (e: Exception) {
            emptyList()
        }
    }


    suspend fun getNotesByCategory(userId: String, category: String): List<NoteEntity> {
        return try {
            firestore.collection(COLLECTION_NOTES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", category)
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(NoteEntity::class.java)?.copy(
                        id = document.id
                    )
                }
        } catch (e: Exception) {
            emptyList()
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

    // Lấy các note đã được thay đổi sau lastSyncTimestamp
    suspend fun getNotesUpdatedAfter(userId: String, lastSyncTimestamp: Long): List<NoteEntity> {
        return try {
            firestore.collection(COLLECTION_NOTES)
                .whereEqualTo("userId", userId)
                .whereGreaterThan("lastSyncTimestamp", lastSyncTimestamp)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    document.toObject(NoteEntity::class.java)?.copy(
                        id = document.id
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
