package com.example.todonotediary.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreenWithNavigation(navController: NavHostController) {
    MainScreen(parentNavController = navController)
}