package com.example.todonotediary.presentation.ai.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todonotediary.presentation.ai.VoiceStatus

/**
 * Display status text based on voice interaction state
 */
@Composable
fun StatusText(
    status: VoiceStatus,
    partialText: String,
    resultMessage: String,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Status label
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = when (status) {
                    VoiceStatus.IDLE -> "Nhấn micro để bắt đầu"
                    VoiceStatus.LISTENING -> "Đang lắng nghe..."
                    VoiceStatus.PROCESSING -> "Đang xử lý..."
                    VoiceStatus.SUCCESS -> "Thành công!"
                    VoiceStatus.ERROR -> "Có lỗi xảy ra"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = when (status) {
                    VoiceStatus.LISTENING -> Color(0xFF5B8EFF)
                    VoiceStatus.PROCESSING -> Color(0xFFFFA726)
                    VoiceStatus.SUCCESS -> Color(0xFF4CAF50)
                    VoiceStatus.ERROR -> Color(0xFFFF7675)
                    else -> Color.Gray
                }
            )
        }
        
        // Partial/final text display
        key(partialText) {
            AnimatedVisibility(
                visible = partialText.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Text(
                        text = partialText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Result message
        AnimatedVisibility(
            visible = resultMessage.isNotEmpty() && status == VoiceStatus.SUCCESS,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Text(
                    text = resultMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Error message
        AnimatedVisibility(
            visible = errorMessage != null && status == VoiceStatus.ERROR,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    text = errorMessage ?: "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC62828),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Processing indicator
        AnimatedVisibility(
            visible = status == VoiceStatus.PROCESSING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color(0xFFFFA726)
            )
        }
    }
}
