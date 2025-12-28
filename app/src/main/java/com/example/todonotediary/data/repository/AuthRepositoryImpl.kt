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
            val user =
                authResult.user ?: return Result.failure(Exception("Đăng nhập không thành công"))
            // Check if user exists in Firestore
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val userExists = userDoc.exists()

            Result.success(Pair(user, userExists))
        } catch (e: Exception) {
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


    override suspend fun saveUserToFirebase(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            // Lấy người dùng hiện tại đã đăng nhập (với Google)
            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("Không tìm thấy người dùng đã đăng nhập"))

            // Lấy email từ tài khoản Google đã đăng nhập
            val googleEmail = currentUser.email
                ?: return Result.failure(Exception("Không tìm thấy email từ tài khoản Google"))

            // Kiểm tra xem người dùng đã được liên kết với email/password chưa
            val isAlreadyLinked = currentUser.providerData.any {
                it.providerId == EmailAuthProvider.PROVIDER_ID
            }

            if (!isAlreadyLinked) {
                // Nếu chưa liên kết, thực hiện liên kết
                // Sử dụng email từ tài khoản Google (bắt buộc phải trùng)
                try {
                    val credential = EmailAuthProvider.getCredential(googleEmail, password)
                    currentUser.linkWithCredential(credential).await()
                } catch (e: Exception) {
                    // Nếu lỗi là "already linked", bỏ qua và tiếp tục
                    val isAlreadyLinkedError =
                        e.message?.contains("already been linked", ignoreCase = true) ?: false
                    if (!isAlreadyLinkedError) {
                        throw e
                    }
                }
            }

            // Kiểm tra xem user data đã tồn tại trong Firestore chưa
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()

            if (!userDoc.exists()) {
                // Chỉ tạo mới nếu chưa tồn tại
                val userData = hashMapOf(
                    "email" to googleEmail,  // Sử dụng email từ Google
                    "displayName" to displayName,
                    "avatar_url" to "R.drawable.avt_default",
                    "createdAt" to System.currentTimeMillis()
                )

                // Lưu dữ liệu người dùng vào Firestore sử dụng uid
                firestore.collection("users").document(currentUser.uid)
                    .set(userData)
                    .await()
            } else {
                // Nếu đã tồn tại, chỉ cập nhật displayName nếu cần
                firestore.collection("users").document(currentUser.uid)
                    .update("displayName", displayName)
                    .await()
            }

            Result.success(currentUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Đăng nhập thất bại"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Đăng ký thất bại"))
            // Update display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdates).await()
            // Save to Firestore
            val userData = hashMapOf(
                "email" to email,
                "displayName" to displayName,
                "avatar_url" to "R.drawable.avt_default",
                "createdAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document(user.uid).set(userData).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}