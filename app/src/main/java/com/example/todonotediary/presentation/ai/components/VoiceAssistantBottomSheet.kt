package com.example.todonotediary.presentation.ai.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.todonotediary.presentation.ai.VoiceAssistantEvent
import com.example.todonotediary.presentation.ai.VoiceAssistantState
import com.example.todonotediary.presentation.ai.VoiceStatus

/**
 * Bottom sheet UI for voice assistant
 * Half-screen modal bottom sheet with mic button, waveform, and status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantBottomSheet(
    state: VoiceAssistantState,
    onEvent: (VoiceAssistantEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    if (state.isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF2C3E50)
                )
                
                // Large Mic Button (center)
                AnimatedMicButton(
                    isListening = state.status == VoiceStatus.LISTENING,
                    onClick = {
                        if (state.status == VoiceStatus.IDLE) {
                            onEvent(VoiceAssistantEvent.OnMicClick)
                        }
                    }
                )
                
                // Waveform Animation
                WaveformAnimation(
                    audioLevel = state.audioLevel,
                    isListening = state.status == VoiceStatus.LISTENING,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                
                // Status Text
                StatusText(
                    status = state.status,
                    partialText = state.partialText,
                    resultMessage = state.resultMessage,
                    errorMessage = state.errorMessage,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action Buttons (Cancel)
                ActionButtons(
                    showCancel = state.status == VoiceStatus.LISTENING,
                    onCancel = { onEvent(VoiceAssistantEvent.OnCancel) }
                )
                
                // Bottom spacing
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
