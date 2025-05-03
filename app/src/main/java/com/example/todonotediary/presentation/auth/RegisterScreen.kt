package com.example.todonotediary.presentation.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController

@Composable
fun RegisterScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Enhanced validation
    val isEmailValid = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length >= 6
    val doPasswordsMatch = password == confirmPassword
    val isFormValid = isEmailValid && isPasswordValid && doPasswordsMatch

    // Colors from theme
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.TopCenter)
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Email Field
            EmailField(
                value = email,
                onValueChange = { email = it },
                isError = email.isNotBlank() && !isEmailValid
            )

            if (email.isNotBlank() && !isEmailValid) {
                Text(
                    text = "Please enter a valid email",
                    color = colors.error,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Password Field
            PasswordField(
                value = password,
                onValueChange = { password = it },
                isVisible = passwordVisible,
                onVisibilityChange = { passwordVisible = it },
                label = "Password"
            )

            if (password.isNotBlank() && !isPasswordValid) {
                Text(
                    text = "Password must be at least 6 characters",
                    color = colors.error,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Confirm Password Field
            PasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                isVisible = passwordVisible,
                onVisibilityChange = { passwordVisible = it },
                label = "Confirm Password"
            )

            if (confirmPassword.isNotBlank() && !doPasswordsMatch) {
                Text(
                    text = "Passwords don't match",
                    color = colors.error,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Register Button
            Button(
                onClick = {
                    if (isFormValid) {
                        isLoading = true
                        // Call registration API
                        // On success: navController.navigate("home")
                        // On error: show snackbar
                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    disabledContainerColor = colors.primary.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = colors.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Register",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Email") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null
            )
        },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null
            )
        },
        trailingIcon = {
            IconButton(onClick = { onVisibilityChange(!isVisible) }) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Create else Icons.Default.Lock,
                    contentDescription = if (isVisible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}