package com.example.todonotediary.data.repository

import com.example.todonotediary.domain.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
): AuthRepository {
    override fun getCrurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    override suspend fun signInwWithGoogle(idToken: String): Result<Pair<FirebaseUser, Boolean>> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            // Get the user
            val user = authResult.user!!
            // Check if user exists in Firestore
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val userExists = userDoc.exists()

            Result.success(Pair(user, userExists))
        } catch (e: Exception){
            Result.failure(e)
        }
    }


    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
    override suspend fun updateUserAvatar(userId: String, avatarName: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId)
                .update("avatar_url", avatarName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserData(userId: String): Result<Map<String, Any>> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            if (document.exists()) {
                Result.success(document.data ?: emptyMap())
            } else {
                Result.failure(Exception("User data not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun saveUserToFirebase(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            // Lấy người dùng hiện tại đã đăng nhập (với Google)
            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("Không tìm thấy người dùng đã đăng nhập"))

            val credential = EmailAuthProvider.getCredential(email, password)
            currentUser.linkWithCredential(credential).await()
            // Lưu thông tin người dùng vào Firestore
            val userData = hashMapOf(
                "email" to email,
                "displayName" to displayName,
                "avatar_url" to "R.drawable.avt_default",
                "createdAt" to System.currentTimeMillis()
            )

            // Lưu dữ liệu người dùng vào Firestore sử dụng uid
            firestore.collection("users").document(currentUser.uid)
                .set(userData)
                .await()

            Result.success(currentUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}