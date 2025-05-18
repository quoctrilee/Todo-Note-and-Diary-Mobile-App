package com.example.todonotediary.presentation.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.todonotediary.R
import com.example.todonotediary.presentation.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    navController: NavHostController,
    viewModel: UserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAvatarDialog by remember { mutableStateOf(false) }

    // Display error if any
    LaunchedEffect(state.error) {
        state.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(message = it)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Profile", color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with avatar and display name
                    UserHeader(
                        avatarUrl = state.avatarUrl,
                        displayName = state.displayName,
                        onAvatarClick = { showAvatarDialog = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp).background(Color.White))

                    // Options
                    UserOptions(
                        onLogoutClick = {
                            viewModel.signOut()
                            navController.navigate(Screen.Auth.route) {
                                popUpTo("user") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

    // Avatar selection dialog
    if (showAvatarDialog) {
        AvatarSelectionDialog(
            currentAvatar = state.avatarUrl,
            onDismiss = { showAvatarDialog = false },
            onAvatarSelected = { avatarName ->
                viewModel.updateAvatar(avatarName)
                showAvatarDialog = false
            }
        )
    }
}

@Composable
fun UserHeader(
    avatarUrl: String,
    displayName: String,
    onAvatarClick: () -> Unit
) {
    val context = LocalContext.current

    // Extract resource ID outside of composable flow
    val avatarResId = getAvatarResourceId(context, avatarUrl)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { onAvatarClick() }
        ) {
            Image(
                painter = painterResource(id = avatarResId),
                contentDescription = "User Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Name
        Text(
            text = displayName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// Non-composable helper function to get avatar resource ID
private fun getAvatarResourceId(context: android.content.Context, avatarUrl: String): Int {
    return try {
        val drawableName = avatarUrl.substringAfterLast(".")
        val resources = context.resources
        val packageName = context.packageName
        resources.getIdentifier(drawableName, "drawable", packageName)
    } catch (e: Exception) {
        R.drawable.avt_default // Fallback to default avatar
    }
}

@Composable
fun UserOptions(
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogoutClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Logout",
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Logout",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AvatarSelectionDialog(
    currentAvatar: String,
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit
) {
    val context = LocalContext.current

    // List of available avatars
    val avatarList = remember {
        listOf(
            "R.drawable.avt_default",
            "R.drawable.avt",
            "R.drawable.avt_1",
            "R.drawable.avt_2",
            "R.drawable.avt_3",
            "R.drawable.avt_4",
            "R.drawable.avt_5",
            "R.drawable.avt_6",
            "R.drawable.avt_7",
            "R.drawable.avt_8",
            "R.drawable.avt_9",
            "R.drawable.avt_10"
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Choose Avatar",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(avatarList) { avatarName ->
                        val isSelected = avatarName == currentAvatar
                        val avatarResId = getAvatarResourceId(context, avatarName)

                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .background(
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else
                                        Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onAvatarSelected(avatarName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = avatarResId),
                                contentDescription = "Avatar option",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}