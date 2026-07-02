package com.example.todonotediary.domain.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    fun getCrurrentUser(): FirebaseUser?
    suspend fun signInwWithGoogle(idToken: String): Result<Pair<FirebaseUser, Boolean>>
    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser>
    suspend fun signOut()
    suspend fun saveUserToFirebase(email: String, password: String, displayName: String): Result<FirebaseUser>
    suspend fun updateUserAvatar(userId: String, avatarName: String): Result<Unit>
    suspend fun getUserData(userId: String): Result<Map<String, Any>>
}