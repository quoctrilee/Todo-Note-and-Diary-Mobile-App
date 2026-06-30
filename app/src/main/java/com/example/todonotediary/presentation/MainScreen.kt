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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.todonotediary.R
import com.example.todonotediary.presentation.diary.DiaryScreen
import com.example.todonotediary.presentation.navigation.AddDiaryRoute
import com.example.todonotediary.presentation.navigation.AddNoteRoute
import com.example.todonotediary.presentation.navigation.AddTodoRoute
import com.example.todonotediary.presentation.navigation.DiaryRoute
import com.example.todonotediary.presentation.navigation.NoteRoute
import com.example.todonotediary.presentation.navigation.TodoRoute
import com.example.todonotediary.presentation.navigation.UserRoute
import com.example.todonotediary.presentation.note.NoteScreen
import com.example.todonotediary.presentation.todo.TodoScreen

@Composable
fun MainScreen(
    parentNavController: NavHostController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val innerNavController = rememberNavController()
    val user by viewModel.user.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshUserData()
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopNavigationBar(
                avatarUrl = avatarUrl,
                displayName = displayName,
                navController = innerNavController,
                parentNavController = parentNavController
            )

        },
        bottomBar = { BottomNavigationBar(navController = innerNavController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).background(Color.White)) {
            NavHost(
                navController = innerNavController,
                startDestination = TodoRoute
            ) {
                composable<TodoRoute> { TodoScreen() }
                composable<NoteRoute> { NoteScreen() }
                composable<DiaryRoute> { DiaryScreen() }
            }
        }
    }
}



@Composable
fun TopNavigationBar( avatarUrl: String?,
                      displayName: String?,
                      navController: NavHostController,
                      parentNavController: NavHostController) {
    val context = LocalContext.current
    val avatarResName = avatarUrl?.substringAfterLast('.') ?: "avt_default"
    val avatarResId = context.resources.getIdentifier(
        avatarResName,
        "drawable",
        context.packageName
    )
    val avatarPainter = painterResource(id = if (avatarResId != 0) avatarResId else R.drawable.avt_default)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp, 30.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clickable {
                        parentNavController.navigate(UserRoute) {
                            popUpTo(parentNavController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                shape = CircleShape,
                color = Color.White
            ) {
                Image(
                    painter = avatarPainter,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .clickable {
                            parentNavController.navigate(UserRoute) {
                                popUpTo(parentNavController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Hi, have good day!",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = displayName ?: "User",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.weight(1f))


            Spacer(modifier = Modifier.width(12.dp))
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Nút tạo mới với thiết kế hiện đại hơn
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clickable {
                        when {
                            currentDestination?.hasRoute<TodoRoute>() == true -> {
                                parentNavController.navigate(AddTodoRoute)
                            }
                            currentDestination?.hasRoute<NoteRoute>() == true -> {
                                parentNavController.navigate(AddNoteRoute)
                            }
                            currentDestination?.hasRoute<DiaryRoute>() == true -> {
                                parentNavController.navigate(AddDiaryRoute)
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
        NavigationItem("Todo", Icons.Default.DateRange, TodoRoute),
        NavigationItem("Notes", Icons.Default.Create, NoteRoute),
        NavigationItem("Diary", Icons.Default.Book, DiaryRoute)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 10.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .height(72.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            items.forEach { item ->
                val selected = currentDestination?.hasRoute(item.route::class) == true

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