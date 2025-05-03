package com.example.todonotediary.data.repository

import com.example.todonotediary.domain.repository.AuthRepository
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

    override suspend fun signInwWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            // Get the user
            val user = authResult.user!!

            // Check if user exists in Firestore
            val userDoc = firestore.collection("users").document(user.uid).get().await()

            if (!userDoc.exists()) {
                // Create a minimal user profile
                val userData = hashMapOf(
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )

                // Save user data to Firestore
                firestore.collection("users").document(user.uid)
                    .set(userData)
                    .await()
            }

            Result.success(user)
        } catch (e: Exception){
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            // Add user to Firestore with basic profile
            val userData = hashMapOf(
                "email" to email,
                "displayName" to "",
                "createdAt" to System.currentTimeMillis()
            )

            // Save user data to Firestore
            firestore.collection("users").document(user.uid)
                .set(userData)
                .await()

            Result.success(user)
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