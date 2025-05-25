package com.example.todonotediary.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.todonotediary.presentation.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Thiết lập nền trắng
        contentAlignment = Alignment.Center
    ) {
                Text(
                    text = "TodoNoteDiary",
                    fontSize = 24.sp // Sử dụng sp cho fontSize thay vì dp
                )
            LaunchedEffect(key1 = true) {
                delay(2000) // 2 seconds delay
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
    }
}
