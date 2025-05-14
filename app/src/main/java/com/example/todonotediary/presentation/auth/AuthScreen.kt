package com.example.todonotediary.presentation.auth

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.todonotediary.presentation.navigation.Screen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // States from ViewModel
    val googleSignInState by viewModel.googleSignInState.collectAsState()
    val emailSignInState by viewModel.emailSignInState.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // Định nghĩa màu sắc nhẹ nhàng
    val primaryColor = Color(0xFF6B8E9B) // Màu xanh nhạt
    val backgroundColor = Color(0xFFF8F9FA) // Màu nền nhẹ
    val textColor = Color(0xFF2C3E50) // Màu chữ
    val buttonColor = Color(0xFF4A7186) // Màu nút
    val accentColor = Color(0xFFEC9A73) // Màu nhấn

    // Cấu hình Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("712471685484-jadone5ck2fqss4s7k9qeisvin4s2mi8.apps.googleusercontent.com") // Thay bằng Client ID thực của bạn
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                viewModel.signInWithGoogle(token)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Đăng nhập Google thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle state changes
    LaunchedEffect(googleSignInState) {
        when (googleSignInState) {
            is GoogleSignInState.Loading -> {
                isLoading = true
            }
            is GoogleSignInState.Success -> {
                isLoading = false
                // Người dùng đã tồn tại, chuyển hướng đến màn hình MainScreen thay vì Todo
                navController.navigate(Screen.MainScreen.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
                viewModel.resetStates()
            }
            is GoogleSignInState.NeedRegistration -> {
                isLoading = false
                // Người dùng chưa tồn tại, chuyển hướng đến màn hình Register và truyền email
                val userEmail = (googleSignInState as GoogleSignInState.NeedRegistration).email
                navController.navigate("${Screen.Register.route}?email=$userEmail") {
                    popUpTo(Screen.Auth.route) { inclusive = false }
                }
                viewModel.resetStates()
            }
            is GoogleSignInState.Error -> {
                isLoading = false
                Toast.makeText(
                    context,
                    (googleSignInState as GoogleSignInState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                isLoading = false
            }
        }
    }

    LaunchedEffect(emailSignInState) {
        when (emailSignInState) {
            is EmailSignInState.Loading -> {
                isLoading = true
            }
            is EmailSignInState.Success -> {
                isLoading = false
                // Chuyển hướng đến màn hình chính nếu đăng nhập thành công
                navController.navigate(Screen.MainScreen.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
                viewModel.resetStates()
            }
            is EmailSignInState.Error -> {
                isLoading = false
                Toast.makeText(
                    context,
                    (emailSignInState as EmailSignInState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                isLoading = false
            }
        }
    }

    // Check if already authenticated
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            navController.navigate(Screen.MainScreen.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 100.dp)
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = primaryColor
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                // Title "Welcome"
                Text(
                    text = "Welcome",
                    color = primaryColor,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Email TextField
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = primaryColor
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor,
                    )
                )

                // Password TextField
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = primaryColor
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = primaryColor
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor,
                    )
                )

                // Forgot Password
                TextButton(
                    onClick = { /* Xử lý quên mật khẩu */ },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Quên mật khẩu?",
                        color = accentColor,
                        fontSize = 14.sp
                    )
                }

                // Login Button
                Button(
                    onClick = {
                        viewModel.loginWithEmail(email, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text(
                        text = "Đăng nhập",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Divider với text "hoặc"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        color = Color.LightGray
                    )
                    Text(
                        text = "hoặc",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        color = Color.LightGray
                    )
                }

                // Google Login Button
                OutlinedButton(
                    onClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textColor
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Thông thường bạn sẽ sử dụng painterResource cho hình ảnh Google
                        // Ở đây tạm sử dụng Circle thay thế
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDB4437) // Màu đỏ của Google
                        ) {
                            Text(
                                text = "G",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                        Text(
                            text = "Sign in with Google",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}