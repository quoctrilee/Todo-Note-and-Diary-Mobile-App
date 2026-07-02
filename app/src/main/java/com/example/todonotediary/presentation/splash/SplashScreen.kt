package com.example.todonotediary.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.todonotediary.presentation.auth.AuthUiState
import com.example.todonotediary.presentation.auth.AuthViewModel
import com.example.todonotediary.presentation.navigation.AuthRoute
import com.example.todonotediary.presentation.navigation.MainScreenRoute
import com.example.todonotediary.presentation.navigation.RegisterRoute
import com.example.todonotediary.presentation.navigation.SplashRoute
import kotlinx.coroutines.delay

@Composable
fun SplashUI(
    showProgress: Boolean = false,
    primaryColor: Color = Color(0xFF6B8E9B)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TodoNoteDiary",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Manage your life beautifully",
                fontSize = 14.sp,
                color = Color.Gray
            )
            if (showProgress) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    color = primaryColor
                )
            }
        }
    }
}

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isDelayFinished by remember { mutableStateOf(false) }

    SplashUI(showProgress = true)

    LaunchedEffect(key1 = true) {
        delay(2000)
        isDelayFinished = true
    }

    LaunchedEffect(uiState, isDelayFinished) {
        if (isDelayFinished && uiState !is AuthUiState.Initial && uiState !is AuthUiState.Loading) {
            when (val state = uiState) {
                is AuthUiState.Authenticated -> {
                    navController.navigate(MainScreenRoute) {
                        popUpTo<SplashRoute> { inclusive = true }
                    }
                }
                is AuthUiState.NeedRegistration -> {
                    navController.navigate(RegisterRoute(email = state.email)) {
                        popUpTo<SplashRoute> { inclusive = true }
                    }
                }
                else -> {
                    navController.navigate(AuthRoute) {
                        popUpTo<SplashRoute> { inclusive = true }
                    }
                }
            }
        }
    }
}
