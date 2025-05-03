package com.example.todonotediary.presentation.auth

import android.widget.Toast
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
import com.example.todonotediary.presentation.navigation.Screen

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
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLogin by remember { mutableStateOf(email != null && email.isNotEmpty()) }

    val registerState by viewModel.registerState.collectAsState()

    // Định nghĩa màu sắc
    val primaryColor = Color(0xFF6B8E9B)
    val backgroundColor = Color(0xFFF8F9FA)
    val textColor = Color(0xFF2C3E50)
    val buttonColor = Color(0xFF4A7186)
    val accentColor = Color(0xFFEC9A73)

    // Handle state changes
    LaunchedEffect(registerState) {
        when (registerState) {
            is RegisterState.Loading -> {
                isLoading = true
            }
            is RegisterState.Success -> {
                isLoading = false
                Toast.makeText(context, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                // Chuyển hướng đến màn hình chính
                navController.navigate(Screen.Todo.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
                viewModel.resetStates()
            }
            is RegisterState.Error -> {
                isLoading = false
                Toast.makeText(
                    context,
                    (registerState as RegisterState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = primaryColor
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title with indication if from Google
                Text(
                    text = if (isGoogleLogin) "Hoàn thành đăng ký Google" else "Đăng ký tài khoản",
                    color = primaryColor,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp, top = 24.dp)
                )

                // If from Google auth, show information message
                if (isGoogleLogin) {
                    Text(
                        text = "Vui lòng thiết lập mật khẩu để hoàn tất đăng ký tài khoản Google",
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Email TextField
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = { userEmail = it },
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
                    enabled = !isGoogleLogin, // Disable if email was passed from Google
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor,
                        disabledBorderColor = Color.LightGray,
                        disabledTextColor = textColor.copy(alpha = 0.8f)
                    )
                )

                // DisplayName TextField
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

                // Password TextField
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

                // Confirm Password TextField
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

                // Register Button
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
                        viewModel.registerWithEmail(userEmail, password)
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

                // Điều hướng đến màn hình đăng nhập
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(top = 32.dp)
                                .alignByBaseline(), // Đảm bảo căn chỉnh theo baseline
                            text = "Đã có tài khoản? ",
                            color = textColor
                        )
                        TextButton(
                            onClick = {
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(Screen.Register.route) { inclusive = true }
                                }
                            },
                            modifier = Modifier.alignByBaseline() // Giúp căn chỉnh theo baseline chữ
                        ) {
                            Text(
                                text = "Đăng nhập",
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}