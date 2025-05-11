package com.example.todonotediary.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.todonotediary.presentation.MainScreenWithNavigation
import com.example.todonotediary.presentation.auth.AuthScreen
import com.example.todonotediary.presentation.auth.RegisterScreen
import com.example.todonotediary.presentation.note.AddNoteScreen
import com.example.todonotediary.presentation.splash.SplashScreen
import com.example.todonotediary.presentation.todo.AddTodoScreen

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
        composable(
            route = "${Screen.Register.route}?email={email}",
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email")
            RegisterScreen(navController = navController, email = email)
        }
        composable(Screen.MainScreen.route) {
            MainScreenWithNavigation(navController = navController)
        }

        composable(Screen.AddTodo.route) {
            AddTodoScreen(navController = navController, onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AddNote.route) {
            AddNoteScreen(navController = navController, onNavigateBack = { navController.popBackStack() })
        }
    }
}