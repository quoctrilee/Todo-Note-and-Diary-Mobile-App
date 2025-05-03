package com.example.todonotediary.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.todonotediary.presentation.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "TodoNoteDiary")

        LaunchedEffect(key1 = true) {
            delay(2000) // 2 seconds delay
            // Kiểm tra trạng thái đăng nhập sẽ được thêm sau
            // Tạm thời chuyển đến Auth
            navController.navigate(Screen.Auth.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }
}