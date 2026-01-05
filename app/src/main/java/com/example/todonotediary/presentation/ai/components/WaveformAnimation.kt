package com.example.todonotediary.presentation.ai.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Animated waveform visualization synchronized with audio level
 */
@Composable
fun WaveformAnimation(
    audioLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 20
    val animatedValues = remember { List(barCount) { Animatable(0.3f) } }
    
    // Animate bars based on audio level
    LaunchedEffect(audioLevel, isListening) {
        if (isListening) {
            animatedValues.forEachIndexed { index, animatable ->
                launch {
                    val targetHeight = if (audioLevel > 0) {
                        0.3f + audioLevel * 0.5f + Random.nextFloat() * 0.2f
                    } else {
                        0.3f + Random.nextFloat() * 0.1f
                    }
                    
                    animatable.animateTo(
                        targetValue = targetHeight,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        } else {
            // Reset to idle state
            animatedValues.forEach { animatable ->
                launch {
                    animatable.animateTo(
                        targetValue = 0.2f,
                        animationSpec = tween(300)
                    )
                }
            }
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        animatedValues.forEachIndexed { index, animatable ->
            val heightFraction = animatable.value
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFraction)
                    .background(
                        color = if (isListening) {
                            Color(0xFF5B8EFF).copy(alpha = 0.8f)
                        } else {
                            Color.Gray.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            
            if (index < barCount - 1) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
