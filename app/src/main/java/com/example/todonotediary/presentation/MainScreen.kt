// presentation/MainScreen.kt
package com.example.todonotediary.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.todonotediary.presentation.diary.DiaryScreen
import com.example.todonotediary.presentation.navigation.Screen
import com.example.todonotediary.presentation.note.NotesScreen
import com.example.todonotediary.presentation.todo.TodoScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Todo.route
            ) {
                composable(Screen.Todo.route) { TodoScreen() }
                composable(Screen.Notes.route) { NotesScreen() }
                composable(Screen.Diary.route) { DiaryScreen() }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem(
            title = "Todo",
            icon = Icons.Default.DateRange,  // Thay đổi từ Task sang List
            route = Screen.Todo.route
        ),
        NavigationItem(
            title = "Notes",
            icon = Icons.Default.Create,
            route = Screen.Notes.route
        ),
        NavigationItem(
            title = "Diary",
            icon = Icons.Default.DateRange,
            route = Screen.Diary.route
        )
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}