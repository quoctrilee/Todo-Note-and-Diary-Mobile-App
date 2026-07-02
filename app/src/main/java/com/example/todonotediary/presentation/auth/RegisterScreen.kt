package com.example.todonotediary.presentation.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.todonotediary.presentation.navigation.AuthRoute
import com.example.todonotediary.presentation.navigation.MainScreenRoute
import com.example.todonotediary.presentation.navigation.RegisterRoute
import com.example.todonotediary.presentation.splash.SplashUI

@Composable
fun RegisterScreen(
    navController: NavController,
    email: String? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var userEmail by remember { mutableStateOf(email ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Nhận diện trạng thái nguồn gốc đăng ký
    val isGoogleLogin = remember { email != null && email.isNotEmpty() }

    // Lắng nghe trạng thái UI từ ViewModel
    val uiState by viewModel.uiState.collectAsState()

    val primaryColor = Color(0xFF6B8E9B)
    val textColor = Color(0xFF2C3E50)
    val buttonColor = Color(0xFF4A7186)
    val accentColor = Color(0xFFEC9A73)

    BackHandler(enabled = isGoogleLogin) {
        viewModel.cancelGoogleSignIn()
        navController.navigate(AuthRoute) {
            popUpTo<RegisterRoute> { inclusive = true }
        }
    }
    // Điều phối luồng màn hình sau khi tương tác với Firebase xong
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Authenticated -> {
                Toast.makeText(context, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                navController.navigate(MainScreenRoute) {
                    popUpTo<AuthRoute> { inclusive = true }
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Tiêu đề động tùy theo ngữ cảnh đăng ký
                Text(
                    text = if (isGoogleLogin) "Hoàn thành đăng ký Google" else "Đăng ký tài khoản",
                    color = primaryColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp, top = 24.dp)
                )

                if (isGoogleLogin) {
                    Text(
                        text = "Vui lòng hoàn tất thông tin cho tài khoản Google của bạn",
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Ô nhập Email (Khóa không cho sửa nếu là tài khoản liên kết Google)
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = { if (!isGoogleLogin) userEmail = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = if (isGoogleLogin) primaryColor.copy(alpha = 0.6f) else primaryColor
                        )
                    },
                    readOnly = isGoogleLogin, // 🌟 Người dùng Google chỉ có thể xem chứ không thể sửa đổi
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isGoogleLogin) Color.LightGray else primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = if (isGoogleLogin) Color.Gray else primaryColor,
                        cursorColor = primaryColor,
                        focusedTextColor = if (isGoogleLogin) textColor.copy(alpha = 0.7f) else textColor,
                        unfocusedTextColor = if (isGoogleLogin) textColor.copy(alpha = 0.7f) else textColor
                    )
                )

                // Ô nhập Tên hiển thị
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    label = { Text("Tên hiển thị") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Display Name",
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
                    label = { Text("Mật khẩu") },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor,
                    )
                )

                // Ô Xác nhận Mật khẩu
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    label = { Text("Xác nhận mật khẩu") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Confirm Password",
                            tint = primaryColor
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                tint = primaryColor
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor,
                    )
                )

                // Nút Kích hoạt Đăng ký
                Button(
                    onClick = {
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password.length < 6) {
                            Toast.makeText(context, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.register(userEmail, password, displayName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isGoogleLogin) "Hoàn tất đăng ký" else "Đăng ký",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Khối điều hướng quay lại Đăng nhập bằng Email truyền thống
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier
                            .padding(top = 32.dp)
                            .alignByBaseline(),
                        text = "Đã có tài khoản? ",
                        color = textColor
                    )
                    TextButton(
                        onClick = {
                            if (isGoogleLogin) {
                                viewModel.cancelGoogleSignIn()
                            }
                            navController.navigate(AuthRoute) {
                                popUpTo<RegisterRoute> { inclusive = true }
                            }
                        },
                        modifier = Modifier.alignByBaseline()
                    ) {
                        Text(
                            text = "Đăng nhập",
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}