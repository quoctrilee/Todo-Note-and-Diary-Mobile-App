//package com.example.todonotediary.presentation.auth
//
//import android.content.Context
//import android.util.Log
//import androidx.activity.compose.ManagedActivityResultLauncher
//import androidx.activity.result.ActivityResult
//import androidx.navigation.NavController
//import com.example.todonotediary.domain.usecase.auth.AuthUseCases
//import com.example.todonotediary.presentation.navigation.Screen
//import com.google.android.gms.auth.api.signin.GoogleSignIn
//import com.google.android.gms.auth.api.signin.GoogleSignInClient
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions
//import com.google.android.gms.common.api.ApiException
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//
//class GoogleAuthHandler(
//    private val context: Context,
//    private val viewModel: AuthViewModel,
//    private val navController: NavController,
//    private val coroutineScope: CoroutineScope
//) {
//    private val auth = FirebaseAuth.getInstance()
//    private val firestore = FirebaseFirestore.getInstance()
//
//    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//        .requestIdToken("712471685484-jadone5ck2fqss4s7k9qeisvin4s2mi8.apps.googleusercontent.com")
//        .requestEmail()
//        .build()
//
//    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)
//
//    fun handleSignInResult(result: ActivityResult) {
//        try {
//            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
//            val account = task.getResult(ApiException::class.java)
//
//            // Lấy token ID và email từ Google
//            account?.let { googleAccount ->
//                val idToken = googleAccount.idToken
//                val email = googleAccount.email
//
//                if (idToken != null && email != null) {
//                    coroutineScope.launch {
//                        handleGoogleAuth(idToken, email)
//                    }
//                }
//            }
//        } catch (e: ApiException) {
//            // Xử lý lỗi
//            e.printStackTrace()
//        }
//    }
//
//    private suspend fun handleGoogleAuth(idToken: String, email: String) {
//        try {
//            // Kiểm tra xem email đã tồn tại trong hệ thống chưa
//            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
//            val signInResult = auth.signInWithCredential(credential).await()
//
//            // Lấy user ID của user
//            val user = signInResult.user
//
//            if (user != null) {
//                // Kiểm tra xem user đã tồn tại trong Firestore chưa
//                val userDoc = firestore.collection("users").document(user.uid).get().await()
//                withContext(Dispatchers.Main) {
//                    if (!userDoc.exists()) {
//                        // Nếu là user mới, chuyển đến trang đăng ký với email được truyền vào
//                        // Chuyển đến trang đăng ký với email
//                        navController.navigate("${Screen.Register.route}?email=$email")
//                    } else {
//                        // Nếu user đã tồn tại, xác thực thành công
//                        viewModel.signInWithGoogle(idToken)
//                        navController.navigate(Screen.Todo.route)
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//}