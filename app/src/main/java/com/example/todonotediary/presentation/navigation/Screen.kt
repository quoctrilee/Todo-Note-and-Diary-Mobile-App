// presentation/navigation/Screen.kt
package com.example.todonotediary.presentation.navigation

sealed class Screen(val route: String) {
    object Todo : Screen("todo_screen")
    object Note : Screen("notes_screen")
    object Diary : Screen("diary_screen")
    object Auth : Screen("auth_screen")
    object Register: Screen("register_screen")
    object Splash : Screen("splash_screen")
    object MainScreen : Screen("main_screen")
    object AddTodo : Screen("addtodo_screen")
    object AddNote : Screen("addnote_screen")
    object AddDiary: Screen("adddiary_screen")
}