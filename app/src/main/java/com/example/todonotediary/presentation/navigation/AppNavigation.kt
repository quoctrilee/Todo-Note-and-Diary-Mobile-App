package com.example.todonotediary.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.todonotediary.presentation.MainScreen
import com.example.todonotediary.presentation.auth.AuthScreen
import com.example.todonotediary.presentation.splash.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Auth.route) {
            AuthScreen(navController = navController)
        }
        composable(Screen.Todo.route) {
            MainScreen()
        }
    }
}