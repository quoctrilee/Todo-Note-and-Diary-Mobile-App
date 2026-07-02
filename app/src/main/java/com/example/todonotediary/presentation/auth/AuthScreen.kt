package com.example.todonotediary.presentation.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.todonotediary.presentation.navigation.AuthRoute
import com.example.todonotediary.presentation.navigation.MainScreenRoute
import com.example.todonotediary.presentation.navigation.RegisterRoute
import com.example.todonotediary.presentation.splash.SplashUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
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

    // Lắng nghe trạng thái UI duy nhất từ ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Bảng màu chủ đạo nhẹ nhàng
    val primaryColor = Color(0xFF6B8E9B)
    val textColor = Color(0xFF2C3E50)
    val buttonColor = Color(0xFF4A7186)
    val accentColor = Color(0xFFEC9A73)

    val googleSignInClient = viewModel.googleSignInClient

    // Bộ launcher xử lý kết quả trả về từ Google SDK
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                viewModel.signInWithGoogle(token)
            }
        } catch (e: ApiException) {
            if (e.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                Toast.makeText(context, "Google Sign-In failed (Code: ${e.statusCode})", Toast.LENGTH_SHORT).show()
            }
        } catch (e : Exception) {
            Toast.makeText(context, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Quản lý các hiệu ứng phụ (Side-Effects) như chuyển màn hình hoặc hiện thông báo lỗi
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Authenticated -> {
                navController.navigate(MainScreenRoute) {
                    popUpTo<AuthRoute> { inclusive = true }
                }
            }
            is AuthUiState.NeedRegistration -> {
                val userEmail = (uiState as AuthUiState.NeedRegistration).email
                navController.navigate(RegisterRoute(email = userEmail)) {
                    popUpTo<AuthRoute> { inclusive = false }
                }
            }
            is AuthUiState.Error -> {
                Toast.makeText(context, (uiState as AuthUiState.Error).message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    if (uiState is AuthUiState.Loading || uiState is AuthUiState.Authenticated) {
        SplashUI(showProgress = true)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 100.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                // Tiêu đề chào mừng
                Text(
                    text = "Welcome",
                    color = primaryColor,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Ô nhập Email
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

                // Ô nhập Mật khẩu
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

                // Nút Quên mật khẩu
                TextButton(
                    onClick = { /* Xử lý quên mật khẩu */ },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Forget Password?",
                        color = accentColor,
                        fontSize = 14.sp
                    )
                }

                // Nút Đăng nhập bằng Email
                Button(
                    onClick = { viewModel.loginWithEmail(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                ) {
                    Text(
                        text = "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Thanh phân tách đường kẻ "Or"
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
                        text = "Or",
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

                // Nút Đăng nhập bằng Google
                OutlinedButton(
                    onClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDB4437)
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