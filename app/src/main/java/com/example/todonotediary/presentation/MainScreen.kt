package com.example.todonotediary.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.todonotediary.R
import com.example.todonotediary.presentation.diary.DiaryScreen
import com.example.todonotediary.presentation.navigation.Screen
import com.example.todonotediary.presentation.note.NoteScreen
import com.example.todonotediary.presentation.todo.TodoScreen

@Composable
fun MainScreen(parentNavController: NavHostController) {
    // NavController cho điều hướng bên trong MainScreen
    val innerNavController = rememberNavController()

    Scaffold(
        topBar = { TopNavigationBar(navController = innerNavController, parentNavController = parentNavController) },
        bottomBar = { BottomNavigationBar(navController = innerNavController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = innerNavController,
                startDestination = Screen.Todo.route
            ) {
                composable(Screen.Todo.route) { TodoScreen() }
                composable(Screen.Note.route) { NoteScreen() }
                composable(Screen.Diary.route) { DiaryScreen() }
            }
        }
    }
}

@Composable
fun TopNavigationBar(navController: NavHostController, parentNavController: NavHostController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp, 30.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar người dùng ở góc trái
            Surface(
                modifier = Modifier
                    .size(46.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Hi, Welcome Back!",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Amirnov",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Nút thông báo với thiết kế hiện đại hơn
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(2.dp, CircleShape),
                shape = CircleShape,
                color = Color(0xFFF0F0F5)
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Nút tạo mới với thiết kế hiện đại hơn
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clickable {
                        when (currentRoute) {
                            Screen.Todo.route -> {
                                // Sử dụng parentNavController để điều hướng đến AddTodo
                                parentNavController.navigate(Screen.AddTodo.route)
                            }
                            Screen.Note.route -> {
                                // Điều hướng tới màn hình tạo Note mới
                                parentNavController.navigate(Screen.AddNote.route)
                            }
                            Screen.Diary.route -> {
                                // Điều hướng tới màn hình tạo Diary mới
                                parentNavController.navigate(Screen.AddDiary.route)
                            }
                        }
                    },
                shape = CircleShape,
                color = Color.Black
            ) {
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create New",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem("Todo", Icons.Default.DateRange, Screen.Todo.route),
        NavigationItem("Notes", Icons.Default.Create, Screen.Note.route),
        NavigationItem("Diary", Icons.Default.DateRange, Screen.Diary.route)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .height(68.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            items.forEach { item ->
                val selected = currentRoute == item.route

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                        .background(
                            if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                        )
                        Text(
                            modifier = Modifier.padding(top = 6.dp),
                            text = item.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}