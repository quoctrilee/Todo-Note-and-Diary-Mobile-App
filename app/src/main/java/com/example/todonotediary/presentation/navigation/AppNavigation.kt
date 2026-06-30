package com.example.todonotediary.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.todonotediary.presentation.MainScreenWithNavigation
import com.example.todonotediary.presentation.auth.AuthScreen
import com.example.todonotediary.presentation.auth.RegisterScreen
import com.example.todonotediary.presentation.diary.AddDiaryScreen
import com.example.todonotediary.presentation.note.AddNoteScreen
import com.example.todonotediary.presentation.splash.SplashScreen
import com.example.todonotediary.presentation.todo.AddTodoScreen
import com.example.todonotediary.presentation.user.UserScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        composable<SplashRoute> {
            SplashScreen(navController = navController)
        }

        composable<AuthRoute> {
            AuthScreen(navController = navController)
        }

        composable<RegisterRoute> { backStackEntry ->
            val registerRoute: RegisterRoute = backStackEntry.toRoute()
            RegisterScreen(
                navController = navController,
                email = registerRoute.email
            )
        }

        composable<MainScreenRoute> {
            MainScreenWithNavigation(navController = navController)
        }

        composable<AddTodoRoute> {
            AddTodoScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<AddNoteRoute> {
            AddNoteScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<AddDiaryRoute> {
            AddDiaryScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<UserRoute> {
            UserScreen(navController = navController)
        }
    }
}