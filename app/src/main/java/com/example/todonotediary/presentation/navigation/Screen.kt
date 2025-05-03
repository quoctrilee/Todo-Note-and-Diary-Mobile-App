// presentation/navigation/Screen.kt
package com.example.todonotediary.presentation.navigation

sealed class Screen(val route: String) {
    object Todo : Screen("todo_screen")
    object Notes : Screen("notes_screen")
    object Diary : Screen("diary_screen")
    object Auth : Screen("auth_screen")
    object Splash : Screen("splash_screen")
}