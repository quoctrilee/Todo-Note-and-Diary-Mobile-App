package com.example.todonotediary.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavController

@Composable
fun AuthScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Định nghĩa màu sắc nhẹ nhàng
    val primaryColor = Color(0xFF6B8E9B) // Màu xanh nhạt
    val backgroundColor = Color(0xFFF8F9FA) // Màu nền nhẹ
    val textColor = Color(0xFF2C3E50) // Màu chữ
    val buttonColor = Color(0xFF4A7186) // Màu nút
    val accentColor = Color(0xFFEC9A73) // Màu nhấn

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 100.dp)
            .padding(bottom = 24.dp)
            ,
        contentAlignment = Alignment.TopCenter
    ) {
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
                        // Sử dụng Text thay cho Icon
                        Text(
                            text = if (passwordVisible) "Ẩn" else "Hiện",
                            color = primaryColor,
                            fontSize = 12.sp
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
                onClick = { /* Xử lý đăng nhập */ },
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
                onClick = { /* Xử lý đăng nhập bằng Google */ },
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
                        text = "Đăng nhập bằng Google",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
