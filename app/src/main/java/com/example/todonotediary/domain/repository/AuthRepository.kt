package com.example.todonotediary.domain.repository

import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    fun getCrurrentUser(): FirebaseUser?
    suspend fun signInwWithGoogle(idToken: String) : Result<FirebaseUser>
    suspend fun signOut()
    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser>
    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser>

}