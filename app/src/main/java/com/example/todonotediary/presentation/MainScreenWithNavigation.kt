package com.example.todonotediary.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreenWithNavigation(navController: NavHostController) {
    // Sử dụng navController từ AppNavigation để điều hướng đến các màn hình không nằm trong MainScreen
    MainScreen(parentNavController = navController)
}